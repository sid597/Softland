# Plan — stage 3, "micro-store"

<!-- Phase 1 Step 5 of the rama skill, for the third entry of DECOMPOSITION.json.
Written 25 September 2026 by a fresh-context session (Claude Fable 5.1, effort
max). Plan only: no module or test code. Sources: SPEC.md and everything it
names; IMPLICIT_SPEC.md (cited as I-xx, OPn, RDn, En, On); DECOMPOSITION.json
("micro-store" scope); RIG.md (phase 0, R1 to R15); PLAN-stream-store.md
(validated, cited as "the stream plan", its picks as P1 to P16, its fixes as
[F n]); PLAN_VALIDATION-stream-store.md; PLAN-locks-and-forgetting.md, which was the
untouched template when this session began and was written by its own
session during it; its shapes for the lock record, the lock id, the lock
row, the erasure ledger and the person locks are taken here as the
interface the slots follow (M12), not redesigned; the skill's
references (microbatch, batch, aggregators, pstate-schema, paths,
depot-design, depot-reference, foreign-client, query-topologies, testing,
dataflow, stream, unique-ids, patterns, app-design). Where a line here
summarises a source, the source wins. Vocabulary: "key" is a fact's key;
"lock" is an encryption key. -->

## Revision, 26 September

<!-- Revised on 26 September 2026, 01:50 to 03:00 IST, by a fresh-context
session (Claude Opus 5.5, effort max), the rama skill's plan step, for
tonight's defaults: CONCLUSION.md R1, R2, R3, R7 and R8
(src/proposal/store-next-2026-09-25/ on main). They are Sid's proposals
taken as defaults, not rulings; this plan names them as defaults. The
sections after this revision are the 25 September plan. Where a line there
contradicts a section here, this revision wins, and the body carries a
pointer at each such place. Phase 2's revision (sealing at the door, in
/mnt/data/projects/Softland-rig-plan-locks) was not committed when this was
written; its interface is taken as the step brief names it: `rig.store.locks`
with seal, open, wrap, unwrap, the delivery function and open-value. -->

Each change, with its reason in a line:

1. **Sealing in shared layers (§A).** This gate mints a shared layer's
   leases; the lease rows sit beside the lease act's name row, on the
   lease name's task; a sealed act is routed there; each lock is read, used
   and consumed on that one task in the batch that decides the value. Why: default R1 (nothing that could open a
   value sits in the depot) and ruling 7 (wrapped locks in the record).
2. **The micro depot's partitioner (M4 revised).** A sealed act arrives on
   hash(the lease name of its first cited lock); an act with no sealed
   value arrives on hash(its first entity), as before. Why: the lease rows
   must be on the task where the value-level checks run, a lock is minted
   before anyone knows which entity it will seal, and a random lease name
   spreads even one hot session's acts over the tasks, one lease at a
   time.
3. **Face refusals keyed by the envelope fingerprint (M8 revised).** Why: a
   face refusal belongs to one attempt, and phase 2's parts digest is the
   same for an honest resend and for a resend whose sealed values differ;
   the envelope fingerprint (a keyed hash of the sealed envelope as sent)
   is per attempt, and the door and the gate both hold it.
4. **Phase 2's two digests and record path.** The record keeps the parts
   digest (no value in it; **[PV-F2]** no lock id in it either, phase 2's
   V-F1); each row keeps its value digest (R1's HMAC over the plaintext,
   keyed by the value's lock); a decided name answers from its record
   whatever its leases; a resend's values are opened with its own lock and
   checked on their entity tasks under the recorded locks, and its owned
   lease rows are consumed; after a forget the forgotten value is not
   checked. Why: R1's riders, and one
   rule for both gates.
5. **Permissions (§B).** A permission may name its parent; the check walks
   the chain; one function, `rig.store.permit/refusal`, serves both gates;
   the stream gate changes in three places. Why: default R7.
6. **The base (§C).** Made on the stream gate as a one-owner layer owned by
   the root actor; re-classed to by-entity before the first group is made;
   M11 revised. Why: default R8, which overrides SPEC.md phase 3's placement
   of the base with the group.
7. **The settled frontier (§D).** The frontier id's form, the query that
   reads through it with one explicit F, and what a read entry records.
   Why: RIG.md R5 and default R3.
8. **Stamps (§E)** come from `rig.store.clock`. Why: default R2.
9. **Tests (§F)** rewritten for all of the above. Why: the step's named
   tests.
10. **The partitioning table (§H)** recomputed for the lease read, the
    permission chain and the hop to the entity task. Why: the costs moved.
11. **Rig choices.** M4, M8, M11 and M12 revised; M16 to M25 added (§I).
    First-record picks are marked.
12. **The probe (§G)** settled M1 (one `defn`, one line in `defmodule`), the
    frontier id's class (a Long, the same across a retry), a name vector as
    a top-level key, and the tick depot's cadence. Why: the plan could not
    otherwise be validated on those points.
13. **Open questions (§J)** gathered in one place. Why: each needs Sid, a
    later stage or the build's first check, and none blocks this build.
14. **What stands unchanged, and why it still holds:** M1 (with the probe's
    answer, §G), M2 (batch order), M3 (the leader's fold over skeletons: a
    lock and a value never reach it), M5 (a re-classed layer's stream-era
    history read as settled history; the base now uses it too), M6 (R6:
    the offer carries its stood-on stamps), M7 (batch-stamped, never
    overwritten; lease rows are the one ruled exception, consumed), M9, M10,
    M13, M14, M15; `gate/decide` reused, not rewritten; the name row, so an
    answer is found by name plus layer (the name carries its layer) on the
    name's task, written in the deciding batch; the re-class of a hot layer
    moving it here; the four PStates; the frontier's mechanism.

### A. Sealing in shared layers

Aligned with phase 2's revision (committed at 02:12 IST in the locks
worktree, "The door and the lease road"): its lease fact `{:e s :k :lease
:v {:count n}}`, its lock ids `(locks/lease-ids name n)` = `[name i]`, its
lease row `{:under p :sealed bytes}`, its two digests, its record path and
its consumption rule are taken as they are; only where this store keeps
the rows and how its batch reaches them is this plan's.

**The reading of "the task where it decides".** This gate decides in two
places. The act's verdict is the leader's fold, which needs no lock and
never sees a value (M3). Everything about a value is decided on a task
that holds what it needs: a first offer's lock work (deliver, open, own
subjects, re-wrap, value digest) on the task where the offer arrives, which
is where its lease rows sit; a resend's value check on the value's entity
task, where its recorded lock sits. A lock never leaves the task it is read
on, and a plaintext value never leaves the task it is opened on. What
travels between tasks is sealed bytes, digests, subject sets, lock ids, and
locks wrapped under person locks. A reading that put the leases on the
leader (task 0) was weighed and rejected: every shared layer's lock traffic
and every value's opening would funnel through one task, and the plaintext
would travel to it.

**What travels between tasks, corrected [PV-F2].** The line above holds for
bare locks with two exceptions, both inside the cluster and both to a place
that already holds them so: the empty wrap of a value about no one (K bare
in `:blob`, stored so in the record), and an operator's bare lease row on
the record path. Plaintext values never leave the task they are opened on.

**Who mints a shared layer's leases: this gate.** The stream gate cannot:
a lease row must be consumed by the batch that decides its value, and only
the topology that owns a PState writes it. A lease act is phase 2's, made
into the shared layer L: an ordinary act by a person in session s, citing
s's permission in L (§B), named `[L :by-entity :offer uuid7]`, answered by
name through the name row, with one control fact `{:e s :k :lease :v
{:count n}}`, n a long in 1..256 (`:malformed-control` otherwise, R13; a
non-operator may write it, as phase 2 extends R13's list). The session id
stands as the entity, as a layer id does in M5, so the act arrives on
hash(s) (it seals nothing, so it routes by its first entity). **[PV-F1]**
The person whose lock seals the rows is the lease act's `:who` (phase 2's
L23 as restated at its V-F2: "in the base the writer"; the 26 September
text here read the session's owner, which phase 2 withdrew). The gather
reads `$$persons [who]` on the arrival task (`$$persons` is on every task);
phase 2's `:no-such-person` and `:person-forgotten`, recorded after stage
1's list, refuse the lease when that lock is absent or destroyed. For
`:who :operator` nothing is read and nothing is refused on this ground: the
operator has no person lock (phase 2's V-F2). On yes, block 2b carries the
`:who` from the arrival task to hash(lease-name), the lease act's own name
task, and there mints n locks and their nonces (phase 2's `fresh`, bound
outside any decision), seals each under that person's lock, and writes the
lease rows beside the lease act's name row:

    $$micro-names [lease-name :leases i]  =  {:under   P | nil             ; phase 2's lease row: the sealing person
                                              :sealed  bytes               ; K sealed under P's person lock; K bare when :under is nil
                                              :layer   L                   ; marks this store adds
                                              :session s
                                              :batch   b}

The lock ids are `(locks/lease-ids lease-name n)`, a function of the name
and the count, so the door knows them without an ack. **[PV-F5]** The
operator's lease is phase 2's `{:under nil :sealed K}`: K bare at rest in
this PState until a decision citing it consumes it or its session closes
(PV-F4); no forget can close it, since it is under no person. It opens only
the depot bytes of an operator offer that cited it and was refused on its
face or not yet decided: phase 2's "retention it bounds", which on this
side ends at the close. The marks `:layer` and `:session` are what phase 2
gets from its key (`[layer :leases session lock-id]`); here the key is the
lease's name, so they are fields. The row's shape beyond phase 2's and the
placement are first-record placeholders (M16).

A retried lease batch mints other bytes. Only the committed attempt's rows
exist, and the door reads rows only through the frontier, so it never holds
a lock from an attempt that did not commit.

**The door takes their plaintext by a query.** `micro-lease [lease-name
F]` on hash(lease-name): read F; the rows under `[lease-name :leases]` with
batch ≤ F (one seek, n iterations, a subindexed map keyed by index);
`locks/unlease` each over the owner's `$$persons` entry (local); return the
plaintext locks. The depot never sees this path. Input examples: a settled
lease of n rows, 2 reads (F, then the range: 1 seek and n iterations), all
meaningful; a lease not yet settled or never made, 2 reads with an empty
range, which is the answer the door needs ("not yet"). Fixed: no read
depends on another's result. A restarted door finds its leases again by
`micro-leases-of [s F]` on hash(s): the range `[s :answers]`, the acts that
touched the session as an entity (its lease acts), then `micro-lease` per
name.

**The door seals each value under one leased lock, and the offer cites its
id** (phase 2's sealed fact). All the locks an act cites come from one
lease of the act's `:session`, made for the act's layer (M24). A lease row
whose `:session` or `:layer` is not the offer's counts as missing.

**Routing (M4 revised).** `route-key` is the lease name of the first lock
any of its facts cites (a lock id is `[lease-name i]`, so no read), else
its first fact's entity, else nil, where block 1 refuses the record as
malformed. A sealed act therefore arrives where its lease rows are: the
depot's `hash-by` and `$$micro-names`' default key partitioner hash the same
name vector, the argument P2 and the 25 September plan already rest on for
entities, and the probe showed a name vector works as that key (§G).

**The two digests (phase 2's).** The answer record's `:digest` is the parts
digest: `env/digest` over the offer minus its name with every `:sealed`
and every `:lock-id` removed (**[PV-F2]**, phase 2's V-F1: a door that lost
its locks resends under newly leased ones, and R1's rider opens a resend
"with its own lock" and recomputes "under the lock it recorded the first
time"). It covers nothing that is a value and names no lock, so it is
computed anywhere with no lock, and the name-taken check in the fold stays a
comparison of digests (M8). The value digest, HMAC-SHA256 keyed by the
value's lock over its plaintext (R1), goes in the value's row, written only
for a yes.

**Block 1, a first offer, on the arrival task before any hop.** For a sealed
act, read its lease rows (one range read: one seek, one iteration per cited
lock) and `locks/unlease` each. If all are present: open each value
(`locks/open`), compute its own subjects (phase 2's `subjects-of`: no owner
in a shared layer, then the grammar's subjects and the carried ones) and
the act's subject union. **[PV-F6]** For each subject, the task reads its
`$$persons` entry (local: `$$persons` is on every task) and emits
`[[:person p] entry]`, so `gate/decide` has the input for phase 2's
`:no-such-person` and `:person-forgotten`; the opened values say who the
subjects are, the entries say whether their locks live. Then the flow
drops the plaintext and the delivered locks, and carries on only the parts
digest, the subject union, the cited lock ids, the ids found present and
owned by the offer's `:session` and layer (the only rows it may consume,
PV-F2), and a lease status (all present, or which id is missing). The
skeleton gains `:locks [lid ...]`, `:owned [lid ...]` and `:fp`, the
envelope fingerprint (PV-F3).

**Then the name task, as in the 25 September plan: a decided name answers
from its record whatever its leases** (phase 2's order). If the name has no
record and a cited lock was missing, the offer is refused on its face as
`:no-such-lock` (phase 2's reason): a faces row under `[name
envelope-fp]`, no answer under the name, nothing consumed, nothing else
gathered, so a resend under the same name with a lock that delivers is
decided fresh. If the name has no record and every lock was there, the
gather goes on to the layer's task and the entity tasks as before.

**The record path** (the name has a record), phase 2's:

- the parts digest differs: a `:name-taken` face;
- it matches and the record is a no: "recorded" (the refused act's leases
  were destroyed at its decision, so there is nothing to check);
- it matches and the record is a yes (**[PV-F2]**, phase 2's V-F1 record
  path): the flow goes to the entity task of each value fact, where the
  first act's row sits at `[e name idx]` (the facts correspond by position,
  since the parts digest matched). It carries the resend's sealed bytes
  and, when the cited id is not a consumed one, the cited lease row as
  read on the arrival task, still sealed under its `:under` person (both
  safe to carry; an operator's bare row is the one case where a bare lock
  crosses a task, inside the cluster, on this rare path). On the entity
  task: the recorded lock `R` is unwrapped from the row's `:lock`
  (`locks/unwrap`, `$$persons` local; nil when the value is forgotten or
  its wrap closed by a person forget); the resend's own lock `O` is `R`
  when the cited id is the row's `:lock-id` (a door that kept its locks),
  else the carried lease row unleased there (a door that lost its locks and
  leased again; nil when it was missing, not the offer's session or layer,
  or does not unlease). Both open: the resend's bytes are opened with `O`
  and the value digest recomputed under `R` and compared with the row's;
  bytes that do not open or a digest that differs give `:name-taken` (a
  face, nothing consumed). Either nil: that value is not checked (R1's
  rider, the price of forgetting; phase 2's "a missing own lock skips
  rather than refuses", so a replayed answer stays the same). Every value
  checked or skipped: "recorded".

**[PV-F2]** The record path's one write is phase 2's: the answer returned
from the record (a yes or a no) consumes every cited lease row that block 1
found present and owned by the offer's `:session` and layer (the skeleton's
`:owned`), through block 2c, as default 1 has it ("refused or answered from
the record, it is destroyed"); a `:name-taken` consumes nothing. The
check's result reaches the fold as the envelope's row, `[[:resend name fp]
{:check :recorded|:name-taken :owned [lid ...]}]`, emitted from the entity
task after the last value's check (one row per envelope, the checks of
several values merged by `+map-agg`'s combiner as any row is). The owned
filter is what phase 2's key does on its side: a no-read delete by
`[lease-name i]` alone could destroy another session's row, so this side
deletes only rows it read and matched. Resends are rare (category (e)); a
yes record's check pays one hop per entity. The plaintext of a resend is
opened only on the value's own task.

**The fold** (**[PV-F3]**, keyed per envelope). Block 1 emits the offer row
as `[[:offer name fp] skeleton]`, fp the envelope fingerprint (M8), so two
envelopes under one name do not collapse in `+map-agg` (keyed by name
alone, one of them vanished, and whichever merged last was decided); two
byte-identical envelopes still collapse, rightly. The batch order is
`[uuid7 name fp]`, so no two entries tie. Per envelope, in order:

1. a face row: copied to the output;
2. the name already decided, in W (earlier in this fold) or in committed
   state (a record-path row from block 1): with another parts digest, a
   `:name-taken` face; with the same parts digest and a record-path row,
   the check's result (recorded, or a `:name-taken` face); with the same
   parts digest decided **earlier in this fold** and another fp, a
   `:name-taken` face: the fold holds no value and cannot check content,
   and the rows written this batch are the decided envelope's only
   (block 2b's fp filter). An honest door that re-seals and resends inside
   one batch hears this face for the second attempt while the first
   attempt's answer stands; the stream gate would answer it from the record
   after a value check (named divergence, for builder A);
3. a lock id that an earlier envelope under **another name** in the batch
   order already cited: `:no-such-lock`, a face (two acts citing one leased
   lock is a door's bug; the stream gate gives the same answer because the
   first decision consumed the row);
4. otherwise the decision, as below.

The fold sees digests and ids only. It adds to its output, for every act it
decides in this batch (yes or no) and every record-path answer (yes or no),
a deletion of each lease row in the envelope's `:owned`, for block 2c; for
a decided act it also writes the decided envelope's fp at `[name :fp]`
beside the record (a gate mark outside the answer record, first-record).

**Block 2b (the rows), starting on the arrival task.** For a sealed
record: re-read its lease rows; unlease; open; compute each value's own
subjects; re-wrap each lock under them (`locks/wrap`, shared-layer rules:
7b as written, an empty wrap for a value about no one); compute each value
digest (`locks/value-digest`); drop the plaintext and the bare locks. Then,
as in the 25 September plan, the name task's record decides whether rows
are written (decided yes in this batch, with this parts digest, and
**[PV-F3]** this envelope's fp equal to `[name :fp]`, read in the same
select as the record), and the sealed bytes, the wrapped lock record and
the value digest go on to the entity task (never the plaintext; never a
bare lock except the empty wrap of a value about no one, which is K bare in
`:blob` by phase 2's lock record and is stored so in the record by the
shared-layer rule, **[PV-F2]**). **[PV-F6]** 2b reads `$$persons` again,
as another topology's PState at a moment, so it can differ from block 1's
read: a subject forgotten in between gives a record whose any-of blob for
that subject cannot be made, so the value is closed for it (the wrap's
any-of list is the subject list, never the live list, so this is never an
empty wrap); a lease person forgotten in between makes `unlease` nil, and
the row is written with `:lock` nil: admitted and closed, the price of a
lease under that person's lock. Nothing throws in either case. Two
byte-identical records of one envelope in one batch both pass the filter
and write the same rows with different re-wrap nonces: two wraps of one K
under one wrap, one meaning. There the row is
written: `:v` holds the sealed bytes as offered, `:lock-id` the cited id,
`:lock` the wrapped record (ruling 7: in the record), `:digest` the value
digest. The leased lock has become the value's lock; K's bytes do not
change, only its wrapping (phase 2's re-encoding). The wrap is done before
the verdict is known so that no hop comes back to the arrival task; for a
refused act it is a few microseconds of work thrown away. M12's seam
`micro/row-lock` becomes this code.

**Block 2c, new: consume the leases.** From the fold's output on task 0
(materialized as block 2a's writes are), each deletion goes to its lease
name's task: `(termval NONE)` on `[lease-name :leases i]`, a no-read write.
So a lease row is consumed at decision whatever the decision (admitted: it
has become the value's lock, wrapped into the record; refused: destroyed),
in the batch that decides it, exactly once with the batch; so is every
owned row of a resend answered from the record (**[PV-F2]**). Face refusals
(`:no-such-lock`, `:name-taken`, a double use, anything before the name is
trusted) consume nothing, as phase 2's do.

**[PV-F4] Session close, this side.** Default 1: "unconsumed lease rows go
when their session closes"; phase 2's L28 act, one control fact `{:e s :k
:session-closed :v {:session s}}` into the layer. Here it is an act into
shared layer L named `[L :by-entity :offer uuid7]`, routed by its first
entity s to hash(s), decided by the fold like any act (the session's
permission or the operator's). Block 1 reads `[s :answers]` on hash(s)
(one seek, k iterations: the acts that touched s as an entity, which are
its lease acts and the close) and keeps the names in layer L. The fold, for
a yes, emits for each such name a deletion of its whole `:leases` map
(`(termval NONE)` at `[lease-name :leases]`, no read; a no-op for a name
with no leases), sent by block 2c to each name's task. After the close a
cited id of that session is missing: `:no-such-lock` on the face, as on
phase 2's side. This bounds the retention phase 2 names ("until the close
or the owner's forget, an unconsumed lease row opens the depot bytes of an
offer that cited it and was refused on its face or never decided"), and
for the operator's bare rows the close is the only bound. When a session
closes stays Sid's open item 82. Why a block after 2b: 2b re-reads
the lease rows to re-wrap them, and a deletion before that read would take
an admitted value's lock; the barrier is what makes "consumed in the batch"
safe. A lease minted in this batch cannot be consumed in it: an offer
citing it read committed state in block 1, found it missing, and was
refused on its face.

**Grain.** In a per-value layer each value cites its own leased lock; a lock
cited twice in one act is `:no-such-lock` for its second use. In a per-act
layer the act cites one leased lock for all its values (the grain's "one
small lock shared by all values in an act").

**Nothing here throws.** Every lock function (unlease, open, wrap, unwrap,
value-digest) is total (phase 2's: nil on any failure) and is called
through the per-offer `Throwable` guard besides. A sealed value that does
not open under its cited lock (wrong lock, tampered bytes) is refused on
its face as `:malformed`. A deterministic throw in a microbatch retries the
batch for ever (rule 9, "What Rama showed" 3).

**A missing lock, with nothing recorded.** The faces entry is not the name's
answer, so the name stays free, which is the default's stated purpose ("so a
resend under the same name is still allowed"); phase 2's stream side
answers through the ack, which a microbatch depot does not have, so this
store's face refusals are data the offerer reads (M8). If the validator
reads "nothing recorded" strictly, meaning no entry at all, the change is
one line: drop the faces write for `:no-such-lock`. The offerer then learns
it by timing out, and re-leases.

**One delivery function, and why the micro side has its own read.** Phase
2's `deliver-lock>` is a `deframafn` whose body reads `$$layers [layer
:leases session lock-id]`, the stream store's rows, which this store's
leases are not. So the micro side's delivery is its own read of
`$$micro-names [lease-name :leases i]` around phase 2's pure half,
`(locks/unlease row person-entry) → K | nil`, which phase 2 made public for
this. The unwrapping, the nil for a missing or unopenable row, and the
person-lock rule are one function for both gates; only the read differs,
because a gate reads and consumes its own PState's rows.

**What the depot holds after this.** Sealed bytes, lock ids, names, ids and
the carried parts: nothing that could open a value (R1). The lease rows are
PState rows, durable and replicated, which is why R1 took the lease over
the in-memory holder: nothing unreplicated sits in the decision path. A row
is about 150 bytes (phase 2's 140 and a batch id), at most 256 per lease,
gone at decision. An unconsumed row opens only the depot bytes of an offer
that cited it and was refused on its face or never decided, which are not a
value of the store (phase 2's "retention it bounds"), until a decision
citing it or its session's close (**[PV-F4]**, above); an operator's row
holds K bare over the same span (**[PV-F5]**). Between a person's forget
and the batch that decides an offer about them, the offer's lock and its
depot bytes are both retained, for any lease: the store holds person locks,
so a wrapped row opens for it too. The batch closes that window (it
refuses `:person-forgotten`, recorded, and consumes the row); its length
is one batch.

### B. Permissions (default R7), both gates

**The shape (first-record, M20).** A permission id is P8's triple `[who
layer in]`, or `[who layer in parent]` with `parent` a permission id: a
narrower permission beneath its parent. Chains are at most four deep (root,
session, agent, tool); `env/pid?` refuses deeper ones as malformed. `who`
is the holder:

- a person, a session id, an agent id or a tool id;
- for a group's root, the group itself, named by its layer id (ruling 9's
  "root permission made with the layer naming the group");
- for the base's root, the root actor, `:operator` in the rig (default R8;
  **[PV-F12]** phase 2's V-F2: "the rig uses `:operator`"; was `:root`).

A layer's root permission is granted in its making act (P10's grants in the
making act): `[owner L L]` for a one-owner layer, `[L L L]` for a group,
`[:operator :base :base]` for the base. Opening a session writes, through each
layer's own gate, a grant `[S L L p]` beneath the person's permission p in
L. An agent's or a tool's goes beneath the session's. Grants and revokes
stay the operator's (R13). In the rig, opening a session is the operator's
act, one per layer, made by the test helper `open-session!`; whether a
holder may grant beneath itself is open and changes no record.

**The check: one function both gates call.** `rig.store.permit`, new and
pure, in `src/rig/store/permit.clj`:

- `(chain pid)` gives `[pid parent ... top]`, from the id alone, with no
  read.
- `(refusal offer rows)` gives one of the model's four reasons in the
  model's order, or nil. For an offer that is not exempt:
  - `:permission-does-not-cover-this` when the cited pid is nil, its holder
    is neither the offer's `:who` nor its `:session`, or its layer is not
    the offer's;
  - `:permission-from-another-layer` when the `in` of any element of its
    chain, or the layer of any ancestor, is not the offer's layer;
  - `:no-permission` when any element has no grant in `rows`;
  - `:permission-revoked` when any element is revoked.

  For a triple this is P8's four checks plus one widening (**[PV-F11]**):
  the holder may be the offer's `:session` as well as its `:who` (M21);
  the model requires the holder to be `:who` (model.clj `refusal`), and
  its holders are persons only, so the model's histories answer as before;
  a phase 1 case citing a session-held triple would change its answer (not
  checked here; the build's first run shows it). The model has
  no parent or chain; the chain cases are the rig's own tests.

**What changes in the stream gate, exactly.**

1. `gate/refusal`: its four permission clauses become one clause returning
   `(permit/refusal offer rows)`, in the same place in the order.
2. `gate/pids-to-read`: `[(:permission offer)]` becomes `(permit/chain
   (:permission offer))`. The grant and revoke targets are read as before.
3. `env/pid?`: accepts the four-element form with a nested pid, at most
   four deep.

That is the whole change. The rows are read as now, one keypath per pid on
the home task, only now up to four of them, and `decide` writes permission
rows as it does now. The `:session` part is already a readable keyword in
the envelope.

**[PV-F8] The cascade replaces the walk.** The validation's throughput check
fails the walk on the plan's own numbers below (the cascade does less I/O
per offer), and the skill rules "Never trade I/O efficiency for code
simplicity". The walk stays written below as the fallback builder A may
take if tonight's merge with phase 2's `gate.clj` makes the smaller change
worth its cost (about 1.9 seeks per operation, §H). The cascade, both
gates:

- **Rows.** A permission row gains `:cut`, `{:by revoke-fid :batch b}` on
  this gate (`{:by fid :stamp s}` on the stream gate), written once: the
  first cut stands (M7, never overwritten). A children index `[L :children
  parent-pid child-pid]` = the grant's batch (stamp on the stream gate),
  subindexed, in the same PState and task as the rows (`$$micro [L]`,
  `$$layers [L]`).
- **Grant** `[who L L parent]`: the gather reads the parent's row too (one
  seek); the grant is written with the child index entry, and born cut
  (`:cut` the parent's revoke or cut) when the parent is revoked or cut,
  in committed state or in W.
- **Revoke** of pid P: the gather walks P's subtree on L's task through the
  children index (a `loop<-` with `yield-if-overtime`, one range seek per
  inner node, `{:allow-yield? true}`) and reads each descendant's row; the
  fold writes `:cut {:by revoke-fid :batch b}` on each descendant neither
  revoked nor cut, including descendants granted earlier in this batch
  (W). On the stream gate the same walk runs inside the revoke's event on
  the home, atomic on one task.
- **Check**: `permit/refusal` reads one row, the cited pid's (two for a
  re-classed layer, one per store, merged: live iff granted in either and
  revoked or cut in neither, M5), and refuses `:permission-revoked` when
  it is revoked or cut; on this gate it also refuses when W holds an
  in-batch revoke or cut of any element of the cited pid's chain, taken
  from the id with no read (`permit/chain`). The other-layer rule stays on
  the chain in the id.
- **Re-classed layers**: the walk reads the stream store's frozen children
  and rows too, and writes its cuts into this store's delta
  (`$$micro [L :permissions d]` holding only the cut for a stream-era d).
- **Latency**: a revoke of a group's root reads every descendant once, in
  one batch; with yields the task stays responsive, and that batch is
  slower once (at about 0.5 ms a seek, 1,000 descendants is about half a
  second).
- **Stream gate changes** (replacing the three below): `refusal`'s clauses
  become one `permit/refusal` call; `pids-to-read` gives the cited pid, the
  parent for a grant, and the subtree for a revoke; `pid?` accepts the
  four-element form; `decide` writes the child entry, a born cut and the
  revoke's cuts; `$$layers`' permission value gains `:cut` and the layer
  gains `:children`.

The walk, kept as the fallback:

**How a revoke cuts everything below it.** A revoke writes one row (P8: it
stands on its grant; `:stale-revoke` for an unknown or already revoked pid;
R18). Nothing below it is written. Every later check walks its chain and
meets the revoked row.

- **On the stream gate**, the revoke is one event on the layer's home. Offers
  decided on that task after the event are refused and those decided before
  it were admitted: the home's single thread is the order.
- **On the micro gate**, the revoke is decided in the batch order (M2). The
  fold marks the pid revoked in its working state W, so every later offer
  in the same batch whose chain holds the pid is refused
  `:permission-revoked`, and every later batch reads the committed row. For
  a re-classed layer (and the base), each element's row is read from both
  stores and merged: live if granted in either and revoked in neither (M5).
  A stream-era root revoked on the micro side therefore cuts everything
  below it.
- **A grant beneath a revoked or never-granted parent** is admitted as a
  fact and covers nothing, because the walk refuses every offer that cites
  it (rig choice; no new reason).

**Cost, and the alternative constructed.** The check reads the chain's rows
on the one task that holds the layer's permission index. A session's write
in its person's own layer (session, root) reads 2 rows where P8 read 1; a
session's write in a group (session, person, root) reads 3; all are point
reads on a task the offer visits anyway. With half the traffic at each
depth, that is about 1.5 seeks more per offer.

The alternative is to cascade a revoke. Each permission row gets a `:cut-by`
field and a `:children` index; a revoke walks the subtree and marks every
descendant; a grant reads its parent (1 seek) and is born cut if the parent
is; a check reads 1 row, plus, on the micro side, W's in-batch revokes
matched against the chain in the id (no reads). Per offer that saves the
1.5 seeks. Per revoke it costs one range read and one write per descendant,
s in all: at most tens for a one-owner layer, and for a group root every
member's sessions, agents and tools, say s ≈ 1,000. Per grant it costs 1
read and 1 write more. At one revoke per 10,000 offers, the cascade's
amortised cost is about 0.1 seeks per offer against the walk's 1.5, so on
throughput alone the cascade wins.

It is not taken tonight, for three reasons. The brief asks for the fewest
lines in the stream gate: the walk is three changed places, while the
cascade adds a children index and a cut field to `$$layers`, a subtree walk
inside a stream event (which holds up every offer of that layer's home
while it runs), and born-cut grants. For a re-classed layer, the cascade
must also walk the stream store's frozen children from the micro side and
keep the marks in its own delta. And, the deciding one (rule 10), the cut
marks are projections rebuildable from the grant and revoke facts, so a
later switch to the cascade touches no record. The walk is the simplest
thing that can change later; phase 7's numbers say whether the 1.5 seeks
show.

**A permission cited from another layer** is refused
`:permission-from-another-layer`, whether the cited pid or any of its
ancestors lives elsewhere. That is the ruling: a gate checks only
permissions in layers it orders.

### C. The base (default R8)

This replaces SPEC.md phase 3's placement of the base ("a microbatch gate
for group and base layers") and M11's base.

- **Made on the stream gate.** The seed's operator act on the stream gate
  makes `:base` a one-owner layer: `:kind :base`, `:owner :operator` (the root
  actor, not a person), `:class :by-layer`, `:lock-grain :per-value`, and
  the root permission `[:operator :base :base]`. Its home is hash(:base) (P2).
  Its first facts are ordinary facts through that gate (ruling 9), and
  their ids are constants in the rig's seed (item 59's default). The owner
  id (phase 2's `:operator`, **[PV-F12]**) and the root pid are first-record
  placeholders (M22).
- **Its lock rules follow its kind from day one** (phase 2's to build):
  shared-layer rules, 7b as written, locks kept in the record. The root
  actor has no person lock, so there is no owner-required wrap. Nothing in
  this stage reads the owner for a wrap.
- **Re-classed when the first group arrives.** The rig's operator helper
  `make-group!` reads the base's class in force. While it is `:by-layer`,
  the helper first offers the base's re-class, `{:e :base :k :class :v
  :by-entity}` (nil-tagged, so on the stream gate, P16), and waits for its
  answer. Then it offers the group's making act to this gate, standing on
  the re-class fact with its stamp carried (M6). The only order between the
  two stores is that stood-on (the sharpening). Every later group finds the
  base by-entity and skips the first step.
- **What the re-class does.** Only the gate and placement change. The
  stream gate refuses the base's `:by-layer` names from the re-class on
  (`:class-mismatch`, stage 1's code). This gate orders the base as a
  re-classed one-owner layer (M5): its settings from `$$layers [:base]`;
  its stream-era permissions (the root and everything granted beneath it)
  and heads read as settled history and merged with this store's delta.
  Nothing about locks changes: the stream-era rows keep their wrapped locks
  where they are, and the base's micro-era values follow the same rules.
  The base's `:owner :operator` stays in its settings, and at this gate its
  control facts are the operator's (M14).
- **Against the model.** The model's world has the base shared from the
  start. The rig's seed re-classes it before any model history is
  replayed, so the comparisons run on the same world; the seed is the only
  place the two differ.

### D. The settled frontier (RIG.md R5, default R3)

- **The frontier id (first-record, M23).** The id of the last microbatch
  every task has committed: the value of `ops/current-microbatch-id` minus
  one, written by block 0 on every task (M7). Its class and its sameness
  across a retried attempt are the probe's (§G). Every micro row, record,
  head change, permission change, settings version, face and lease row
  carries the `:batch` that wrote it. The id is this topology's own
  counter. A kept store that could ever re-create the topology would own
  the sequence instead (a counter on task 0, advanced by the leader), with
  the same reader's rule.
- **The read-through rule.** A reader takes F once for a whole read, from
  any one task, and passes the same F to every task it reads. Any task will
  do, because batch F + 1 began only after every task had committed F (RQ
  7). The reader hides everything with `:batch` above F. A reader keeps the
  largest F it has used and never reads at a smaller one.
- **The query.** `micro-frontier` is `foreign-select-one [(keypath
  :frontier)] $$micro-task {:pkey 0}`: one seek. Every reader-facing query
  takes F explicitly: `micro-lookup [name digest envelope-fp F]`,
  `micro-act [e name F]`, `micro-lease [lease-name F]`. An F of nil means
  the reading task's own. `micro-lookup` answers, in order: a faces entry
  under `[name envelope-fp]` at or below F gives its reason (so a resend
  refused on its face is told so even when the name has a record); else a
  record at or below F gives the record when its parts digest matches and
  `:name-taken` when it differs; else `:no-answer`, and the offerer polls.
  Three reads at most (F, faces, record), all on the name's task.
  **[PV-F10]** A faces entry is written keep-first, `[(keypath name :faces
  fp) (term #(or % face))]` in block 2a (one read, on the rare face path):
  an identical envelope refused again in a later batch would otherwise move
  the entry's `:batch` above a reader's F and hide a face it had shown
  (M7's "never overwritten"). It is block 2a's one `term`; every other
  write stays a `termval`.
  `micro/visible-at? row F` is the one predicate, pure, that the reads
  stage builds shared-layer reads on.
- **What a read entry records** (R3): for a shared layer the moment part
  `{:frontier F}`, in the place where a one-owner layer's entry has `{:stamp
  s}` inline. That is the read exit's own slot (PLAN-read-exit.md in the
  reads worktree, line 128 at its commit 5a6bb732, **[PV-F9]**: "will put
  `{:frontier id}` in its place (phase 3's settled-frontier id)"), so this
  plan fixes only what F is: the Long above. For a re-classed layer the frontier
  alone: its stream-era facts are all before the re-class, and the re-class
  is before every batch that orders the layer. A stream-side setting after
  the re-class (P16) is a stamped stream fact the entry does not bound, an
  open edge that is stage 5's.

### E. Stamps through `rig.store.clock` (default R2)

Everything this gate stamps is a hybrid long from `rig.store.clock`
(tonight's build): next = max(ms-now × 65536, last + 1, largest stood-on +
1). For a micro act, `last` is the maximum of its touched tasks' clocks (from
W) and the stamps it replaces; `stood-on` is its carried stamps; ms-now is
the leader's wall, read once per batch (`TopologyUtils/currentTimeMillis`).
The call goes through `gate/decide`, which is reused, so both gates stamp by
one function. Lease acts, refusals, grants and revokes are stamped the same
way. `$$micro-task :clock` holds a hybrid long. R16's bound (2^62) sits far
above ms × 65536 (about 1.2 × 10^17 today).

### F. Tests (these replace the lists under "Namespaces and tests" where they differ)

From the model, run in the rig and, where the model has the same history,
asked of the model too:

- **The micro cases** (`model.clj` `micro-prepare`, `micro-decision`,
  `micro-commit`, `stamp-for` for `:micro`, `lookup`): a batch decided in
  order, each offer seeing the ones before it; a name already decided,
  skipped and answered from the record; a name taken by other content,
  refused by digest; a name made for another layer or class, refused on its
  face; `refusal`'s branches at this gate (a fact outside the act's layer,
  a class mismatch, the four permission reasons, stale replaces); the
  stamp over every touched task including the name row's (`:name-row`);
  an answer found by name (`lookup` with `:findable-by-name`).
- **The permission cases** (`refusal` under `:permissions-in-their-layer`,
  and `scenarios.clj` `d-cases`): D1, a group write under Alice's session
  permission kept in her hand layer, with the session revoking it between
  prepare and commit, gives `:missing` (refused
  `:permission-from-another-layer`); D2, a group write under her group
  permission after it was revoked in the group, gives `:missing` (refused
  `:permission-revoked`). The rig's outcomes are compared with
  `formal.model/run` on the same histories.
- **The group's six A cases** (`a-cases`, the ones "in the group"): run
  through this gate once phase 2's locks merge. Their lock assertions are
  phase 2's; this stage provides the path and the wrapped lock in the row.

The model's permission ids map to the rig's (M20): `[p l :own]` becomes
`[p l l]` when p owns l (the layer's root), else `[p l l root]` beneath l's
root; `[p l :session]` becomes `[p l :alice-hand]`, kept in the hand layer,
so it is refused as from another layer, as under the model's baseline.

The rig's own, each named in the step:

- **The revocation race.** D1 as above; in one batch, a revoke then a write
  citing the pid (the write refused) and a write then the revoke (the write
  admitted), by M2's order; a revoke of an ancestor (Alice's group
  permission) cutting her session's permission beneath it, in the same
  batch after it and in the next batch; a stream-side revoke of her
  session's permission in her own layer leaving her group permission live
  (a gate checks only its own layers' permissions).
- **Revoke cuts everything below, on the stream gate:** in `:alice`, the
  root `[:alice :alice :alice]`, the session's beneath it, an agent's
  beneath that; revoking the session's refuses the agent's writes
  `:permission-revoked` and leaves the root's admitted.
- **A batch that fails on one task leaves nothing.** One-shot throws, one
  at a time, in block 1 (the gather), block 2a, block 2b and block 2c, each
  on one task: after the retry, one record, one stamp, the rows once, the
  lease rows consumed once, the frontier past the batch, and the code run
  at least twice (R4); a reader through the frontier sees none of the
  batch before its commit.
- **A reader never sees half a batch through the frontier.** M15's run
  (300 batches or more, several readers, two tasks, zero violations), now
  with the explicit F: every read in a pass uses one F; the same readers
  bypassing the frontier report their count of inconsistent pairs, expected
  above zero, as in phase 0.
- **Re-class moves a layer, and its order promise ends there.** Re-class
  `:alice-agent` through the stream gate; its `:by-layer` names are then
  refused `:class-mismatch` and its `:by-entity` offers are decided here; two
  offers appended in one order whose names' UUID7s sort the other way, put
  in one batch (pause the topology, append both, resume), are decided in
  UUID7 order (the single-owner order promise is gone); a
  stream-era head replaced here gets its tombstone and the frozen head is
  untouched; a second replace of it is refused stale.
- **The base re-classed at the first group.** The seed makes the base on
  the stream gate; a base offer there is admitted; `make-group!` re-classes
  the base, then makes the group standing on it; after that a `:by-layer`
  base name is refused `:class-mismatch`, a `:by-entity` one is admitted
  here, a stream-era base fact can be replaced here (tombstone), and the
  stream-era row's lock record is byte-identical before and after the
  re-class.
- **A missing lock refused on its face:** a lock id never leased; a lease
  already consumed; a lease made for another layer; one of another session;
  one whose owner's person lock is destroyed. Each gives a `:no-such-lock`
  face through `micro-lookup`, no record under the name, and the same name
  resent under a fresh lease is admitted.
- **A lease row consumed in the batch:** after an admitted offer the lease
  row is gone and the value opens through its row's lock (phase 2's
  `open-value`); after a refused offer (no permission) the row is gone and
  nothing opens; after a resend answered from the record, no cited row
  exists (the first decision consumed them) and nothing was written; two offers citing one lock in one batch, the first by M2's order
  uses it and the second is `:no-such-lock`; the lease act itself answered
  by name, its locks readable through `micro-lease` only once its batch is
  at or below F.
- **A permission cited from another layer refused:** D1; a chain whose
  ancestor lives in another layer; on both gates.
- **The value check, before and after a forget:** a same-name offer citing
  the recorded lock ids with other plaintext gives `:name-taken` before the
  value is forgotten, and the recorded answer after (phase 2's forget; the
  forgotten value is not checked); a same-name offer with other content
  under fresh locks gives `:name-taken` by the parts digest, before and
  after.

**[PV-F2, F3, F4, F8] Tests the validation adds.** A resend by a door
that lost its locks (fresh lease, the same plaintext): answered from the
record, its new lease rows consumed; the same with other plaintext:
`:name-taken` on the face, its rows left; a resend citing another
session's lease ids: answered, the other session's rows untouched. Two
envelopes under one name in one batch (pause, append both, resume): the
earlier by `[uuid7 name fp]` decided, the other `:name-taken` on its face,
the rows the decided envelope's bytes, with the same lock ids and other
plaintext and with other lock ids. A session close in the group: its
unconsumed rows gone, a later offer citing one `:no-such-lock`. The
cascade: a revoke of Alice's group permission cuts her session's and an
agent's beneath it (committed and granted earlier in the same batch); a
grant beneath a revoked parent is born cut; a cut row keeps its first cut
when a second ancestor is revoked; a reader at F between the two sees the
first.

The pure tests (`micro_prepare_test.clj`) gain generated sealed offers,
chains of up to four pids, and double-cited locks; `skeleton`, `prepare`,
`row-of` and `permit/refusal` never throw.

### G. The probe (a Rama question the plan must settle)

One probe ran on the in-process cluster on 26 September at 02:08 IST, under
`flock` on the relay's cluster.lock file, one cluster at a time (Rama 1.6.0, 4 tasks, 2 threads, 1 worker; source
`runs/probe-micro-2026-09-26/rig/probe/micro_decl_test.clj`, report
`runs/probe-micro-2026-09-26.txt`, raw log `runs/probe-micro-2026-09-26.raw.txt`;
run 2: 8 passes, 0 failures, 0 errors). Each answer is what ran:

- **M1 is settled: a plain `defn` can declare the store's micro side.** A
  `defmodule` whose body was the one line `(declare-micro! setup topologies)`
  launched, and the `defn` declared a depot, a tick depot, a microbatch
  topology with four PStates and its `<<sources`, and a query topology. The
  topology processed an append (`$$by-tag :q1 = 7`) and the query answered
  through `foreign-invoke-query` (7). So `module.clj` gets the micro store
  through one function called by one line, `(micro/declare! setup
  topologies)`, with its `:require`; no macro is needed. Rama allows it
  everywhere this plan needs it.
- **The frontier id's class is settled.** `ops/current-microbatch-id` gave a
  `java.lang.Long`, `dec` worked on it in dataflow, and a batch retried
  after a one-shot throw saw the same id on both attempts (4 and 4, dec 3).
  The ids rose by one per batch (2, 4, 15 across the run, with tick batches
  between). So §D's frontier, `b − 1` written in block 0, is a Long and the
  same on every retry of its batch. The throw was fatal to the worker, as
  phase 0 found, and a client read during the restart threw
  `ExecutionException :connection-closed`: an offerer's poll must treat a
  read that throws as "not yet" (the rig's `offer-micro!`).
- **A vector as a PState's top-level key works.** The schema
  `{clojure.lang.PersistentVector (fixed-keys-schema {:x Long})}`, written
  after `(|hash *k)` with `*k` = `[:group :by-entity :offer "abc"]`, read
  back by the client with `foreign-select-one (keypath k)` as `{:x 3}`. So
  `$$micro-names` keeps the name vector as its key, and the `(pr-str name)`
  fallback is not needed; the depot's `hash-by` on a name vector and the
  PState's key partitioner agree, which §A's routing rests on.
- **A tick depot as a second source keeps batches running with no data.**
  With no appends for 2 s, the microbatch ran 10 times and the tick emitted
  5 times. So the frontier advances after the last data batch (M7); the
  cadence was about 200 ms here. No control run without the tick was made.

Still open for the build's first checks, as the 25 September plan listed
them: `materialize>` after the post-agg of `+map-agg` and sourcing it in the
next `<<batch` (the fallback is named there); a `<<cond` whose branches hold
different partitioners in a pre-agg-only `<<batch`; vector-key prefix order
for stage 5's `[L k]` range (not needed by §A, whose lease rows are keyed by
index under the name).

### H. Partitioning efficiency, recomputed

The categories of the table below keep their meaning; the seeks per offer
change. A sealed act adds its lease read on the arrival task in block 1 and
again in block 2b (one seek each, one iteration per cited lock), and the
permission chain adds up to two reads on the layer's task (a session's pid
under a person's under the root: three reads in all). Routing by session
turns the common single-entity act's entity reads and row writes into one
hop, with no extra seek. Control-only acts (g) are unchanged.

Block 1's reads, the convention of the 25 September table (block 2b's reads
are stated after it). The numbers are the same at N = 1, 16 and 128, so one
table stands for the three.

| Data category | Proportion | Seeks/op at N = 1, 16, 128 | Iterator reads/op |
|---|---|---|---|
| (a) ordinary sealed act, one entity, no replace: lease 1 + name 1 + settings 1 + chain 3 + clock 1 | 0.50 | 7 | 1 |
| (b) as (a) with a replace: + head 1 | 0.18 | 8 | 1 |
| (c) on two entities: + clock 1 | 0.05 | 8 | 2 |
| (d) into a re-classed layer: lease 1 + name 1 + settings 2 + chain 3 in each store + clock 1 | 0.12 | 11 | 1 |
| (e) a resend: lease 1 + name 1 + first row 1 | 0.10 | 3 | 1 |
| (f) a face refusal (a missing lock has read its lease) | 0.02 | 1 | 1 |
| (g) an operator act (make, grant, revoke): no lease | 0.03 | 4 | 0 |

Weighted seeks = 3.50 + 1.44 + 0.40 + 1.32 + 0.30 + 0.02 + 0.12 = 7.10;
weighted iterator reads = 0.50 + 0.18 + 0.10 + 0.12 + 0.10 + 0.02 + 0 =
1.02; both the same at N = 1, 16 and 128. Flat in N: every read is on the
arrival task, the name's task, the layer's task or an entity's task, each a
fixed number of point or range reads. Block 2b adds, per sealed record, one
lease read and the 25 September plan's one name-row read; block 2c only
writes, one deletion per cited lock. Against the 25 September table (4.31),
the lease adds 1 and the chain 2 (typically), both required by named lines
(R1, R7).

**[PV-F7] The totals per operation, across all tasks.** The template counts
every seek an operation causes on every task, so block 2b's reads and every
`$$persons` read are in. Assumptions: one subject per value act on
average; one cited lock per value fact; the cascade of PV-F8 (one
permission row per offer). A lease act is its own category, at phase 2's
0.01 per offer, taken from (a).

| Data category | Proportion | Seeks/op, N = 1, 16, 128 | Iterator reads/op |
|---|---|---|---|
| (a) sealed act, one entity, no replace: block 1 lease 1 + `$$persons` of `:under` 1 + of the subject 1 + name 1 + settings 1 + permission 1 + clock 1; block 2b lease 1 + `$$persons` 2 + name (with `:fp`) 1 | 0.49 | 11 | 2 |
| (b) as (a) with a replace: + head 1 | 0.18 | 12 | 2 |
| (c) two value facts on two entities: + clock 1 | 0.05 | 12 | 4 |
| (d) into a re-classed layer: + settings 1 + permission 1 (both stores) | 0.12 | 13 | 2 |
| (e) a resend, yes record: lease 1 + name 1 + entity row 1 + `$$persons` to unwrap R 1 + `:under`'s for a re-leased door 0.5; block 2b name 1 | 0.10 | 5.5 | 1 |
| (f) a face refusal: lease 1 + `$$persons` 0.5 + name 1 + the keep-first face write 1 | 0.02 | 3.5 | 1 |
| (g) an operator act (make, grant, revoke): name 1 + settings 1 + clock 1 + a grant's parent 1 or a revoke's subtree amortised; block 2b name 1 | 0.03 | 5 | 3 |
| (h) a lease act: name 1 + settings 1 + permission 1 + `$$persons [who]` 1 + clock 1; block 2b name 1 | 0.01 | 6 | 0 |

Weighted seeks = 5.39 + 2.16 + 0.60 + 1.56 + 0.55 + 0.07 + 0.15 + 0.06 =
10.54; weighted iterator reads = 0.98 + 0.36 + 0.20 + 0.24 + 0.10 + 0.02 +
0.09 + 0 = 1.99; proportions sum to 1.00; the same at N = 1, 16 and 128:
every read is on the arrival, name, layer or entity task, a fixed count per
operation. With the walk instead of the cascade, (a) to (c) add 2, (d) adds
4, (g) drops the parent read and (h) adds 2: 12.47 weighted, 1.93 more per
operation, flat in N too. Block 2c and the close only write.

### I. New and revised rig choices

Each is a pick where the rulings and tonight's defaults are silent. "First-
record" marks a pick that touches a record (its bytes, a stamp, an id, what
an entry holds): the simplest placeholder, listed in the receipt.

- **M4, revised.** One client depot, `*micro-offers`, partitioned by the
  lease name of the first lock it cites, else by its first fact's entity.
  Why: §A; the common control act keeps its local gather.
- **M8, revised (first-record).** A face refusal, a taken name or a missing
  lock is recorded under `[name envelope-fp]` in the name entry's
  `:faces`, never as the name's answer. The envelope fingerprint is P6's keyed
  HMAC (the rig constant secret) over the canonical sealed envelope minus
  its name, which the door and the gate both hold. It is not R1's reuse
  digest, which must not be taken over sealed bytes: a face refusal belongs
  to one attempt, and a resend sealed again has its own envelope fingerprint and
  its own faces entry. Why: the parts digest cannot tell an honest resend
  from one whose values differ, and a face refusal must not shadow the
  record an honest resend is owed.
- **M11, revised.** The group is seeded through this gate as the model's
  `:group` with its root `[:group :group :group]` and Alice's and Bob's
  permissions beneath it; the base as §C. Why: R7, R8.
- **M12, revised.** The row seam is §A's block 2b; the lock slots take
  phase 2's shapes; `$$persons` is read locally where a wrap or an unwrap
  needs it. Why: sealing is now at the door, not a seam to fill later.
- **M16 (first-record).** A shared layer's leases are minted by this gate
  from a lease act in that layer; the rows sit beside the lease act's name
  row, `$$micro-names [lease-name :leases i]`, sealed under the lease
  act's `:who` person lock (**[PV-F1]**, phase 2's V-F2); a leased lock's id
  is `[lease-name i]`; the operator's row is `{:under nil :sealed K}`, K
  bare (**[PV-F5]**). Why: §A; the stream gate cannot consume a row of this
  store.
- **M17.** A first offer's lock work (unlease, open, subjects, re-wrap,
  value digest) runs on its arrival task, where its lease rows are; the
  leader sees digests and ids only. Why: a lock and a plaintext value stay
  on one task.
- **M18.** A resend with a yes record is value-checked on each value's
  entity task under the recorded lock, the resend's sealed bytes carried
  there; a forgotten value is not checked. Why: phase 2's record path, with
  no bare lock or plaintext crossing tasks.
- **M19.** Leases are consumed in block 2c, after the rows, from the fold's
  output: every cited row of every act decided in the batch, yes or no,
  and (**[PV-F2]**) every owned row of a resend answered from the record;
  face refusals consume nothing; a session's close deletes its unconsumed
  rows in the layer (**[PV-F4]**). Only rows block 1 read and found the
  offer's session's and layer's are deleted. Why: §A, and phase 2's rule,
  so both gates consume alike.
- **M20 (first-record).** A permission id is `[who layer in]` or `[who
  layer in parent]`, at most four deep; the check walks the chain; a revoke
  writes one row and cuts by the walk. Why: R7, with the fewest lines in
  the stream gate and a bounded check.
- **M21.** `rig.store.permit` holds the one check both gates call; a
  holder is covered as the offer's `:who` or its `:session`. Why: "the
  rig's gates must decide the same way".
- **M22 (first-record).** The base's owner and root holder is the root
  actor, `:operator` in the rig as in phase 2 (**[PV-F12]**); a
  group's root holder is its layer id; the root permission is granted in
  the making act; `make-group!` re-classes the base before the first
  group. Why: R8, ruling 9.
- **M23 (first-record).** The frontier id is the microbatch id of the last
  batch every task committed; a read entry records `{:layer L :frontier F}`
  for a shared layer. Why: R3; it is the one number that already bounds
  every row.
- **M24 (first-record).** A lease is scoped to one session and one layer;
  all the locks one act cites come from one lease; `:lease` is a control
  key; n ≤ 256 per lease act. Why: one lease home per act, so its lock work
  is on one task, and a session's acts move to another task with each new
  lease.
- **M25.** For a re-classed layer (the base included), a nil-tagged
  `:forget` goes to the store that holds its target: a micro-era target
  (its name tagged `:by-entity`) to this gate, landing on the value's
  entity task (M9); a stream-era target to the stream gate. Settings
  (`:class`, `:lock-grain`) stay with the stream gate, as P16. Why: P16
  sent every nil-tagged act of a re-classed layer to the stream gate, which
  cannot reach a row of this store; the client reads the target's tag, so
  the routing needs no read.

**The schema addition** for §A, one field in `$$micro-names`' fixed-keys
value, beside the lease act's own name row (the same key and partitioner, so
one PState by the merge rule):

```clojure
:leases (map-schema Long                                     ; i -> the lease row [PV-F1]
          (fixed-keys-schema {:under   clojure.lang.Keyword    ; phase 2's: the lease act's :who person; nil for the operator
                              :sealed  <phase 2's bytes class> ; K sealed under :under's person lock; K bare when :under is nil
                              :layer   clojure.lang.Keyword
                              :session clojure.lang.Keyword
                              :batch   Long})
          {:subindex-options {:track-size? false}})
:fp     String                                               ; [PV-F3] the envelope fingerprint of the envelope decided under this name
```

The map is subindexed although n ≤ 256 (M24): its entries are removed one
by one as they are consumed, and a subindexed entry is one seek to delete.

Row `:v` keeps its type (String): the base64 text of the sealed bytes,
phase 2's form. The faces map's key keeps its type (String): now the
envelope fingerprint.

**Files this stage touches.** New: `src/rig/store/micro.clj` (the gate, the
queries, the offerer's side, `make-group!`, `open-session!`) and
`src/rig/store/permit.clj`. Changed: `module.clj` (one line and its
require, §G), `gate.clj` (the two permission lines of §B),
`envelope.clj` (`pid?`), `client.clj` (M13's dispatch, and M25's). Phase 2
also changes `gate.clj` and `envelope.clj` tonight; the lines here are in
functions its sealing does not need (`refusal`'s permission clauses,
`pids-to-read`, `pid?`), and the merge is where a mismatch would show.

### J. Open after this revision

Each is either Sid's, a later stage's, or the build's first check; none
blocks the build of this stage.

1. **A refused name reused with other content** (**[PV-F2]**, phase 2's
   V-F1). The parts digest no longer holds lock ids, so other value content
   under a refused name has the refused act's parts digest and hears the
   recorded no; other non-value parts (another entity, key, replace, mark
   or control value) still give `:name-taken`. A refused act keeps no
   value digest, so nothing can tell its value content apart: phase 2's
   "For Sid" item 3, the same on both gates.
2. **"Nothing recorded" for a missing lock**: this plan writes a faces
   entry that is not the name's answer (§A). If the default means no entry
   at all, the change is one line.
3. **The permission walk against the cascade** (§B, **[PV-F8]**): the
   cascade is adopted (the walk costs about 1.9 seeks per operation more,
   §H); the walk stays written as builder A's fallback if tonight's merge
   with phase 2's `gate.clj` favours the smaller change. Both touch no
   record.
4. **Who may grant beneath a permission.** The operator only, tonight (R13);
   delegation by a holder is open and changes no record.
5. **Coverage by `:who` or `:session`** is a claim the gate cannot check,
   as every offer's `:who` is at launch (operator trusted, no signing).
6. **Unused leases.** A lease whose door never sends stays as a row sealed
   under its `:who` (bare for the operator), opening nothing in the store,
   until its session's close (**[PV-F4]**); an expiry is not designed. A stream lease for the base minted before its re-class is
   phase 2's to consume or expire; the door leases here after.
7. **Phase 2's shapes** (**[PV-F1], [PV-F2], [PV-F4]**: now aligned with
   the locks branch's b11525b4 and 6320b312, after its validation: lock ids
   out of the parts digest, the record path with the resend's own lock,
   consumption on the record path, rows sealed under the lease act's
   `:who`, the operator's bare row, `:session-closed`; this plan's session
   close is §A's) were first taken from its revision as committed at 02:12
   (the lease fact, `lease-ids`, the lease row, `unlease`, the two digests,
   the record path, consumption). Where the built code differs, phase 2's
   wins and the adapters are in `micro.clj`.
8. **The root actor and the operator**: the root actor (`:operator` in the
   rig, phase 2's pick, **[PV-F12]**) owns the base and holds its
   root; the operator offers the seed and control acts and cites no
   permission (the model's `exempt?`). Whether the root actor stays the
   operator's identity in a kept store is Sid's.
9. **The frontier id** is Rama's microbatch id (M23). A kept store that
   could re-create the topology owns the sequence instead.
10. **A re-classed layer's stream-side settings after the re-class** (P16)
    are not bounded by a read entry's frontier: stage 5's.

## Scope of this stage, in one paragraph

> **Revision, 26 September:** values now arrive sealed under leased locks
> (§A), so "values are stored as offered" and "a slot for the wrapped lock
> that stage 2 fills" below are superseded; the base is made on the stream
> gate and ordered here after its re-class (§C).

The micro store end to end for layers placed by entity: the shared layers
(group, base) and one-owner layers re-classed to by-entity. A second depot
and a microbatch gate, in their own namespace `rig.store.micro`
(`src/rig/store/micro.clj`), declared into the one module of P1 by one call
from `rig.store.module/Store`. The gate decides a whole batch in one order
where each offer sees the ones decided before it, as `model.clj`'s
`micro-prepare` does, admits an act whole across tasks (I-G6, RQ 5), and
writes the name row in the same batch (I-G4). Permissions are facts in the
layer they govern, checked only here for the layers this gate orders (I-P1);
a revoke; the revocation race (D cases). A re-classed one-owner layer moves
here and its stream-era heads, permissions and settings are read as settled
history (R 105). The settled frontier of R5, so no reader, the offerer
included, sees half a batch (I-O5). R6 is picked. Seeding the shared layers
and the permissions that live in them (model `seed-permissions`, shared side).
Values are stored as offered; every row carries a slot for the wrapped lock
that stage 2 fills; promotion's landing is stage 4's; reads as of a moment
are stage 5's. Stage 2 is built by another session at the same time (R15),
so this plan touches no file of stage 1 or stage 2: what it needs from that
code is listed under "What this stage takes from the stream store".

The rulings this plan implements are I-G1 to I-G8 on the micro side, I-O1
(the micro side and the re-class), I-O2, I-O3, I-O4, I-O5, I-P1, I-P3, I-P4,
I-P5, and the operations OP3, OP4 (micro side), OP5 (shared layers), OP6 and
OP7 on their micro side, RD1's name row, and E1 to E4 for shared layers. The
open items that fall to this stage are O3 (micro side), O4 (micro side), O6
(micro side), O7, O9 (shared layers) and O17 (left open, see M14); O22 is
left to stage 5; each pick is under "Rig choices proposed", never as a
ruling.

## The shapes every section below uses

Everything the stream plan fixed is reused unchanged: ids as keywords (P3),
the name `[layer class scheme uuid7]` and its tag (P4), the envelope and its
total `parse` (P5, [F6]), the derived digest (P6), the permission id
`[who layer in]` (P8), the stamp formula with carried stood-on stamps (P9),
the making act (P10), canonical EDN text in the value slot (P12), retracts
(P13), the subject slot (P15), the routing of nil-tagged names (P16). Only
what differs at this gate is stated here.

**Which names reach this gate.** A name whose tag class is `:by-entity`, and
a nil-tagged name (a store-placed act: every fact key in the placed set) of a
*shared* layer (P16's client rule). Schemes: `:offer` from people and the
operator; `:landing` (stage 4's, derived from a request's name) is accepted
as data (M4); `:crossing` is refused on its face (a read-out is never a
micro name). A name tagged `:by-layer` is refused on its face as
`:wrong-gate`, the mirror of the stream gate's refusal, so a name reaches one
gate only whatever a client does.

> **Revision:** the skeleton also carries `:locks`, the cited lock ids, and
> its `:digest` is phase 2's parts digest, which holds no value (§A).

**Offer skeleton** (what travels to the deciding task, "the leader", in
block 1; never a value): the parsed offer minus every `:v`, plus its digest,
the maximum carried stood-on stamp, and, per fact, `{:e :k :replaces :mark}`
with its index. Sizes: about 200 bytes plus 60 per fact.

**Batch order** (M2; **[PV-F3]**: `[uuid7 name fp]`, the envelope
fingerprint as the last tiebreak, so two envelopes under one name never
tie): the offers of one microbatch are decided in ascending
order of `[uuid7 name]`, the UUID7 of the name compared as `java.util.UUID`
(time-ordered, unique-ids.md) and the full name vector as the tiebreak (the
same uuid can appear under two schemes). Deterministic for a fixed batch, so
identical across retries of one microbatch id; close to arrival for one
client; two clients with skewed clocks can differ from the model's inbox
order, and phase 8 reports it.

**Answer record**: the stream plan's record (answer, reason, stamp, digest,
who, class, permission, session, because-of, claimed-when, subjects) plus
`:batch`, the microbatch id that decided it (M7). It is written under the
name on the name's task (the name row of I-G4) and under every entity the
act touches (OP3's "kept on every task it touches"), in the deciding batch.

**Log row** (per fact, on the fact's entity task): `{:layer L :k k :v text
:replaces fid-or-nil :mark #{...} :lock-id nil :lock nil}`. The row names
its layer because an entity's rows come from many layers. `:lock-id` and
`:lock` are stage 2's slots, in the shapes the locks plan gives them
(M12). A row is addressed by `[e name idx]`; the fact id stays `[name idx]`
(M9).

**Batch-stamped, never overwritten** (M7): every micro row carries the id of
the batch that wrote it; a later change is a new field with its own batch id
(a head's `:replaced-batch`, a permission's `:revoked-batch`) or a new entry
(a settings version keyed by batch), never a rewrite of an earlier field. A
reader at frontier F hides what a batch above F wrote. This is how R5 is
kept; the rule is stated once here and applied in every schema below.

> **Revision:** the stamp is `rig.store.clock`'s hybrid long (§E); the
> terms below stand, in that unit.

**Stamp** (I-O2, model `stamp-for` for `:micro`): `max(wall, max over the
touched tasks (clock + 1), max carried stood-on stamp + 1, max replaced
stamp + 1)`, the wall from `TopologyUtils/currentTimeMillis` read once per
batch on the leader, the touched tasks being every task a fact of the act
lands on plus the name row's task (the model's `parts`), their clocks taken
from the fold's working state so two acts on one task in one batch get
strictly increasing stamps. Refusals are stamped too (model `decide`).

## Reads

> **Revision:** every reader-facing query takes an explicit F;
> `micro-lookup` takes `[name digest envelope-fp F]`, the digest being
> phase 2's parts digest (§D, M8 revised); `micro-lease` and
> `micro-leases-of` are added (§A).

Point reads route by the first key; reads that need two PState reads on one
task are query topologies (phase-1-plan Step 1), which is what the frontier
makes of every reader-facing read.

| read | who | how | seeks | note |
|---|---|---|---|---|
| RD1 the answer by name, through the frontier | the offerer, stage 4, tests | query `micro-lookup [name digest]`: `(|hash name)`; `[(keypath :frontier)]` on `$$micro-task` → F; `[(keypath name :answer)]` on `$$micro-names`; if absent, `[(keypath name :faces digest)]` | 2, 3 when no answer | `:yes`/`:no` with the record when `(:batch rec) ≤ F` and the digest matches (nil digest: the record as data, [F13]); `:name-taken` when it differs; a face refusal's reason when a faces entry ≤ F exists (M8); else `:no-answer`, and the offerer polls (I-G3, no ack on a microbatch depot) |
| the rows of an act on an entity, through the frontier | stage 5, tests | query `micro-act [e name]`: `(|hash e)`; F; `[(keypath e :answers name)]`; if `≤ F`, `[(keypath e :log name) ALL]` | 2 + rows | rows in index order; `:v` decoded by the client |
| the frontier alone | tests, stage 5 | `foreign-select-one [(keypath :frontier)] $$micro-task {:pkey k}` | 1 | any task; every task's value is a safe lower bound (M7) |
| a layer's settings, latest or as of F | the gate (in the gather), stage 5, tests | `[(keypath L :settings) (sorted-map-range-to-end 1)]` on `$$micro`, or `(sorted-map-range-to (inc F) 1)`; for a one-owner layer `[(keypath L :settings)]` on `$$layers` | 1 (2 for one-owner: miss, then `$$layers`) | settings versions keyed by batch; `$$layers` is the stream store's, on the same task (P2's hash on the layer id) |
| a permission's state | the gate, tests | `[(keypath L :permissions pid)]` on `$$micro`; for a one-owner layer also on `$$layers` | 1 or 2 | live iff granted in either and revoked in neither (M5) |
| whether a fact heads its chain | the gate, stage 5 | `[(keypath e :heads [L k fid])]` on `$$micro` → `{:stamp :batch :replaced-by :replaced-batch}`; for a re-classed layer's stream-era fact `[(keypath L :heads [e k fid])]` on `$$layers` and `[(keypath L :replaced [e k fid])]` on `$$micro`, both on hash(L) | 1; 2 for stream-era | unreplaced iff present and `:replaced-by` nil (or above F for a reader) |
| a task's clock | the gate, tests | `[(keypath :clock)]` on `$$micro-task` | 1 | the micro store's own clock per task (the stream `$$clock` is another topology's) |

The gate's own reads per offer happen in block 1 of the batch, in this order
and on these tasks, each conditional on the one before (so a decided name
costs one seek): on hash(name) the name row; on hash(layer) the settings (micro
first, `$$layers` on a miss), the permission row(s), and for a replacing
fact of a one-owner layer the stream-era head and its tombstone; on hash(e)
per entity the clock and the task id, and per replacing fact not found
stream-side the micro head. Counts are in "Partitioning efficiency". The
gate reads committed state: at the start of a batch every task has
committed the previous one (RQ 7), so the gate's view is whole without the
frontier; the frontier is for readers outside the topology.

## Writes

> **Revision:** OP5 no longer makes the base (§C); the seed row follows M11
> as revised (§I); a lease act is added (§A); an OP6 grant may name a parent
> (§B); a nil-tagged forget of a re-classed layer's micro-era value comes
> here (M25).

One client depot, `*micro-offers`; every write is an offer as shaped by the
stream plan (P5). By operation:

| op | offer | facts | decided by |
|---|---|---|---|
| OP3 offer an act into a shared layer | `:who` the person, `:permission` a pid whose `in` is this layer, `:class :by-entity`; name tagged `[layer :by-entity]` | ordinary facts on any entities, any number ≥ 1 | the leader's fold, this batch; rows written on each fact's entity task, the record on the name's task and under each entity |
| OP3 offer into a re-classed one-owner layer | as above; the layer's class fact (`$$layers`) says `:by-entity` | as above | as above; heads, permissions and settings from both stores (M5) |
| OP2 resend | the same map | — | skipped as decided in the same batch (the fold's name row), answered from the name row in a later batch; nothing written |
| OP4 replace / retract | as OP3 | a fact with `:replaces fid` (P13) | the fold; a stale or doubled replace refuses the whole act; a stream-era head of a re-classed layer is tombstoned in `$$micro [L :replaced]` rather than deleted from `$$layers` (M5) |
| OP5 make a shared layer | `:who :operator`, `:class :by-entity` | `{:e L :k :kind :v :group|:base}`, `{:e L :k :members :v #{person ...}}` (group), `{:e L :k :class :v :by-entity}`, `{:e L :k :lock-grain :v :per-value}`, with the layer's grants in the same act (P10) | the fold; the class in force is the act's own class fact when the layer has none; `:kind`/`:members` on a made layer refused `:layer-already-made` [F8] |
| OP6 grant | `:who :operator` | `{:e (perm-entity who) :k :permission :v {:id pid}}` in the layer `in` of the pid | the fold; projected into `$$micro [L :permissions pid]` on hash(L) |
| OP6 revoke | `:who :operator`, `:stood-on {grant-fid stamp}` | `{:e (perm-entity who) :k :revoke :v {:permission pid}}` in the permission's layer | the fold; refused `:stale-revoke` when no live grant is found in either store's index (P8); every later offer in the batch's order citing the pid is refused `:permission-revoked` (the race, OP6) |
| OP7 re-class | — | — | the stream gate (P16): the `:class :by-entity` fact is nil-tagged and stays on the layer's home; this gate only *reads* it. A `:class` fact reaching this gate outside a making act is refused `:unsupported-reclass` (O9: a shared layer's re-class is not ruled) |
| OP8 grain switch on a shared layer | `:who :operator` | `{:e L :k :lock-grain :v ...}` | the fold, as a new settings version; a person's is refused `:control-not-allowed` (R13, D11: no owner on a shared layer; O7) |
| forget in a shared layer | `:who :operator`, nil-tagged name, `:e` the value's entity (M9) | `{:e e :k :forget :v {:target fid}}` | the fold admits it as a control fact (R13) and writes its row; its effect on the lock is stage 2's, through the row seam (M12) |
| seed | the operator's OP5 and OP6 acts for the shared layers | the model's `layers` and `permissions`, shared side (M11) | the fold, before any history |

The writes of one batch, all `termval`s of values computed before any write
(so a retried attempt writes the same rows and Rama applies them exactly
once, microbatch.md "Guarantees"), by block:

- **Block 0, every task**: `$$micro-task :frontier` = the previous batch's
  id (M7).
- **Block 2a, from the leader's output**: per decision, the answer record at
  `$$micro-names [name :answer]` and at `$$micro [e :answers name]` for every
  entity of the act; per face refusal `$$micro-names [name :faces digest]`
  (M8); for a yes: per fact `$$micro [e :heads [L k fid]]` = `{:stamp s
  :batch b}`; per replaced fact either the full replaced value
  `$$micro [e :heads [L k r]]` = `{:stamp s_r :batch b_r :replaced-by fid
  :replaced-batch b}` (its old fields are known from the gather, so no read)
  or, for a stream-era head, the tombstone `$$micro [L :replaced [e k r]]` =
  `{:by fid :batch b}`; per setting fact one settings version
  `$$micro [L :settings b]` = the whole new map (kind, owner, class, grain,
  batch); per `:members` fact one `$$micro [L :members person]` per member;
  per permission fact `$$micro [L :permissions pid]` = `{:granted fid
  :granted-batch b}`; per revoke the full row with `:revoked fid
  :revoked-batch b`; per touched task `$$micro-task :clock` = the highest
  stamp given to it in the batch.
- **Block 2b, from the offers themselves**: per admitted act,
  `$$micro-names [name :stood-on fid]` = stamp per carried entry ([F2]); per
  fact `$$micro [e :log name idx]` = the row, with `:lock-id` and `:lock`
  from the stage-2 seam (nil in this stage), and `$$micro [e :locks lid]`
  when the seam says the lock is a row (nothing in this stage).

Nothing is deleted (I-L5's rebuild and purge, and M7). No increments, no
list appends.

## PState Design

Four PStates, all owned by the micro topology, grouped by key structure and
partitioner (the merge rule of phase-1-plan Step 2): entity-keyed data in
one, name-keyed data in one, per-task values in one; the fourth is the
stream store's `$$layers`, read here, never written.

### `$$micro` — everything keyed by an entity, on the entity's task

> **Revision:** a `:log` row's `:v` holds the sealed bytes and `:lock` the
> wrapped lock record (§A). `$$micro-names` gains `:leases` beside a lease
> act's name row (§I, the schema addition).

Candidates, costed for the dominant read (the gate's gather per offer) and
for the frontier rule:

- **Option A, separate PStates per piece** (`$$rows`, `$$heads`,
  `$$answers-by-entity`, `$$settings`, `$$permissions`, `$$members`), all
  keyed by an entity id and hash-partitioned: the same key and partitioner,
  so the split buys nothing and costs a partition's overhead per task each;
  the gather would pay a top-level seek per PState instead of one per entity
  entry. Rejected by the merge rule.
- **Option B, one PState keyed by entity** with a fixed-keys value: the
  ordinary entity's fields (`:log`, `:heads`, `:answers`, `:locks`) and the
  layer-as-entity's fields (`:settings`, `:permissions`, `:replaced`,
  `:members`), all subindexed children. The layer id doubles as the entity
  its setting and permission projections live under, as the model's
  `:e layer` does; a layer id and an entity id never collide in the model's
  world, and a collision would only co-locate two maps under one key.
- **Option C, heads deleted on replace** (the stream store's `NONE>`): one
  seek less per stale check? No: the check is one seek either way (present
  and unreplaced vs present); but a deleted head breaks the frontier (a
  reader at F below the replacing batch must still see the old head) and
  stage 5's chain history. Rejected on R5.

Chosen: B. Schema:

```clojure
(declare-pstate mb $$micro
  {clojure.lang.Keyword                                        ; entity id, or a layer id used as one
   (fixed-keys-schema
     {;; an ordinary entity
      :log         (map-schema clojure.lang.PersistentVector    ; name
                     (map-schema Long                          ; idx -> row
                       (fixed-keys-schema {:layer    clojure.lang.Keyword
                                           :k        clojure.lang.Keyword
                                           :v        String     ; canonical EDN; nil for a retract (P13)
                                           :replaces clojure.lang.PersistentVector
                                           :mark     (set-schema clojure.lang.Keyword)
                                           :lock-id  clojure.lang.PersistentVector   ; stage 2's: [:value fid] | [:act name] | nil (M12)
                                           :lock     <lock-record>})                 ; stage 2's: the locks plan's (fixed-keys-schema {:required :any-of :blob :any-blobs}) when kept in the record; nil for a row lock, nil once excised
                       {:subindex-options {:track-size? false}})
                     {:subindex-options {:track-size? false}})
      :heads       (map-schema clojure.lang.PersistentVector    ; [layer k fid]
                     (fixed-keys-schema {:stamp          Long
                                         :batch          Long
                                         :replaced-by    clojure.lang.PersistentVector
                                         :replaced-batch Long})
                     {:subindex-options {:track-size? false}})
      :answers     (map-schema clojure.lang.PersistentVector    ; name -> the act's record, on every entity it touched
                     <answer-record>                            ; the stream plan's record schema plus :batch Long
                     {:subindex-options {:track-size? false}})
      :locks       (map-schema clojure.lang.PersistentVector    ; lock id -> the lock record: lock rows for values on this task (an own-row mark, a re-classed personal or hand layer); stage 2's shape (M12)
                     <lock-record>
                     {:subindex-options {:track-size? false}})
      :erased      (map-schema clojure.lang.PersistentVector    ; lock id -> {:stamp :how}: the erasure ledger for values on this task; stage 2 writes it (M12)
                     (fixed-keys-schema {:stamp Long :how clojure.lang.Keyword})
                     {:subindex-options {:track-size? false}})
      ;; a layer, as the entity its projections live under
      :settings    (map-schema Long                             ; batch -> the settings in force from that batch
                     (fixed-keys-schema {:kind    clojure.lang.Keyword   ; :group :base
                                         :owner   clojure.lang.Keyword   ; nil for shared layers
                                         :class   clojure.lang.Keyword   ; :by-entity
                                         :grain   clojure.lang.Keyword   ; :per-value :per-act
                                         :batch   Long})
                     {:subindex-options {:track-size? false}})
      :members     (set-schema clojure.lang.Keyword {:subindex-options {:track-size? false}})
      :permissions (map-schema clojure.lang.PersistentVector    ; pid [who layer in]
                     (fixed-keys-schema {:granted       clojure.lang.PersistentVector
                                         :granted-batch Long
                                         :revoked       clojure.lang.PersistentVector
                                         :revoked-batch Long})
                     {:subindex-options {:track-size? false}})
      :replaced    (map-schema clojure.lang.PersistentVector    ; [e k fid] of a stream-era head, replaced here
                     (fixed-keys-schema {:by clojure.lang.PersistentVector :batch Long})
                     {:subindex-options {:track-size? false}})})})
```

Why each part is shaped so:

- `:log` keyed by name, then by index: an act's rows on this entity are the
  subset of its facts with this entity, so a map of index to row, not a
  vector; a row is one seek at `(keypath e :log name idx)`, the act's rows on
  this entity are read whole by `ALL`. Unbounded both ways (acts of any
  size, entities of any history), so both levels are subindexed.
- `:heads` keyed by `[layer k fid]` under the entity: the stale-replace check
  for `{:e e :k k :replaces r}` in layer L is one seek at `[e :heads [L k r]]`
  and needs no scan; the value keeps the stamp, the batch and, once
  replaced, by whom and in which batch, so a reader at any frontier and a
  read as of a moment (stage 5) can tell the head as of then. Ids and stamps
  only, no value, so a forget never has to reach it (I-L5).
- `:answers` under each entity the act touched: OP3's invariant, and block 2b
  needs no hop to learn an act's verdict and stamp on the entity's task; it
  also gives stage 5 "which acts touched this entity, when, by whom" without
  a scan of names.
- `:settings` as versions keyed by batch: the gate takes the last one
  (`sorted-map-range-to-end 1`, one seek); a reader at F takes the last at or
  below F (`sorted-map-range-to (inc F) 1`, one seek); nothing is
  overwritten (M7). Rare writes, so the history stays tiny.
- `:members` as its own subindexed set, not a field of the settings map: a
  group can have any number of members, and the gate reads the settings on
  every offer, so the members must not ride in that value. The gate never
  reads it; stage 5's visibility does.
- `:permissions` keyed by pid under the layer id: the exists and revoked
  checks are one seek on the task the settings are on, so the gather's
  layer hop serves both; the row changes exactly twice (grant, revoke: no
  re-grant, P8), so two batch fields make it frontier-safe.
- `:replaced` under the layer id: the tombstones for a re-classed layer's
  stream-era heads, on the same task as the frozen `$$layers [L :heads]`
  they annotate, so the settled-history check is two local seeks (M5).
- `:locks` and `:erased`: the lock rows and the erasure ledger for the
  values on this task (an own-row mark, or a re-classed personal or hand
  layer, ruling 7; I-L2), in the locks plan's shapes (its `:locks` and
  `:erased` under the layer, here under the entity because that is the
  value's task), declared now so the merge needs no schema edit. The
  ledger is not batch-stamped: an erasure crosses the frontier (I-L7).
- Size tracking off everywhere: nothing counts; a count costs a read per
  write.
- No `Object`: keys are keywords or vectors the parser rebuilt with
  `(into [] ...)` [F6]; `:v` and `:lock` are text (P12).

### `$$micro-names` — everything keyed by a name, on the name's task

```clojure
(declare-pstate mb $$micro-names
  {clojure.lang.PersistentVector                               ; the name [layer class scheme uuid7]
   (fixed-keys-schema
     {:answer   <answer-record>                                 ; the name row (I-G4), with :batch
      :faces    (map-schema String                             ; digest -> a refusal that is not the name's answer (M8)
                  (fixed-keys-schema {:reason clojure.lang.Keyword :batch Long})
                  {:subindex-options {:track-size? false}})
      :stood-on (map-schema clojure.lang.PersistentVector Long ; fid -> stamp as carried (P9, [F2])
                  {:subindex-options {:track-size? false}})})})
```

The name row is the ruled read (I-G4, "a name row the microbatch commit
writes in the same batch when placed by entity"): a point read by name,
routed by the name, so a separate PState from `$$micro` (a different key and
partitioner). `:faces` is subindexed because a hostile client can reuse one
name without bound, as it can fill the depot. `:stood-on` is beside the
record for the reason of [F2]. The answer is read with `[(keypath name
:answer)]`, a plain map (foreign-client.md: a value with subindexed children
cannot be selected whole).

### `$$micro-task` — the task's clock and the frontier

```clojure
(declare-pstate mb $$micro-task (fixed-keys-schema {:clock Long :frontier Long}))
```

Two per-task longs with no key: the micro store's own stamp clock (I-G9's
"the partition's stamp", for this store's partitions; the stream `$$clock`
belongs to the other topology and cannot be written here) and the frontier
(M7). One top-level fixed-keys PState, not two class PStates, by the merge
rule; fields start absent, so reads coalesce nil to 0 and -1.

### `$$layers` — read, never written

The stream store's PState (P1: one module), read by this topology as
committed state (RQ 6's second half, a read at a moment, I-O4) for a
one-owner layer's settings (the re-class fact of record, P16), its
stream-era permission rows and heads (R 105). Every such read is on
hash(L), the task `$$micro [L]` is on too (P2).

### What is not a PState

No name-to-entities index (an act's entities are in the offer, which the
depot keeps, and in the name row's caller's hands; a test knows what it
sent). No TaskGlobal: the fold's working state lives for one batch on the
leader and is rebuilt from the gather every batch; nothing is cached.

## Depots

> **Revision:** `*micro-offers` routes a sealed act by its `:session` (M4
> revised, §A); an act with no sealed value routes as below.

- **`*micro-offers`** — `(declare-depot setup *micro-offers (hash-by
  micro/route-key))`, client appends (people, the operator, the seed) and,
  in stage 4, the stream gate's `depot-partition-append!` of a landing
  (a client depot accepts topology appends; `:disallow` only rejects
  clients). `route-key` is a top-level `defn` (depot-design.md: a keyword or
  a var), total: the entity of the act's first fact, nil for anything else
  (nil hashes to some task, where block 1 refuses the record as malformed;
  the partitioner never throws, RQ 3's client side). By the first fact's
  entity because the common act has one entity (an agent's thread, a
  group's note), and then its arriving task is its entity task: block 1's
  entity hop and block 2b's row writes are local (M4). One depot, not one
  per operation: every operation is an offer, and the order among the
  offers of one layer is the batch order, which a second depot would not
  change (both would feed the same batch). Retry mode is not a depot option
  for a microbatch source; the topology retries the batch whole.
- **`*micro-tick`** — `(declare-tick-depot setup *micro-tick 250)`, so a
  batch runs at least every 250 ms and the frontier advances after the last
  data batch (M7; microbatch.md "Tick depots for timely background work").
  In tests it is replaced by a global depot the test appends to
  (testing.md "Testing Tick Depots"), or left live with the tests polling
  the frontier instead of counting records.
- No internal depot in this stage. The landing handoff is described by the
  stream plan ("The landing handoff"); its target is `*micro-offers`.
- Placement is computed, never stored: `hash(e) mod N` by the depot's
  partitioner on the client and by `$$micro`'s default key partitioner on
  the server, `hash(name) mod N` for `$$micro-names` (P2's argument; a
  stored placement cannot serve a depot partitioner and would cost a read
  per offer for the same function).

## Topologies and PStates

Two topologies are added, both in `rig.store.micro`: the microbatch gate
`"micro"` (owns `$$micro`, `$$micro-names`, `$$micro-task`) and the query
topology `micro-lookup` with `micro-act` (read-only). The module then has
one stream topology (stage 1's `gate`) and one microbatch topology, which
is P1's shape and the topology count rule's.

### `micro` — microbatch

**Why microbatch** (ruling 1 names the type; artifact-plan's default): the
ruled properties are exactly a microbatch's: an act admitted whole across
tasks (I-G6, RQ 5's cross-partition atomicity), a batch decided before it is
visible (RQ 5), exactly-once writes on retry with no idempotency argument
per write, and one order for a batch's decisions. No concern in it needs
stream: nothing returns a value through an ack, and OP3's latency is
"hundreds of milliseconds to a few seconds". No second microbatch topology:
every concern here is the one decision and its commit, one latency class.

**The shape: prepare on one task, commit on every task.** The model's micro
gate is a leader that decides a queued batch in order, each offer seeing
the ones before it, then commits in one step (`micro-prepare`,
`micro-commit`). The rig keeps that shape because nothing weaker is correct:
an act's facts sit on several tasks and its verdict is one; a later offer's
stale-replace, revoked-permission or taken-name check depends on that
verdict; so the batch is a sequential fold over a total order, and a fold
over state spread across tasks runs where the state is brought to it. The
alternatives, constructed and costed, are in the difficulty log (a per-task
fold is unsound for multi-task acts; replicating the fold to every task
with `|all` multiplies the leader's work by N). What the leader receives is
skeletons and index rows, never values (a few hundred bytes per offer);
reads and writes stay on the entity and name tasks. This is patterns.md's
"two-phase aggregation": a first pass gathers, a repartition to one task
decides, a second pass writes.

> **Revision:** block 1 begins with §A's lease work on the arrival task (and,
> for a resend, §A's content check); the `$$persons` subject read in block 1
> below is replaced by §A's subjects from the opened value; block 2b is §A's;
> a block 2c after it consumes the leases (§A). The fold's permission inputs
> are the chain's rows (§B).

**The batch, block by block.** Every block is a `<<batch` (a global
barrier, microbatch.md), so each sees the previous one's writes on every
task (reads inside the owning topology see its uncommitted writes).

0. **Frontier.** `(ops/current-microbatch-id :> *b)`, `(|all)`,
   `(local-transform> [(keypath :frontier) (termval (dec *b))]
   $$micro-task)`. At the start of batch b every task has committed b−1
   (RQ 7), so b−1 is a frontier every task can vouch for; the write becomes
   visible on a task when b commits there, and stays true (M7). Runs on
   every batch, data or tick.

1. **Gather, then decide on the leader.** Pre-agg, per record of `%mb` (the
   depot's records on their arriving tasks, `(%mb :> *raw)`; the tick
   fragment is emitted once at the top of `<<sources` with no binding, to
   force the batch):
   - `(envelope/parse *raw :> *p)` (total, [F6]) with the micro side's
     gate check (see "What this stage takes"); `(micro/skeleton *p :>
     *s)` (pure: the digest, the skeleton, the face reason if any).
   - A face refusal (malformed with a readable name, `:wrong-gate`,
     mis-tagged, `:crossing` scheme, `:reserved-who`, empty act, over the
     subject cap) emits one row `[[:face name digest] reason]` and no
     other; a record with no readable name emits nothing (nobody can ask
     for it).
   - `(|hash *name)`; `(local-select> [(keypath *name :answer)]
     $$micro-names :> *row)`; if a record exists (any digest) emit
     `[[:name name] row]` and stop: the fold will answer "recorded" or
     "taken" from it and needs nothing else (one seek for a resend or a
     replay, category (e) below).
   - Else `(|hash *layer)`: the settings, micro first
     (`sorted-map-range-to-end 1`), `$$layers [L :settings]` on a miss;
     the permission row(s) (skipped for the operator): `$$micro [L
     :permissions pid]`, and for a one-owner layer `$$layers [L
     :permissions pid]` too; for each replacing fact of a one-owner layer
     `$$layers [L :heads [e k r]]` and, when present, `$$micro [L
     :replaced [e k r]]`. Emit the layer rows `[[:settings L] m]`,
     `[[:perm L pid] rows]`, `[[:shead L e k r] v]`, `[[:tomb L e k r] v]`.
   - Before any hop, on the arriving task, for each subject of the act
     `[(keypath p)]` on `$$persons` (stage 2's person locks, on every task
     by `|all`, read as committed state): rows `[[:person p] entry]`, so
     `gate/decide`'s stage-2 reasons (a subject with no lock or a destroyed
     one) have their input; one local seek per subject.
   - `(ops/explode (:entities *s) :> *e)`; `(|hash *e)`;
     `(ops/current-task-id :> *t)`; `(local-select> [(keypath :clock)]
     $$micro-task :> *clock)`; for each replacing fact on this entity not
     found stream-side, `$$micro [e :heads [L k r]]`. Emit `[[:task-of e]
     t]`, `[[:clock t] clock]`, `[[:head L e k r] v]`, and once the offer
     row `[[:offer name] skeleton]`.
   - Every branch ends in the same row shape `[*need *found]` (the branches
     unify with `unify>`, or the layer rows are carried to the entity hop
     and exploded there; either is allowed in pre-agg, batch.md), then
     `(|global)` and `(aggs/+map-agg *need *found :> *state)`: a combiner,
     so each task pre-merges its rows and ships one partial map (two-phase
     aggregation, aggregators.md). Duplicate needs collapse to one entry.
   - Post-agg, on task 0: `(micro/wall :> *wall)`;
     `(ops/current-microbatch-id :> *b)`; `(micro/prepare *state *wall *b
     :> *out)`, the pure fold described next; `(ops/explode (:writes *out)
     :> *w)`; `(materialize> *w :> $$writes)`.

2a. **Commit the index writes.** `($$writes :> *w)` on task 0, each `*w` a
   `[kind route path value]`; `(<<cond (case> (= kind :entity)) (|hash
   route) ... (case> (= kind :name)) (|hash route) ... (case> (= kind
   :task)) (|direct route) ...)` then the `local-transform>` with
   `(termval value)` at the path. The writes are those listed under
   "Writes", block 2a. Pre-agg only, no aggregator.

2b. **Commit the rows.** `(%mb :> *raw)` again on the arriving tasks (a
   second pass over the same microbatch data, microbatch.md); parse; skip a
   face refusal; `(|hash *name)`; `(local-select> [(keypath *name :answer)]
   $$micro-names :> *rec)`; `(filter> (and> (= (:batch *rec) *b) (=
   (:digest *rec) *digest) (= :yes (:answer *rec))))` — only an act decided
   yes *in this batch for this content* writes rows (a resend of a decided
   name is not re-written; two contents under one name in one batch write
   only the first's rows); write `[(keypath *name :stood-on fid) (termval
   stamp)]` per carried entry; `(ops/explode (indexed facts) :> [*i *f])`;
   `(|hash *e)` (only `*f`, `*i`, `*name`, `*rec`'s few fields ship,
   dataflow.md liveness); `(micro/row-of *f *rec *settings :> *row)` with
   `:lock-id` and `:lock` from `micro/row-lock` (nil, nil in this stage;
   M12); `(local-transform> [(keypath *e :log *name *i) (termval *row)]
   $$micro)`; and, when `row-lock` says the lock is a row, `(local-transform>
   [(keypath *e :locks *lid) (termval *lock)] $$micro)` (nothing in this
   stage).

**The fold, `micro/prepare`** (pure, total, the executable counterpart of
`micro-prepare` + `micro-decision` + `refusal` + `stamp-for` under
`baseline`): from `*state` it takes the offers, sorts them by the batch
order (M2), and folds over them with a working state W initialised from the
gathered rows. Per offer:

1. A face row: copied to the output as a `:faces` write; nothing else.
2. A name row in W (gathered, or written earlier in this fold) with the same
   digest: "recorded", nothing written (E1 N2/N3 × resend; a resend in the
   same batch as its original, OP3 "the second is skipped as decided"). With
   another digest: a `:faces` write with `:name-taken`, nothing else (the
   first use keeps the name, E1 N1 × other content).
3. Otherwise the decision, by handing `gate/decide` (stage 1's pure
   decision) exactly the inputs the stream gate gives it, built from W: the
   settings in force (`$$layers`'s for a one-owner layer, else the latest
   micro version, else the act's own class fact, P10); the permission row
   merged from both stores (live iff granted in either and revoked in
   neither, M5); the heads map `r → stamp-or-nil` (micro head unreplaced,
   or stream-era head present and untombstoned, with W's in-batch updates
   applied); the clock = the maximum over the act's touched tasks of W's
   clocks; the leader's wall. So the reason order is the stream plan's
   (I-G5 with `:no-such-layer`, `:stale-revoke`, `:layer-already-made`,
   `:unsupported-reclass`, `:malformed-control`, `:control-not-allowed`
   placed as P7, [F8] and R13 say), the same code, and the stamp is the
   model's micro stamp (touched tasks' clocks + 1, stood-on + 1, replaced +
   1, the wall). The only micro-side additions are in the inputs: a `:class`
   fact outside a making act is `:unsupported-reclass` (O9), a person's
   control fact on a shared layer is `:control-not-allowed` (no owner,
   R13, D11).
4. Apply to W: the name row; the answer copies; for a yes, the heads
   changes, the permission and settings projections, the clocks of the
   touched tasks (each set to the stamp, so the next act on that task in
   this batch stamps higher). Emit the writes of "Writes", block 2a.

The output is `{:writes [...]}`; the rows (block 2b) are derived from the
offers, not from the fold, because the fold never sees values.

**Deterministic across retries.** A retry of microbatch b reruns block 0
to 2b on the same records against the same committed state (the failed
attempt's writes were reset, microbatch.md "prime"); the order is a
function of the records; only the wall can differ, so a stamp can differ
between attempts, and only the committed attempt's stamp exists (no stamp
given twice, I-G2). `ops/current-microbatch-id` is the same across retries,
so `:batch` is too.

**Idempotency of every write, traced.** Every write is a `termval` of a
value computed from the batch and the pre-batch state: records, faces,
heads (the replaced value is rewritten whole with fields known from the
gather), tombstones, settings versions keyed by batch, permission rows,
members, clocks (set to a max computed in the fold), the frontier, rows,
stood-on entries. Within one attempt no location is written twice with
different values: the fold gives each name one record, each `[L k fid]`
one head value, each task one clock. Across attempts, exactly-once is
Rama's. No `AFTER-ELEM`, no `term inc`.

**Same answer after a crash mid-batch.** A worker crash during any block
discards the attempt on every task (RQ 5 ran and held); the retry decides
the same batch in the same order on unchanged state, so the same answers
and admitted facts, possibly different stamps, none of which were ever
visible. The test injects one throw (R3, `inject/maybe-fail!` armed by name,
one-shot so the batch does not retry for ever, microbatch.md "A record that
throws deterministically retries forever") in block 2b on one task and
asserts one record, one stamp, rows once, the frontier moving past the
batch, and at least one retry (R4).

**No input can make topology code throw** (I-G1, RQ 3), which in a
microbatch is worse than a restart: a deterministic throw retries the batch
for ever and stalls the store. (1) `%mb` binds the raw record; (2)
`envelope/parse` is total ([F6]); (3) `micro/skeleton`, `micro/prepare`
and `micro/row-of` are pure and total, with a `Throwable` guard per offer
inside the fold that turns a failure into an unrecorded `:gate-error`
face (the stream plan's [F6]), never a guard around the whole fold that
could drop a batch; (4) every hop key is a keyword or a parser-rebuilt
vector; `|direct` gets a task id the gather produced; (5) every written
value has the schema's class because `parse` normalised it and the fold
builds records from typed parts; (6) `+map-agg` receives plain data; (7)
`(dec *b)` is on a Long (a build check, "could not settle"); (8) zero
emits are fine everywhere: a face record emits one row, a decided name one
row, and the aggregator fires on zero rows (batch.md), so an empty or
all-refused batch still runs blocks 0 to 2b and advances the frontier.

**Cooperative multitasking.** The gather and the writers iterate the
offer's own facts only. The fold on the leader is O(offers in the batch)
map operations, bounded by `depot.microbatch.max.records` × N; no
`:allow-yield?` read exists (no read iterates a PState). The one long
synchronous stretch is the fold itself; at 1,000 records per partition and
N = 16 it is about 16,000 iterations of map lookups, tens of milliseconds.
If phase 7 finds it long, `depot.microbatch.max.records` bounds it.

**PStates owned:** `$$micro`, `$$micro-names`, `$$micro-task`.

## Query Topologies

> **Revision [PV-F9]:** §D and §A supersede this section's inputs and read
> order. `micro-lookup [*name *digest *fp *f :> *result]` reads F (or takes
> the caller's), then the faces entry under `[name fp]`, then the record;
> input examples: a face-refused attempt, 2 reads (F, the face), both
> meaningful; a decided name, 3 (F, no face, the record); a name never
> sent, 3, all three the question asked. `micro-act [*e *name *f]` as
> below with F explicit. `micro-lease [*lease-name *f]` and
> `micro-leases-of [*s *f]` are §A's. Four query topologies in all.

Two, both read-only, both one partition, both needed because the frontier
makes every reader-facing read two reads on one task (phase-1-plan Step 1:
more than one PState read → a query topology, one roundtrip).

- **`micro-lookup [*name *digest :> *result]`** (RD1, micro side; `lookup`
  in the model with `:digest-check`): `(|hash *name)` as the leading
  partitioner (client-side routing, query-topologies.md); read F, the name
  row, and, only when the row is absent or above F, the faces entry for the
  digest; `(|origin)`; the result as the "Reads" table says.
  - Input 1: a decided name, digest matches → 2 reads, 2 meaningful.
  - Input 2: a name never sent → 2 reads (F, the row) + 1 (faces) = 3, of
    which the row and the faces are empty: not meaningful, but they are the
    question being asked ("is there an answer, is there a face refusal");
    a read that answers "no" is the read the offerer needs.
  - Input 3: a face-refused name → 3 reads, 3 meaningful.
  - Variable: the faces read is conditional on the row (`<<if`), so no
    input pays a read it cannot use. No explode, no aggregation; the output
    is emitted once on every path (the branches bind `*result` alike).
- **`micro-act [*e *name :> *result]`**: `(|hash *e)`; F; the answer copy
  under the entity; if `:batch ≤ F`, the rows by `[(keypath *e :log *name)
  ALL]` collected with `subselect` into one vector; `(|origin)`. Fixed: 2
  reads, 3 when the act is visible. Rows are ordered by index (a subindexed
  map sorts by key).

Both apply I-P2's visibility only as "stage 5's": in this stage the caller
is trusted (O22 left to stage 5).

## Partitioning efficiency

> **Revision:** recomputed in §H. The tables below are the 25 September
> costs, before the lease read and the permission chain.

**Optimal placement, derived first.** Two reads dominate: the gate's gather
per offer and the offerer's lookup by name. The gather wants, for an act,
its facts' chain heads and rows on the facts' entity tasks (so an entity's
chains are one task's point reads and a single-entity act, the common one,
touches one task), its layer's settings and permission index on one task
per layer (so one hop serves the class and the permission check), and its
name row on one task found from the name alone (so a lookup is one hop with
nothing else known). So `f(entity) → hash(entity) mod N`, `f(layer) →
hash(layer) mod N` for the layer's projections (the same function, since a
layer id is a keyword like an entity id, and it is the task `$$layers [L]`
is on, P2), `f(name) → hash(name) mod N`. Ruling 2 fixes the first ("by
entity for shared ones") and fixes N. Load balances across entities and
names as a hash does when there are many per task (entities and names grow
without bound); a hot entity (one thread written by an agent at full rate)
is a hotspot on one task by ruling, the by-entity counterpart of the hot
layer, and the fold on task 0 is the one deliberate funnel, costed above.
`hash-by` on the depot and the PStates' default key partitioners implement
all three `f`s; a stored placement was rejected on P2's total-cost argument.

**Validation.** Seeks/op are totals across the cluster. Every read below is
a point read on one task; the gather touches 1 + (distinct layers, 1) +
(distinct entities, usually 1) tasks, each with a fixed number of local
seeks, so the totals do not change with N. The one per-batch cost that does
scale with N, block 0's frontier write (N no-read sets per batch), is a
write, is per batch not per offer, and is amortised over the batch's
records (and over a 250 ms tick when there are none); it is stated, not
hidden.

Data categories for the gate's decision (one offer), block 1's reads:

- (a) an ordinary act, one entity, a shared layer, a person, no replace:
  name row 1 + settings 1 + permission 1 + clock 1 = 4.
- (b) as (a) with r replacing facts (r = 1): 4 + 1 (micro head) = 5.
- (c) an act on m entities (m = 2 counted): 4 + (m − 1) clocks = 5, plus r.
- (d) an act into a re-classed one-owner layer: name 1 + settings (micro
  miss 1, `$$layers` 1) + permission (`$$layers` 1, micro 1) + clock 1 = 6;
  with a replace of a stream-era head + 2 (head, tombstone) = 8; counted 7.
- (e) a resend or a replay of a decided name: name row 1, then nothing.
- (f) a face refusal: 0.
- (g) an operator act (make, grant, revoke): name 1 + settings 1 + clock 1
  (+ the permission row for a revoke) = 3 to 4, counted 4.

Frequencies: group and base writes and re-classed agent streams dominate;
replaces are edits; multi-entity acts are rare; resends per client error;
operator acts rare. Once stage 2's `$$persons` exists, every category but
(e) and (f) adds one local seek per subject the act names (on the arriving
task, before any hop); a plain note names none, a mention one or two; not
counted below because it is stage 2's read, and flat in N either way.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| (a) ordinary act, one entity, no replace | 0.50 | 4 | 0 |
| (b) act with a replace | 0.18 | 5 | 0 |
| (c) act on two entities | 0.05 | 5 | 0 |
| (d) act into a re-classed layer | 0.12 | 7 | 0 |
| (e) resend / replay, decided | 0.10 | 1 | 0 |
| (f) face refusal | 0.02 | 0 | 0 |
| (g) operator act | 0.03 | 4 | 0 |
Weighted seeks = 4.31   |   Weighted iterator reads = 0

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| (a) ordinary act, one entity, no replace | 0.50 | 4 | 0 |
| (b) act with a replace | 0.18 | 5 | 0 |
| (c) act on two entities | 0.05 | 5 | 0 |
| (d) act into a re-classed layer | 0.12 | 7 | 0 |
| (e) resend / replay, decided | 0.10 | 1 | 0 |
| (f) face refusal | 0.02 | 0 | 0 |
| (g) operator act | 0.03 | 4 | 0 |
Weighted seeks = 4.31   |   Weighted iterator reads = 0

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| (a) ordinary act, one entity, no replace | 0.50 | 4 | 0 |
| (b) act with a replace | 0.18 | 5 | 0 |
| (c) act on two entities | 0.05 | 5 | 0 |
| (d) act into a re-classed layer | 0.12 | 7 | 0 |
| (e) resend / replay, decided | 0.10 | 1 | 0 |
| (f) face refusal | 0.02 | 0 | 0 |
| (g) operator act | 0.03 | 4 | 0 |
Weighted seeks = 4.31   |   Weighted iterator reads = 0

Flat in N. The offerer's lookup is 2 seeks (3 with no answer) at every N;
`micro-act` 2 seeks plus the act's rows on that entity. Writes per admitted
act, all no-read sets: 1 name row + m answer copies + f rows + f heads +
r replaced values or tombstones + b stood-on entries + s settings versions +
p permission rows + |touched tasks| clocks, plus one name-row read in block
2b; per batch N frontier sets. Network per offer: the skeleton to the
layer task, to each entity task and (index rows only) to the leader; the
decision back out (one message per write); the offer once to the name task
and each fact once to its entity task.

Where the design is known not to be optimal and accepts it: the leader's
fold and `+map-agg` on task 0 are one task's CPU and memory per batch,
bounded by the batch cap; a per-task fold would be cheaper and is unsound
(difficulty log). The frontier costs a batch of latency (below).

## Design Decisions

- **Subindexing.** In `$$micro`, every child map and set is subindexed
  (`:log` at both levels, `:heads`, `:answers`, `:locks`, `:settings`,
  `:members`, `:permissions`, `:replaced`); each grows with entities, acts
  or members without an enforced bound. In `$$micro-names`, `:faces` and
  `:stood-on` are subindexed for the same reason; the answer record is a
  plain map (its `:subjects` capped by the parser at 256, [F3]). Size
  tracking off everywhere. `$$micro-task` has two longs.
- **Colocation.** `hash-by route-key` puts the common single-entity act on
  its entity task, so block 1's entity hop and block 2b's row writes are
  local; the layer's projections sit where `$$layers [L]` sits, so one hop
  reads both stores; the name row sits where the name hashes, so a lookup
  needs nothing but the name.
- **Prepare on one task, commit on every task.** The model's shape; the
  only sound one for acts that span tasks; skeletons and index rows travel,
  values do not.
- **Batch order by the name's time.** Deterministic without state; the
  same on every retry; the closest stateless approximation to arrival.
- **Nothing overwritten, everything batch-stamped.** The frontier is a
  filter on batch ids, so every mutable projection keeps its history: heads
  keep their replaced-by, permissions their revoked-batch, settings their
  versions. One rule, applied uniformly, is what makes "no reader sees
  half a batch" a property a test can check rather than a hope.
- **The frontier is written, not computed.** A reader takes one long from
  the task it is on (safe on every task by construction) instead of the
  minimum over N tasks (N seeks per read, growing with the cluster). The
  price is one batch of lag and a tick depot to keep batches running.
- **Refusals as data, three classes.** Face refusals and taken names are
  `:faces` entries under `[name digest]` (M8): data the offerer can read,
  never the name's answer, deterministic on resend. Recorded refusals are
  the name's answer with a stamp, as the model. The stream gate's ack
  channel does not exist here, so the second class is what carries O4.
- **The stream store is read as settled history, never written.** A
  re-classed layer's class, stream-era grants and revokes, and stream-era
  heads stay in `$$layers`; this store keeps the delta (its own permission
  rows, tombstones for replaced stream-era heads). The two are merged in
  the fold. The stream gate refuses that layer's `:by-layer` names from the
  re-class on, so the stream side is frozen for the facts this gate merges
  in, and a stale read only refuses (R 103); the grain switch is the named
  exception (P16, D6).
- **`gate/decide` is reused, not rewritten.** The verdict, the reason
  order and the stamp are one function shared with the stream gate; the
  micro fold prepares its inputs and re-keys its outputs. "The rig's gates
  must decide the same way" is then one implementation, not two kept in
  step.
- **Control facts at this gate**: making a shared layer, grants, revokes,
  a shared layer's grain switch and forget are the operator's (R13); a
  `:class` fact outside a making act is refused (O9); the re-class of a
  one-owner layer is the stream gate's (P16).

## State primitive selection

- `$$micro` (PState): durable, partitioned by entity. Per admitted act the
  write volume is f rows + f heads + r replaced values or tombstones + m
  answer copies + s settings versions + members + p permission rows, all
  bounded by the act the offerer sent. Source of truth for this store's
  facts (the rows with the records); the heads, settings, permissions,
  replaced and members maps are projections rebuildable from the rows and
  the records.
- `$$micro-names` (PState): durable, partitioned by name; 1 record per
  decided name, 1 faces entry per face refusal, b stood-on entries per
  admitted act.
- `$$micro-task` (PState): durable, two longs per task; 1 clock set per
  touched task per batch, 1 frontier set per task per batch.
- The fold's working state W: transient, one batch, on the leader, built
  from the gather and discarded; not a TaskGlobal, not a cache (CLAUDE.md's
  rule: nothing here is cached; a retry rebuilds it from committed state).
- No TaskGlobal, no external system. The fingerprint secret is P6's
  constant.

## Resource usage analysis

Sizes as the stream plan's: ids about 10 bytes, a name about 60, a fact id
about 70, a pid about 40, a stamp or batch id 8.

### Disk usage (PStates and the depot), per task

- `$$micro :log`: one row per fact, about 90 bytes of framing and ids plus
  the value's EDN text (the toy's about 40) plus 70 for a `:replaces`, plus
  the lock slot (stage 2's bytes); the key is the 60-byte name plus an index.
  Growth: one per admitted fact.
- `$$micro :heads`: about 190 bytes per fact (key 100, value 90 with the
  replaced fields); never deleted (M7). Growth: one per admitted fact.
- `$$micro :answers`: the record, about 400 bytes with its key, once per
  (act, entity). Growth: one per admitted or refused act per entity touched.
- `$$micro :settings`, `:permissions`, `:members`, `:replaced` under a layer
  id: about 100 bytes per version, 250 per permission, 20 per member, 180
  per tombstone; rare writes except tombstones (one per replaced
  stream-era head of a re-classed layer, bounded by that layer's stream-era
  heads).
- `$$micro-names`: the record about 400 bytes per name; faces 90 per face
  refusal; stood-on 80 per carried entry. Growth: one record per decided
  name, never expired (D4).
- `$$micro-task`: 16 bytes.
- `*micro-offers`: the raw offer, 500 bytes to 1 KB, retained (no
  trimming; O2 is stage 2's). `*micro-tick`: nothing kept.

At 100,000 single-fact acts on one task: rows about 20 MB, heads 19 MB,
answer copies 40 MB, name rows 40 MB (spread by name over all tasks),
depot 80 MB. With N tasks each holds its entities' and names' share.

### Memory usage (TaskGlobals)

None. The leader's per-batch state (`+map-agg`'s map and the fold's W) is
transient: about 250 bytes per gathered row; at 1,000 records per partition
and N = 16, about 16,000 offers × 5 rows × 250 bytes ≈ 20 MB for the batch,
then garbage. Bounded by `depot.microbatch.max.records`; lower it if phase
7 sees GC pauses on task 0.

### Minimization

- The answer record is stored 1 + m times per act (name row plus one copy
  per entity touched). The copies are what OP3 states and what block 2b
  reads locally; dropping them would route every row write through the
  name task instead (one hop more per fact) and cost stage 5 a scan for
  "acts on this entity". Kept, with the reason.
- Heads keep replaced entries for ever: the price of the frontier and of
  chain history (stage 5); an ids-and-stamps row, no value.
- `:v` and `:lock` as text, as P12; a kept store would store bytes.
- Faces grow with hostile reuse; so does the depot, which keeps the same
  bytes; a kept store would trim both by policy.
- Nothing is duplicated across storage beyond the projections named under
  "Design Decisions", each needed by every decision or by the frontier.

## The settled frontier (R5), stated whole

> **Revision:** the frontier id's form, the explicit-F reader's rule, the
> query and what a read entry records are §D.

- **What it is.** `$$micro-task :frontier` on every task: the id of the
  last batch every task has committed, written by block 0 of each batch as
  the previous batch's id, visible on a task once that batch commits there.
  Every micro row carries the id of the batch that wrote it; a change to a
  projection is a new batch-stamped field or entry, never an overwrite.
- **The reader's rule.** Read F from the task you read on; then hide any
  record, row, face, stood-on entry, head, tombstone, settings version,
  grant or revocation whose batch id is above F. What remains is exactly
  the store as of the end of batch F on every task, because every task had
  committed F before F + 1 began (RQ 7) and F + 1's writes are the ones
  hidden. An offerer reading its own answer sees `:yes` only when F is at
  or past its batch, and at that F the act's rows on every entity are
  committed and visible: an answer and a read by the same offerer never
  contradict (OP2's derived invariant).
- **Why written, not computed.** A frontier computed as the minimum of N
  per-task "last committed" values is N seeks per read and grows with the
  cluster; a written one is one seek and is a safe lower bound on every
  task. The written one lags: a batch's writes become readable when the
  *next* batch commits on the reader's task. The tick depot bounds that
  lag to about one tick plus one batch (250 ms tick, a batch of a few
  hundred milliseconds on the in-process cluster), so an answer is
  readable within roughly two batch cycles of its commit.
- **What it costs in latency.** For an offerer: append → its batch decides
  and commits (one microbatch cycle, hundreds of milliseconds) → the next
  batch's block 0 commits (up to one tick plus one cycle) → the lookup
  (one roundtrip). About two cycles, the second one the frontier's own
  price, against a raw read's one. For a reader of a value: the same lag
  behind the newest commit. For the gate: nothing (it reads committed
  state directly).
- **What it costs in space and writes.** N sets per batch; the history the
  no-overwrite rule keeps (replaced heads, revoked rows, settings versions),
  costed above.
- **What it does not promise.** A read at F is final only for what the
  micro store held at F; the stream store can still admit something dated
  at or before a moment (P 173-177, R 97); a read across both stores has
  one moment per store (O23, stage 5's).
- **Erasures cross it** (I-L7): a forget's effect (stage 2, the lock) is not
  hidden by F, because "gone for everyone including the past" is ruled;
  the forget *fact* is hidden like any row until its batch is at or below F.

## Rig choices proposed

> **Revision:** M4, M8, M11 and M12 are revised, and M16 to M25 added, in
> §I; M1's open question is the probe's (§G). The rest stand.

Each is a pick where the rulings are silent, one line of what and one of
why. None changes PROGRESS.md. The build session copies the ones it keeps
into RIG.md with the next free numbers.

- **M1. The micro store lives in `rig.store.micro` and is declared into
  `Store` by one call, `(micro/declare! setup topologies)`.** Why: R15
  builds this stage beside stage 2, so its depots, PStates, topologies and
  query topologies must be declared from a file stage 2 never touches; the
  one call is the whole edit to `module.clj` (plus its `:require`).
- **M2. A batch's offers are decided in ascending order of the name's UUID7,
  then the full name.** Why: the fold needs a total order that is a
  function of the records alone (the same on every retry) and needs no
  cross-task state; the UUID7 was made at the offerer's clock just before
  the send, so it is arrival order for one client and a stated, reported
  divergence from the model's inbox order for two clients with skewed
  clocks.
- **M3. The batch is prepared on task 0 from skeletons and index rows, and
  committed by fan-out; the wall clock is read once per batch on the
  leader.** Why: an act spanning tasks has one verdict that later offers'
  checks depend on, so the batch is a sequential fold that must run where
  all the state it needs is brought; values never travel to the leader,
  so its cost is a few hundred bytes and a few map lookups per offer.
- **M4. One client depot, `*micro-offers`, partitioned by the first fact's
  entity; the `:landing` scheme is accepted as data from any appender; the
  `:crossing` scheme and `:who :store` are refused on the face.** Why: the
  common act has one entity, and that placement makes its gather and its
  row writes local; the landing's authenticity cannot be told from a client
  append without signing, which is ruled out at launch (P 63-64), so it is
  stage 4's to settle (an internal depot or a store mark), and this gate
  must not refuse what stage 4 will send.
- **M5. A shared layer's settings and permission index live under the layer
  id as an entity (`$$micro [L]`); a re-classed one-owner layer's class,
  stream-era grants, revokes and heads are read from `$$layers [L]` on the
  same task and never written there; the micro store keeps the delta (its
  own permission rows under `[L :permissions]`, tombstones under
  `[L :replaced]` for stream-era heads it replaces), and the fold merges
  the two.** Why: a PState has one writing topology, so the stream store's
  history cannot be edited from here, and copying a hot layer's whole heads
  index at re-class would be an unbounded scan inside a batch; the merge
  is two local seeks on one task because both stores hash the layer id the
  same way.
- **M6. R6: the offer carries the stamps of what it stood on, as P9; the
  micro gate reads nothing in the other store to stamp.** Why: the ruled
  order between the stores is stood-on, a read stamped in the checking
  store, which the offerer's read is; a hop per stood-on fact would put
  the other store's latency inside every batch; and the one store-made
  offer that stands on the other store, the landing, has its stamps
  written by the stream gate itself, so the carried value is the store's
  own read.
- **M7. Every micro row carries the id of the batch that wrote it and is
  never overwritten; the frontier is `$$micro-task :frontier`, written on
  every task at the start of each batch as the previous batch's id; a tick
  depot at 250 ms keeps batches running; readers hide what is above their
  task's frontier.** Why: RQ 5 showed a batch visible on one task before
  another, so a reader needs a bound every task can vouch for; a written
  bound costs one seek and is safe by RQ 7, where a computed minimum costs
  N seeks; no-overwrite is what makes the bound a snapshot rather than a
  guess; the tick is what makes the last batch ever become visible.
- **M8. A face refusal or a taken name at this gate is recorded under
  `[name digest]` in the name entry's `:faces` map, with its reason and
  batch, never as the name's answer; `micro-lookup` returns it when no
  answer exists.** Why: E1's rows require the refusal to reach the offerer
  as data and forbid it being the name's answer (O4); a microbatch depot
  has no ack channel, so a readable record is the only road; keying by the
  digest keeps it deterministic on resend and unable to shadow a first use.
- **M9. In the micro store a value's address is `[e name idx]`; the fact
  id stays `[name idx]`; a forget in a shared layer names the value's
  entity in its own `:e`, so it lands on the value's task.** Why: placement
  by entity means the id alone does not say the task, and every holder of
  an id (a read's row, a forget's author, a promotion's request) has the
  entity beside it; the model's forget carries its place the same way
  (`:at`).
- **M10. The answer record is written on the name's task and under every
  entity the act touches, in the deciding batch; rows are written only for
  a record of the current batch whose digest is the offer's.** Why: OP3's
  invariant says both; the batch-and-digest check is what keeps a resend
  in a later batch, or a second content under a name in the same batch,
  from re-writing rows, without any read of the other tasks.
- **M11. The shared layers are seeded as the model's `layers` and
  `permissions` on the shared side, through this gate as operator acts:
  `:group` (kind group, members Alice and Bob) and `:base`, each with its
  class and grain facts, and own permissions for Alice and Bob in each;
  Alice's session permissions for them stay in `:alice-hand` (stage 1's
  seed).** Why: the tests compare with the model's answers; ruling 9's group
  root "made with the layer naming the group" has no shape in the model
  yet (O6), so the per-person own permissions stand in for it until it is
  settled.
- **M12. The lock is an interface slot in the locks plan's shapes: each
  log row carries `:lock-id` and `:lock` (its lock record when kept in the
  record), each entity a `:locks` map (lock rows) and an `:erased` ledger,
  all nil or empty in this stage; block 2b fills the row's two fields and
  the lock row through `micro/row-lock`, a function returning `{:lock-id
  nil :lock nil :row? false}` that stage 2 re-points at its own in the
  merge; the gather reads `$$persons` locally for the act's subjects so
  stage 2's refusals have their input.** Why: ruling 7 puts the wrapped
  lock in the record for shared layers and a row on the value's task for
  marked values, the locks plan fixes the record's and the row's shape and
  puts `$$persons` on every task, so this stage declares those shapes
  where stage 2 needs them (the value's task) and touches none of stage
  2's files; the seam is one function.
- **M13. The client routes a name by its tag: `:by-layer` to `*offers`,
  `:by-entity` to `*micro-offers`, nil by the kind in the world map it
  seeded; the micro side's offerer functions (`offer-micro!`,
  `lookup-micro`, `act-rows`, `seed-shared!`) live in `rig.store.micro`
  too, and `client.clj` gains the dispatch in the merge.** Why: P16 left
  how the client learns a layer's kind to this stage, the rig's client is
  the test that seeded the world, and keeping the micro functions in this
  namespace keeps the parallel builds apart.
- **M14. At this gate only the operator writes control facts (making,
  grants, revokes, a shared layer's grain switch, forgets); a `:class` fact
  outside a making act is refused `:unsupported-reclass`; a person's
  control fact is `:control-not-allowed`; a group's rule that requires the
  mark on write (O17) is not implemented, and stays open.** Why: R13 and
  D11 (no owner on a shared layer); O9 says a shared layer's re-class is
  not ruled; O17 has no shape in the model and the D cases do not need it.
- **M15. Tests synchronise on the frontier, not on
  `wait-for-microbatch-processed-count`: an offer is settled when
  `micro-lookup` answers, a batch when the frontier passes its id.** Why:
  the tick depot's records make the processed count advance on its own
  (testing.md); the frontier is the observable the design promises, so
  polling it tests the promise as well as synchronising.

## What this stage takes from the stream store, and the one-line change

> **Revision:** this stage also takes phase 2's `rig.store.locks` (seal,
> open, wrap, unwrap, the delivery function, open-value, the content
> digest) and tonight's `rig.store.clock` (§E), and gives
> `rig.store.permit` (§B), which changes `gate.clj` in two places and
> `envelope.clj` in one.

Read as interfaces, never edited here (R15: the two builds touch different
files). The build session reads the built code first; where the built
shape differs from the stream plan's description, the adapter is written in
`micro.clj`, not the other file, except the one arity noted.

- **`rig.store.module/Store`**: the one-line change is a call inside the
  `defmodule` body, after the stream gate's declarations:
  `(micro/declare! setup topologies)`, with `[rig.store.micro :as micro]`
  added to the `ns` `:require`. `declare!` receives the module's `setup`
  and `topologies` handles and declares `*micro-offers`, `*micro-tick`,
  the `micro` microbatch topology with its three PStates, and the two
  query topologies. If `<<query-topology` or `microbatch-topology` must be
  lexically inside `defmodule`, `declare!` becomes a macro with the same
  call form (a build check, "could not settle").
- **`rig.store.envelope`**: `parse` (total, [F6]) with the gate side chosen
  by the caller; `digest`, `canonical`, `tag-of`, `mis-tagged?`,
  `store-placed?`, `placed-keys`, `landing-name`, `crossing-name`. The
  stream plan has `parse` refuse a `:by-entity`-tagged name as
  `:wrong-gate` inside the parser. This gate needs the opposite: `:by-layer`
  refused, `:by-entity` and nil accepted. If the built `parse` hard-codes
  the stream side, the smallest change is a second arity, `(parse raw
  :micro)`, made in `envelope.clj` at the merge (one function, one arity,
  the stream's arity unchanged); until then `micro.clj` calls whatever
  lower-level total parse exists and applies its own gate check with
  `tag-of`.
- **`rig.store.gate`**: `decide` (the verdict, the reason order of I-G5
  with P7, [F8] and R13's reasons, and the stamp formula), `wall-now`. The
  fold builds `decide`'s inputs (settings, record, permission row, heads
  map, clock, wall) from its working state and re-keys the outputs for the
  micro PStates; it never re-implements a check. If `decide`'s arguments
  are shaped otherwise than the stream plan says, the adapter is in
  `micro.clj`.
- **`rig.store.inject`**: `maybe-fail!` for the crash test (P14, R3), armed
  by name from the test; called, not edited.
- **`$$layers`**: read on hash(L) for a one-owner layer's `:settings`,
  `:permissions pid`, `:heads [e k fid]` (M5); never written.
- **`rig.store.client`**: nothing is called from `micro.clj`; the dispatch
  by tag (M13) is added to `client.clj` in the merge, one `<<cond`-shaped
  function; the tests of this stage use `micro.clj`'s offerer functions
  directly.

## What later stages consume, and where it is

Stated as what this stage leaves, not as their design.

- **For locks and forgetting (stage 2).** The row's `:lock-id` and
  `:lock`, the entity's `:locks` map and `:erased` ledger, in the locks
  plan's shapes, all on the value's task (I-L2); `$$persons` read locally
  in the gather (their `|all` placement makes it one seek on any task);
  the seam `micro/row-lock` called in block 2b with the fact, the act's
  record (who, subjects, permission) and the layer's settings (kind, owner
  nil for shared layers, grain); the marks on the row; a forget in a shared
  layer reaches this gate as an operator act on the value's entity (M9,
  M14) and its row is written like any other; the erasure's effect (the row
  deleted from `:locks`, the record's `:lock` excised, the ledger entry)
  is theirs, applied from block 2b through the seam; the wrap for shared
  layers (7b as written, an empty wrap for a value about no one, which
  their plan already names as "stage 3") is theirs to compute from the
  record's subjects and the row's mark (I-L1).
- **For promotion (stage 4).** The landing is appended to `*micro-offers`
  as the stream plan describes, named `[target :by-entity :landing uuid]`,
  standing on the crossing fact with its stamp carried (M6); it is decided
  as an ordinary act in the batch order, refused stale, revoked or
  mismatched like any other (OP15); whether it landed is `micro-lookup` by
  the landing's name through the frontier: `:yes` done, `:no` refused,
  `:no-answer` not yet (RD5 combines this with the stream side's request
  and crossing fact). A replayed append of the same landing is answered
  from the name row by the same name and digest (I-G2). The envelope parts
  a landing carries beyond P5's set (`:source`, `:crossing`) are theirs to
  add to `parse`; this gate reads no part it does not need.
- **For reads (stage 5).** Rows at `[e :log name idx]` with their layer;
  heads at `[e :heads [L k fid]]` with stamp, batch and replaced-by, so a
  chain's head as of a moment or a frontier is one range over the `[L k]`
  prefix (vector-key prefix contiguity is the stream plan's open Rama
  question, "could not settle"); the act's stamp, who and batch under
  `[e :answers name]`; stood-on under the name; settings versions by batch;
  tombstones under the layer id for a re-classed layer's stream-era heads,
  to be merged with `$$layers` as this gate does (M5); the frontier and its
  reader's rule; the erasure exception. Every index over values here is
  keyed by, or reachable from, `[e name idx]`: a purge is one seek per
  row; a rebuild of any projection reads `[(keypath e :log) ALL]` and
  `[(keypath e :answers) ALL]` per entity and merges on the name (2 seeks
  plus one iteration per act). PState ownership: every index a later stage
  keeps in `$$micro` or `$$micro-names` is written from this topology's
  batch (block 2a or 2b), never from a query topology or the stream gate.
- **For tools and grammars (stage 6).** Nothing new: the envelope's
  `:subjects` part arrives on the record as in the stream store.

## Namespaces and tests

> **Revision:** the tests are §F's; where the lists below differ, §F wins.

- `src/rig/store/micro.clj` — `declare!` (M1); `route-key`; `skeleton`,
  `prepare` (the fold), `row-of`, `row-lock` (M12), `wall`, all pure; the
  `micro` topology and the two query topologies; the offerer's side:
  `offer-micro!` (append with `:append-ack`, then poll `lookup-micro` at
  50 ms up to a bound, I-G3), `lookup-micro`, `act-rows`, `settings-of`,
  `permission-of`, `head-of`, `frontier`, `clock-of`, `seed-shared!`
  (M11, M13).
- `test/rig/store/micro_prepare_test.clj` — the fold without a cluster:
  property tests that `skeleton`, `prepare` and `row-of` never throw on
  generated input (the stream plan's generators plus batches of mixed
  offers); that `prepare` is deterministic (the same batch twice gives the
  same writes but the stamps' wall term); that its order is M2's; and the
  decision branches against a hand-built working state: admit whole across
  two entities; recorded and taken in one batch; the refusal reasons in
  order (fact outside the act's layer, no such layer, class mismatch, the
  four permission reasons, stale and doubled replace, stale revoke, layer
  already made, unsupported re-class, malformed control, control not
  allowed); the revocation race in both orders (a revoke named before a
  write refuses it, after it admits it); stamps strictly increasing per
  task within a batch and above carried stood-on and replaced stamps.
- `test/rig/store/micro_test.clj` — one IPC, `{:tasks (rand-nth [2 4 8])
  :threads 2 :workers 1}`, the world seeded on both sides (stage 1's
  `seed!` for the one-owner layers, `seed-shared!` here), then `testing`
  blocks: an act with facts on two entities admitted whole, its record
  found by `micro-lookup`, its rows on both entity tasks by `act-rows`,
  heads and clocks as written; a resend in a later batch answered from the
  name row with the same stamp; a reuse with other content refused
  `:name-taken` through `micro-lookup` and the first record untouched; a
  resend and its original in one batch (pause, append both, resume) decided
  once; two contents under one name in one batch: the earlier by M2's order
  holds it; a face refusal (a `:by-layer` name at this depot, a `:crossing`
  scheme, a mis-tagged name, `:who :store`) readable as data through
  `micro-lookup`; a fact naming another layer; a `:by-entity` name into
  `:alice` before its re-class refused `:class-mismatch` from `$$layers`'s
  class; a group write citing `[:alice :group :alice-hand]` refused
  `:permission-from-another-layer` (D1); a revoke of `[:alice :group
  :group]` then a write citing it refused `:permission-revoked` (D2); the
  race in one batch both ways; a permission that does not exist, one that
  does not cover (Bob's on Alice's write); the operator exempt; a stale
  replace (already replaced, in another layer, unknown), a doubled replace,
  an act with one good and one stale fact refused whole; a re-class of
  `:alice-agent` through the stream gate, then a micro offer into it that
  replaces a stream-era head (the tombstone written, the frozen head
  untouched, a second replace refused stale), cites a stream-era permission
  (admitted), and, after a micro-side revoke of that permission, is refused;
  a grain switch by the operator on `:group` as a new settings version, a
  person's refused; a crash injected in block 2b on one task: one record,
  one stamp, rows once, the frontier past the batch, the code run at least
  twice (R4, RQ 5); the frontier run: with a live tick, for at least 300
  batches of continuous small acts on entities on every task, several
  readers alternate `micro-lookup`/`act-rows` across two tasks and assert
  that no later read shows an older frontier than an earlier one and that
  an act visible on one entity task is visible on its other entity task in
  the same pass (zero violations); the same readers bypassing the frontier
  (raw `foreign-select-one` of the records and `:frontier` per task) report
  their count of inconsistent pairs, expected above zero as in phase 0, so
  the test shows the frontier does work rather than that the cluster is
  slow.
- The D cases are also run through the model (`formal.model/run` on
  `scenarios/d-cases`) and the rig's outcomes compared: both `:missing`.
  Every claim in RIG.md says what ran and what it showed; the frontier's
  claim is "ran and held for n batches", never "proved".
- Run with `clojure -M:test rig.store.micro-prepare-test rig.store.micro-test`
  from the rig folder.

## Design difficulty log

Written while designing, first person.

- **Where the batch is decided.** The hard one. I wanted each task to fold
  its own offers in the global order and avoid the leader, and it works
  until an act spans two tasks: task A admits its half tentatively, task B
  refuses its half, and every later decision on A that saw the tentative
  head is wrong; fixing that is a fixpoint across tasks, which is a leader
  in disguise. Replicating the gathered state to every task with `|all`
  and running the fold everywhere avoids the fan-out of decisions but does
  N folds and ships N copies. The leader over skeletons was forced once I
  separated what the fold needs (ids, stamps, digests) from what the
  writes need (values), and saw the values never have to visit task 0. It
  is the model's shape, which helped me trust it, but the model's shape
  was not the argument; the two-task act was.
- **The frontier: computed or written.** A minimum over N per-task values
  is exact to one commit phase and needs no tick; it costs N seeks per read
  and fails the flat-in-N rule. The written frontier costs one seek and a
  batch of lag, and needs batches to keep running. Close, and I went back
  and forth on whether a tick depot is "mechanism the spec does not
  demand"; it is the price of making the last batch visible, which the
  spec does demand (an offerer must get its answer). Settled by the
  throughput rule.
- **Whether anything may be overwritten.** I first kept the stream plan's
  heads (`NONE>` on replace) and then could not make a frontier read
  consistent: a reader whose F is two batches behind a task needs the head
  as of F, and it was gone. Batch-stamping every row and keeping the
  replaced entry followed, then the same for settings and permissions. Not
  close once traced; the cost is ids and stamps, never values.
- **Where a shared layer's settings and permissions live.** Under the layer
  id as an entity was the model's `:e layer` and the only placement that
  lets one hop read both the class and the permission, and, for a
  re-classed layer, `$$layers` too. The alternative, replicating
  permissions to every task with `|all`, makes the check local but every
  grant a write on every task; permissions are rare, so it was
  competitive, and lost on the re-class case, where the stream store's
  rows are on one task anyway.
- **The stream-era heads of a re-classed layer.** Copying them into the
  micro store at re-class is a scan of a hot layer's index inside one
  batch; deleting them is impossible (another topology's PState). The
  tombstone was the only shape left; the frozen index plus a delta is also
  what stage 5 will want for chain history. Forced.
- **Face refusals without an ack.** The stream store answers them through
  the ack and records nothing. Here nothing carries them unless something
  is written, and E1 says the offerer must learn them. Keying the record
  by the digest under the name, in a map that is not the name's answer, is
  the smallest thing that satisfies both lines of E1. I considered "the
  client validates first and a face refusal is a bug that times out", and
  rejected it because a taken name is not a client bug and must be told.
- **Reusing `gate/decide`.** Writing the micro refusal fresh would let the
  reasons drift between gates. Reusing it means the fold must shape its
  inputs as the stream event does, which it can, since every input is a
  small map. The one risk is the built signature; adapters go in
  `micro.clj`. Not close.
- **The batch order.** Arrival order does not exist across partitions;
  `claimed-when` is a client claim; the name's UUID7 is a client claim
  too, but it is the ruled name and time-ordered by design. I took it and
  wrote down where it diverges from the model.

## Self-validation against `artifact-plan-validation.md`

- Query topologies: two; each has input examples with total and meaningful
  reads; the one variable read (faces) is conditional, so no input pays a
  wasted read; both end at `|origin`, emit once.
- PState schemas: `$$micro` (entity key, hash), `$$micro-names` (name key,
  hash), `$$micro-task` (no key, per task) differ in key structure, so the
  split is justified; no `Object`; records are `fixed-keys-schema`; no
  polymorphic position (`:v` and `:lock` are text; nullable fields on one
  shape); every child collection that can pass 100 elements is subindexed;
  the unsubindexed values are records with parser-bounded parts ([F3]).
- Partitioning: `|hash` on entities and names with many keys per task; the
  hot-entity case is the by-entity counterpart of the ruled hot layer; the
  table is filled for N = 1, 16, 128, proportions sum to 1, seeks are
  totals, flat in N; the per-batch `|all` write is stated with its cost;
  a stored placement was rejected on P2's total cost; no justification
  rests on a later stage's mechanism (stage 2's slot, stage 4's landing
  and stage 5's reads are described as what is left, and the two places
  where a later stage's shape is named, the landing's name and parts, are
  the stream plan's already validated statements).
- Topologies: one microbatch topology, ruling 1's type, default microbatch
  with the ruled properties cited; no stream concern; no test-synchronisation
  argument shaped the design (M15 synchronises on the design's own
  observable).
- Production readiness: concurrent clients (two contents under one name in
  one batch: the batch order decides, the other hears `:name-taken`; two
  clients replacing one head: the earlier in order wins); a client restart
  (P6, [F13]); a worker restart at any point (the attempt is discarded
  whole, the retry is deterministic); scale (every growing collection
  subindexed, no scan on any hot path); no non-idempotent write; the
  multi-partition write is the microbatch's own atomicity.
- Internal depots: none. Cross-topology flows: the landing append is stage
  4's and is described by the stream plan with its commit boundary.
- In-memory state: none.
- Minimality, the simplest sketch: one depot by entity, one microbatch
  topology that decides each offer on its entity task, one PState of rows
  and records by entity, a clock. The plan adds the leader fold (delete it:
  an act on two tasks has no single verdict and later offers see a state
  that was never decided), the name row (I-G4 names it), the answer copies
  (OP3 names them; without them every row write hops through the name
  task), `:heads`, `:permissions`, `:settings` (delete any: a scan of an
  entity's or a layer's rows per check), the batch stamps and the frontier
  (RQ 5 and R5 require them), the faces map (E1 requires refusals as data
  and forbids them as the answer), the tick depot (without it the last
  batch is never visible), the tombstones (a re-classed layer's stream-era
  head cannot be deleted from another topology's PState). Each is required
  by a named line.
- Throughput: the cheapest design meeting every check is the one costed
  (4.31 point reads per offer on one to three tasks, a handful of no-read
  sets, one name-row read on the commit path); the lower-cost variants,
  the per-task fold and the deleted heads, fail a requirement rather than
  cost more.
- Spec coverage: OP3, OP4 (micro side), OP5 (shared), OP6, OP7 (micro
  side), OP2 (micro side), RD1's name row, E1 N0 to N3 for micro names, E2's
  chain rows on this gate's side, E3 L3 and L4 rows, E4 P0 to P2 and the
  race row, the D cases, I-O5 and I-O4 are each traced in "Writes",
  "Topologies", "The settled frontier" and "Rig choices".

## What this plan could not settle

> **Revision:** M1's question went to the probe (§G), which also says what
> stays open.

- Whether `microbatch-topology`, `declare-pstate`, `declare-depot`,
  `declare-tick-depot` and `<<query-topology` may be called from a function
  handed `setup` and `topologies` (M1). The build's first check, before the
  module: a throwaway module whose body is one such call; the fallback is a
  macro with the same call form.
- Whether `materialize>` accepts one variable from a post-agg after
  `|global` + `+map-agg`, and whether the next `<<batch` can source it as
  `($$writes :> *w)` on task 0. Fallback: the fold's writes go to a durable
  scratch PState keyed by batch id on task 0, written from the post-agg (or,
  if post-agg forbids `local-transform>`, from a `<<batch` that starts on
  task 0 and reads the aggregated state from a `+compound` into that scratch
  PState), and block 2a starts on task 0 and reads it.
- Whether `ops/current-microbatch-id` is a Long that `dec` and `<` apply
  to. Fallback: `$$micro-task :last` written by the batch's last block on
  every task and copied into `:frontier` by the next batch's block 0; the
  `≤ F` comparisons then need ids to be comparable, which the doc's
  "increments by one" gives.
- Whether a vector is accepted as the top-level key of `$$micro-names`
  (the smoke test showed vector keys as subindexed map keys, RIG.md).
  Fallback: `(pr-str name)` as the key, the name kept inside the entry.
- Whether a `<<cond` whose branches hold different partitioners unifies in
  a pre-agg-only `<<batch` (block 2a). Fallback: three `<<batch` blocks,
  one per route kind.
- The built shapes of `envelope/parse` (its gate side) and `gate/decide`
  (its arguments); the adapters are named above.
- Vector-key prefix contiguity for stage 5's `[L k]` range over `:heads`,
  as the stream plan left it.
- The `+map-agg` per-batch size on task 0 under phase 7's rates, and
  whether `depot.microbatch.max.records` needs lowering.
- A deterministic test of "half a batch": a hold inside a task's commit
  phase has no hook, so the frontier's test is a run with counts (M15), and
  RIG.md must say so.
