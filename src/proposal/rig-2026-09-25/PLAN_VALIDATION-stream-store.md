# Plan Validation — stage 1, "stream-store"

<!-- Phase 2 of the rama skill, for DECOMPOSITION.json's first entry. Written
25 September 2026 by a fresh-context adversarial session (Claude Fable 5.1,
effort max). Inputs: SPEC.md and the sources it names (PROGRESS.md "Now" 9-195;
formal-model README 42-152; model.clj in full, `baseline`); RIG.md phase 0;
IMPLICIT_SPEC.md sections 0-3, OP1, OP2, OP4-OP8, RD1, RD2, RD4, E1-E4, Open;
DECOMPOSITION.json; PLAN-stream-store.md ("the plan"); the skill's references
(stream, depot-design, depot-reference, pstate-schema, paths, dataflow,
testing, foreign-client, unique-ids, core-concepts, phase-1-plan,
artifact-plan). Vocabulary: "key" is a fact's key; "lock" is an encryption key.
Plan line numbers cite the plan as it stood before this session's fixes. -->

**Verdict: minor-fail.** Every failure below is a localized edit to the plan;
none re-opens the depot, the topology, the partitioning or the decision
order. The fixes were applied to `PLAN-stream-store.md` after the verdict and
are listed at the end under "Fixes applied". What was checked, what was
derived and what is assumed is marked in each block.

## Query topology: none

- The plan declares no query topology (plan 465-471). Every read in its
  "Reads" table (plan 130-137) is one `foreign-select-one` whose path starts
  with `(keypath layer ...)` on `$$layers`, or `STAY` on `$$clock` with
  `{:pkey layer}`; each navigates to a plain value: an answer record
  (fixed-keys, no subindexed child inside it after fix F2), a row vector (a
  subindexed vector after fix F1, so read with `foreign-select ... ALL`,
  not `foreign-select-one`), a settings map, a permission row, a Long. No
  path selects a subindexed structure whole (foreign-client.md "Subindexed
  PState queries"). Checked.
- N == M for each: one read, one meaningful read. No query topology is
  needed because no read in this stage needs two PState reads or two
  partitions (phase-1-plan.md Step 1). The reads that would (a point read as
  of a moment across chain history, a pattern read) are stage 5's by
  DECOMPOSITION.json and the plan says so (plan 144-145, 465-471). PASS.

## PState schemas

- **Grouping by (key type, partitioner).** `$$layers`: key `Keyword` (layer),
  default hash partitioner. `$$clock`: no key, one value per task. Different
  key structure, so the split is justified (plan 284-288). Within `$$layers`
  every per-layer piece (settings, answers, log, heads, permissions) is a
  field of one fixed-keys value, which is the merge rule (phase-1-plan.md
  Step 2). Checked. PASS.
- **Any `Object`?** No (plan 207-242, 272-276). The value slot is `String`
  (canonical EDN, rig choice P12). PASS.
- **Uniform records use `fixed-keys-schema`?** Yes: the answer record, the
  permission row, the log row, the settings map. PASS.
- **Polymorphic position?** None: `:reason` is a keyword or nil (nil is
  always permitted, pstate-schema.md "Any PState location can be set to
  nil"); `:permission` is a vector or nil (operator); `:v` is a String or nil
  (a retract, P13). Nullable fields on one shape, which the template allows.
  PASS.
- **Inner collections that can exceed 100 elements subindexed?**
  - `:answers`, `:log`, `:heads`, `:permissions`: subindexed, size tracking
    off (plan 214-242). PASS.
  - The log entry's inner row vector (`(vector-schema (fixed-keys-schema
    ...))`, plan 229-236): NOT subindexed. Enforcement of a size limit: none;
    the plan chooses "no act size limit" (P7, plan 707) and the ruling is
    "Acts of any size, one fact included" (PROGRESS 53). The plan's
    justification ("one act, the ruled unit of admission and reading", plan
    254-258) is a unit-of-admission argument, not a size bound. Scenario: an
    agent session imports 100,000 facts in one act (allowed); the row vector
    is one 10 MB serialized value; stage 5's point read of the chain head
    (RD2) resolves `:heads` to `[name idx]` and must load the whole act to
    return one row; stage 2's lock kept "in the record for agent sessions"
    as "a field of the row itself" (plan 772-776) makes an excision a
    read-and-rewrite of the whole act. **FAIL** → fix F1: subindex the row
    vector (size tracking off); the act is still admitted whole in one event
    and read whole by `ALL`; a row is one seek. Localized.
  - `:stood-on (map-schema PersistentVector Long)` on the answer record (plan
    224): NOT subindexed, on the record every resend and every replay reads
    (the hot path the plan itself protects in rejecting Option C, plan
    196-201). Enforcement: none. Ruling 3 makes "a model's or a person's
    reads ... the crossing's exact list, always" (PROGRESS 78-80), so an act
    that stood on a pattern read's 10,000 matches carries 10,000 entries
    (about 800 KB) into the record read on the replay path. **FAIL** → fix
    F2: `:stood-on` moves to its own subindexed map keyed by name (inner map
    `{fid stamp}` subindexed, size tracking off); the answer record keeps the
    bounded parts. Localized.
  - `:subjects (set-schema Keyword)` on the answer record (plan 227):
    owner ∪ what the tool named (P15). Enforcement: none in the plan. **FAIL**
    → fix F3: the parser caps carried subjects (256, rig choice; over the cap
    is refused as malformed, unrecorded), which is an enforcement mechanism
    and keeps the record bounded. Localized.
  - `:mark`: at most the two marks the parser knows (plan 558). Enforced by
    the parser. PASS.
  - `:settings`: four fields. PASS.

## Partitioning

- **Writes and their partitioner.** Every write of this stage lands on the
  task the depot's `hash-by :layer` chose; the topology has no partitioner
  (plan 299-304, 355-357). `f(layer) = hash(layer) mod N`. `|hash`
  indicators: the keyspace is large (layers accumulate: OP5 "each task is
  home to ever more layers"; "agent sessions may be many per person per
  day"), so hash variance is negligible; a hot key is a hot layer, which
  ruling 2 names as the re-class case ("a hot layer can be re-classed",
  PROGRESS 73-74) and which the plan states as the accepted non-optimal
  case (plan 544-547). No `|all`. PASS.
- **Table filled for N = 1, 16, 128?** Yes (plan 509-537). Categories (a)
  to (e) cover the common input (an ordinary act), replaces, resends and
  replays, operator acts and face refusals; proportions 0.60 + 0.25 + 0.10 +
  0.04 + 0.01 = 1.00. Weighted sums computed. Seeks counted as totals: every
  read touches one task, so the total is the per-task count; no `|all`
  read, no fan-out. Flat in N. PASS on growth.
- **Recount.** The plan's per-category seeks omit the `$$clock` read
  (`(local-select> STAY $$clock :> *clock)`, plan 373), which is a read of
  a class-reference PState "backed by a single value on disk"
  (pstate-schema.md "Class-reference schemas"), one seek by the template's
  counting whether or not the block cache hits. It also reads the layer's
  entry and the clock before knowing whether the offer is already decided,
  so category (c) pays for reads it does not use. Recounted with the reads
  ordered record-first (fix F4): (a) settings + record + clock + permission
  = 4; (b) 4 + r = 5 at r = 1; (c) record only = 1; (d) settings + record +
  clock (+ the permission row for a revoke) = 3-4, counted 4; (e) 0.
  Weighted = 0.60×4 + 0.25×5 + 0.10×1 + 0.04×4 + 0.01×0 = 3.91 at every N.
  Still flat; the plan's 3.12 was an undercount, not a growth. **FAIL** on
  the table's accuracy → fix F4 rewrites the rows and the sum. Localized.
- **Justifications from the spec only?** The placement is ruling 2 ("by
  layer for one-owner layers"; N fixed) and I-G4/I-G9 (answers by name on
  the layer's home; per task the log and the stamp). No partitioning
  justification rests on a later stage's mechanism. Rig choice P1 (one
  module for both stores) cites the ruled promotion (the read-out on the
  owner's partition sends the landing, PROGRESS 150-153, README 60-62) and
  R6, which are rulings and rig choices, not assumed mechanisms; the
  landing's mechanics are "described as theirs" (plan 797-809). PASS.
- **Stored placement considered on total cost?** Yes: a placement table
  cannot serve the depot partitioner (a pure function of the record on the
  appending client, depot-design.md "Depot partitioners run on the appending
  client") and would add a read per offer for the same `f` (plan 323-328).
  Rejected on computed cost, not on complexity. PASS.

## Topologies

- **Microbatch unless justified?** One stream topology, `gate`, justified by
  both stream reasons: (a) a person's write visible to their next read with
  no optimism (I-O6; OP1 latency single-digit to low tens of ms); (b) the
  answer returned through the append's ack (`ack-return>`), and `:ack` must
  not return before the decision is visible so a resend or lookup after an
  error reads the record (I-G3, RQ 4). Microbatch gives neither (at least
  300 ms; `:ack` on a microbatch depot confirms only the depot write,
  foreign-client.md "Ack visibility by context"). Ruling 1 also names the
  type. PASS.
- **Low-latency writes on stream?** OP1, OP2, OP4, OP5, OP6, OP7, OP8 are all
  one offer decided by `gate`; none uses microbatch. PASS.
- **Every concern in the stream topology needs stream?** The topology does
  one thing per record: decide and answer. The settings and permission
  projections, the heads maintenance and the log write are parts of the
  decision that must be visible with the answer (I-G6 whole or not at all;
  I-G3 the record answers a resend). Moving any to a microbatch would let a
  resend find an answer whose facts are not yet visible (E1 N2 × resend:
  "unchanged, no second admission" needs the record and the log together).
  PASS.
- **Any choice made on test-synchronization grounds?** None found; the plan
  says so (plan 984-985) and the sections bear it out. PASS.

## Production readiness

- **Multiple concurrent clients?** Two clients, one layer: both records land
  on the layer's home task and are decided in depot order (stream.md
  "Partition ordering"). Two contents under one random name at once: the
  first decided holds the name; the second reads the record, digests
  differ, `:taken`, nothing written (E1 N1 × other content). The same
  streaming batch: the second event sees the first's uncommitted writes
  (stream.md "Individual events can read their own writes immediately";
  pstate-schema.md "Read visibility"). Two clients replacing one head: the
  first decided admits, the second is `:stale-replaces` (OP1 Concurrency).
  PASS.
- **After a client process restart?** A client that kept the map it sent
  resends it and takes the answer (RQ 4, P6). One that lost it makes a new
  name; if the first was admitted, the same content is admitted twice under
  two names. That is inherent in "names: random, made by the offerer"
  (PROGRESS 58): the store cannot know two names are one intent, and the
  model has the same property. A restarted client that kept only the name
  can read the record by name; the plan's `lookup` demanded a digest, so
  fix F13 lets `lookup` take a nil digest and return the record as data
  (the `:name-taken` comparison only when a digest is given). PASS after
  F13.
- **Worker restart at any point?** Traced under "Replays" below: before,
  during and after the writes, and a completed record replayed. PASS.
- **Large scale?** Every unbounded per-layer collection subindexed (after
  F1, F2); no scan on any hot path; the log entry and the stood-on map are
  the two collections the plan had left unbounded and are fixed above.
  PASS after F1-F3.
- **Non-idempotent writes in the stream topology?** None. Every write is a
  `termval` of a value computed before any write (answer record, log rows,
  heads puts, settings fields, permission rows, `$$clock`) or a `NONE>`
  (heads deletes); no `AFTER-ELEM`, no `term inc` (plan 164-173, 423-430).
  Resolution for each: "Made idempotent by: a set of a value derived from
  the record, keyed by name, fact id, pid or layer; a replay that finds the
  record writes nothing". Checked against stream.md "Stream retries and
  exactly-once". PASS.
- **Multi-partition writes?** None: no partitioner in the event. PASS.

## Internal depot usage

None (`*offers` is client-appended, plan 299-315). The landing handoff to
the micro depot is stage 4's and stage 3's (plan 797-809). PASS.

## Cross-topology correctness

No data flows between topologies in this stage. PASS.

## Stream topology correctness

No `depot-partition-append!` in this stage. The plan's description of stage
4's landing append carries the commit boundary `(|direct
(ops/current-task-id))` before it (plan 801-805), as stream.md "When PState
writes commit" requires. PASS.

## In-memory state efficiency

No TaskGlobal, no cache (plan 290-295, 598-601). The one candidate, the
layer's settings, is one seek that the block cache serves for a hot layer;
a TaskGlobal copy would save no seek the plan counts and would be one more
thing to keep right across restarts. PASS.

## Minimality — adversarial simplification

**Simplest sketch.** One depot `*offers` partitioned by layer; one stream
topology; one PState `{layer (fixed-keys {:acts {name {answer + rows}}})}`;
one per-task clock. The gate reads the act record by name, decides from the
layer's log by scanning for the class fact, the permission fact and the
chain, writes one record, returns the answer through the ack.

**Diff against the plan.** The plan adds `:heads`, `:permissions`,
`:settings` (projections), the answers/log split, the digest, the name tag,
the envelope parser, the test crash hook, and (after fixes) `:stood-on` as
its own map. Each block:

### `*offers` (one depot, by layer)
- **Delete it**: no writes enter. Merge/bypass: one depot per operation
  would lose the ruled order between a replace and its head, a revoke and
  the writes it governs, a re-class and the offers after it (OP1
  Concurrency; OP6 "a revocation and the writes it governs are ordered by
  one gate"; depot-design.md "Same depot when events require local
  ordering"). Required. PASS.

### `gate` (one stream topology)
- **Delete it**: nothing decides. Merge: there is nothing to merge into;
  at most one stream topology (artifact-plan.md topology count rules) and
  this is it. PASS.

### `$$layers` `:answers` (by name)
- **Delete it**: "answers by name" (SPEC phase 1; I-G4 "An answer is found
  by name plus layer ... Never a scan"), the retry from the record (I-G3),
  the digest check (I-G7) all need a point read by name. Required.
- **Merge with `:log` (Option C)**: the plan rejected it on the replay path
  reading the act's rows with every answer (plan 196-201). After fix F1
  the rows are a subindexed collection, so the cost argument changes shape
  but the conclusion holds for a different reason: a record that contains a
  subindexed child cannot be returned by `foreign-select-one` (foreign-
  client.md "Subindexed PState queries"), so the answer record must stay a
  plain value beside the rows, not around them. Constructed alternative: a
  record `{:answer ... :rows (vector-schema row {:subindex? true})}`; the
  lookup then needs field-by-field reads or a `submap`, and the gate's local
  read of the record would carry a RocksDB handle into `decide`. Same seeks
  on the decision path (1 record read), worse read API, no saving. PASS.

### `$$layers` `:log` (rows by name)
- **Delete it**: the admitted facts are not stored; "the log" (SPEC phase
  1; PROGRESS 26 "the log appends") is lost; stage 5 cannot rebuild any
  index (PROGRESS 167-168). Required. PASS.

### `$$layers` `:heads` (`[e k fid]` → stamp)
- **Delete it**: `replaceable?` ("A fact may replace r only while r heads
  its chain in that layer", model.clj 313-319; R7 "a stale or doubled
  replace") needs, per replacing fact, whether r is unreplaced on this
  entity and key in this layer. Without the index that is a scan of the
  layer's log (every act's rows) per replacing fact: a layer of 100,000 acts
  is 100,000 iterations per replace against 1 seek. Constructed and costed;
  the index wins by 10^4. Merge into `:log`? A different key. PASS.

### `$$layers` `:permissions` (pid → grant/revoke)
- **Delete it**: the exists and revoked checks (`permission-fact`,
  `revocation`, model.clj 323-331) scan the layer's log for permission and
  revoke facts: same arithmetic as `:heads`. Merge into `:settings`? A
  permission set is unbounded (a hand session holds a session permission
  per layer the person writes into); `:settings` is a plain field. PASS.

### `$$layers` `:settings` (kind, owner, class, grain)
- **Delete it**: the class check ("verified by the gate against the
  layer's class fact", PROGRESS 72-73) scans for the class fact. Merge: it
  is already a plain field of the layer's entry read at one seek. PASS.

### `$$layers` `:stood-on` (after fix F2)
- **Delete it**: the clock promise's inputs are not kept; the envelope's
  "based on" part (PROGRESS 50) is lost from the record; a reader cannot
  audit what an act stood on. Merge into the answer record: that was the
  plan and it is unbounded there (FAIL above). PASS.

### `$$clock`
- **Delete it**: "never backward within a unit" (ruling 4) needs the task's
  last stamp; deriving it from the max over every layer's answers on the
  task is a scan. Merge into `$$layers`: a per-task value under a sentinel
  layer key would make a per-task value look like a layer, and a class
  PState is the documented shape for one value per partition. PASS.

### The digest (keyed, derived at the gate and at the lookup)
- **Delete it**: "a reused name with different content is refused by a
  digest of the offer" (PROGRESS 132-133; `:digest-check` in `baseline`)
  needs it; without it the record answers any content under the name
  (model `ruled` `:digest-check false`, README's E failure). Merge: the
  plan derives it from one function shared by gate and client (P6). PASS.

### The name tag (`[layer class scheme id]`)
- **Delete it**: "A name carries the layer and class it was made for and
  reaches one gate only" (PROGRESS 131-132; `:name-tag :layer-and-class`).
  Without it E fails (README "Why the name tag"). Merge: the tag is the
  name's first two elements, read with no parse. PASS.

### The envelope parser (`parse`, total)
- **Delete it**: "the gate refusing parts it does not know" (PROGRESS 51-52)
  and "a gate never throws" (RQ 3) both need a total parse; a schema
  violation in a write throws inside the topology. Required. PASS.

### The crash hook (`inject/maybe-fail!`, test only)
- **Delete it**: R7's last case ("the same answer after a crash mid-offer")
  and R3 (failure injection through global atoms) need a crash on either
  side of the writes. Test-only, no production cost. PASS.

### The client library
- Plain functions over the foreign API; the one nontrivial protocol is
  `offer-until-answered!` (RQ 4). Required by I-G3. PASS.

Nothing in the plan is absent from the optimal design that meets the
checks above. Minimality: PASS.

## Throughput — adversarial

Frequent operations: the decision (once per offer) and the lookup (per
client error). Lowest-aggregate design constructed per operation:

- **Decision, ordinary act (a).** Reads needed by the checks: the record
  (recorded/taken/decide), the settings (class), the permission row (exists,
  revoked), the clock (stamp). Four point reads on one task, no iteration.
  Could settings and permission ride in one value? Only if permissions were
  bounded, which they are not. Could the clock ride in the answer record?
  It is per task, not per name. Could the record and the settings be one
  seek? The record is under the name, the settings under the layer: two
  RocksDB keys. So 4 seeks is the floor for the plan's data model; the plan
  counted 3 by omitting the clock (fixed in F4). No cheaper design found
  that keeps every check.
- **Decision, recorded (c).** Only the record is needed: 1 seek. The plan
  read settings and clock first (2-3 seeks); fix F4 orders the record read
  first and conditions the rest. A real saving on the replay path RQ 2 puts
  on every record since the checkpoint.
- **Replace (b).** +1 seek per replacing fact (`:heads`), which is the
  index's purpose; no cheaper check exists.
- **Writes per admitted act.** 1 record + f rows (after F1; f row puts in
  one batch replace one f-row value: bytes are the same, puts are
  sequential keys) + 2r heads + s settings + p permission rows + |stood-on|
  (after F2) + 1 clock. All no-read sets. Bounded by the offer.
- **Lookup.** 2 seeks (layer record, then the name), or 1 if Rama addresses
  a subindexed element by composite key; the plan counts 2, conservative.

No design with lower aggregate cost that meets every check was found.
Throughput: PASS after F4.

## The six traces the orchestrator asked for

### 1. Is the plan's log adequate as *the* log?

Sid's phase 1: "Per task: the log, answers by name, the partition's stamp."
The rulings: "the log appends and time runs one way" (PROGRESS 26); "One
log. Erasure by destroying a lock, not values beside the log" (PROGRESS
45-46); rig constraint "Every index over values is rebuildable from the log
or purgeable by value id" (PROGRESS 167-168).

The model's partition keeps `:log []` in admission order and `:stamps []`
beside `:answers {}` (model.clj 135-141). The plan keeps, per layer on the
layer's home task, `:log {name → rows}` and `:answers {name → record with
the stamp}` (plan 105-116, 214-236), both keyed by the name and sorted by
the name's serialized form, which within one class and scheme is UUID7
order (client time at name-making), not admission order.

Trace, one task, two layers homed on it, `:alice` and `:alice-hand`. Acts
A1 (`:alice`, stamp 5), H1 (`:alice-hand`, stamp 6), A2 (`:alice`, stamp 7,
replaces `[A1 0]`). The task's `$$clock` is 7. Recover admission order:
every stamp is on `:answers[name].stamp`; stamps on one task are strictly
increasing (`max(..., clock+1, ...)`, plan 118-122), so stamp order is
admission order per task, and per layer (a layer sits on one task while by
layer). A rebuild of any index over `:alice` reads `[(keypath :alice
:answers) ALL]` and `[(keypath :alice :log) ALL]`, both sorted by the same
key, and merges them on the name: 2 seeks + 2×(acts) iterations per layer;
a per-task order across layers is a merge over the layers homed here, fine
for a rebuild and needed by no read the spec names (reads are per layer:
RD2, RD4). A purge by value id `[name idx]` is `(keypath layer :log name
idx)` after F1: one seek. A read as of T in `:alice` (stage 5) needs every
act with stamp ≤ T: without a by-stamp index it is a scan of `:answers`;
stage 5 can keep `stamp → name` per layer, rebuildable from `:answers`, as
the plan says (plan 813-814). Chain history for (e, k) as of T needs the
replaced facts too, which `:heads` drops; that index is rebuildable from
`:log` + `:answers` and is stage 5's to add.

Two things the plan did not state and must, because they decide whether
stage 5 can build on this stage (fix F12): (a) the log *is the pair*
`:answers` + `:log` (+ `:stood-on` after F2) under one key, not `:log`
alone, since the stamp, who, permission and because-of live on the record
and the rows carry neither stamp nor layer; (b) a PState is written only by
its owning topology (core-concepts.md "PStates"; phase-1-plan.md Step 4), so
every index a later stage keeps in `$$layers` is written from *this* stream
gate's event (the same event, one atomic group), never from a query or
microbatch topology; the plan's "belongs in `$$layers` if kept" (plan 814)
left that implicit.

**Judgment: a storage choice, not a divergence from the ruling.** The
ruling is about meaning: facts appended, stamped once, never rewritten, time
one way. Nothing in the plan rewrites or re-stamps; the position of every
fact (act name and index) and its stamp are kept; admission order is
recoverable exactly (strictly increasing stamps per task). A divergence
would be a lost or mutable stamp or position, and there is none. The cost
of answering reads as of a moment and of rebuilding or purging every index
over values is acceptable: per-layer range scans for rebuilds, one seek per
purge, and the by-stamp and chain indexes stage 5 must add are derivable
from the pair. Marked: checked against the plan and the sources; the seek
counts are derived from pstate-schema.md's storage model, not measured.

### 2. The clock promise (ruling 4) against carried stamps (P9)

Ruling 4: "Never backward within a unit; never earlier than anything the
fact stood on." `stamp-for` (model.clj 455-466) takes `max` of the wall,
each landing partition's clock + 1, and the *real* stamps + 1 of what the
offer stood on, what its facts replace and its source, read through
`fact-by-id` over every partition of both stores. The plan (P9, plan
726-736): the offer carries `:stood-on {fid stamp}` as the offerer read
them; the gate uses the carried stamps and the replaced facts' *recorded*
stamps from `:heads`, and re-reads nothing else.

Where a carried stamp can break the promise, by cases:

- **Stood-on fact admitted on this task** (same layer, or another layer
  homed here): the task's clock is ≥ every stamp it ever gave, and the new
  stamp is ≥ clock + 1, so the promise holds *whatever the carried value
  is*, even a lie. Derived from the formula; the carried stamp is redundant
  for these.
- **Replaced fact**: its recorded stamp is read from `:heads` (`[e k r]` →
  stamp, plan 259-263); real, not carried. Holds.
- **Stood-on fact on another task or in the other store**: the promise
  holds if the carried stamp is the fact's real stamp, which an honest
  client has because it read the fact and the read returned the stamp; it
  breaks when the client carries a lower stamp (a bug, a lie, or a stamp
  for a fact that does not exist, which the model also skips: OP1 edge
  "The model skips unknown ids when stamping", O3). Under the model this
  case is a read of another partition, which the README says the toy makes
  atomic and a real store cannot ("a real check against another partition
  is a read too", README 101-104), and under I-O4 a check against the other
  store "is a read, stamped in the checking store". The plan makes that
  read the client's read and carries its result.

Scenario: Alice's hand session (task 1) admits H1 at stamp 900; Alice's
own layer (task 0, clock 100) receives A3 standing on `[H1 0]` with the
carried stamp 900 → A3's stamp is max(wall, 101, 901) ≥ 901. Holds. Same
with the carried stamp forged as 50 → A3 could be stamped 101 < 900: the
promise is broken *relative to the real stamp* and kept *relative to the
claim*. The store already takes `:who`, `:permission` and the identity of
the stood-on facts from the client unsigned ("Operator trusted at launch.
No signing", PROGRESS 63-64); trusting the carried stamp is the same trust
extended to one more part, and the rig's client is in-process test code.

**Judgment: acceptable as a rig choice, not a requirement broken**,
provided the plan says exactly this (fix F10): which stood-on stamps the
clock covers regardless (facts admitted on this task), which rest on the
carried value (other tasks, the other store), that the carried map is kept
on the record so the claim is auditable, and that the model's read of the
real stamps is what a kept store would do (a read stamped in the checking
store, per I-O4), with a same-task verification available at one seek per
stood-on fact if a later stage wants it. Phase 8's replay carries the real
stamps (the rig client reads them), so the model's and the rig's stamps can
be compared. Marked: derived from the formula and the sources; not run.

### 3. Replays

Retry mode `:all-after` (P11): on a failure the failed record and every
later record on that partition replay from the source, in order
(stream.md "Retry semantics"; depot-design.md "`:all-after` for
order-sensitive streams"). The event has no partitioner, so all its writes
are one atomic group on one task (RQ 1; stream.md "Writes between two
partitioners form one atomic group").

- **Crash before the writes** (hook before the writes, plan 414-416):
  nothing committed, no ack sent (the ack goes out only when the event tree
  completes, stream.md "Ack return"). Replay: same record, same state, same
  reason; the stamp is `max(wall', clock+1, ...)` and may differ from what
  the lost attempt would have computed, but no attempt gave a stamp, so one
  stamp exists. Same answer (the reason is a function of unchanged state).
  Nothing admitted twice. Holds.
- **Crash during the writes** (between two `local-transform>`s): the
  streaming batch's writes are discarded together (RQ 1 ran and held:
  "The attempt's writes were discarded"). As above. Holds.
- **Crash after the writes, inside the event** (the plan's second hook,
  plan 414-416): still before the batch commits (commit is at the end of
  the streaming batch, pstate-schema.md "Writes are batched to disk"), so
  the writes are discarded and this is the same case as above. The plan's
  "after them" hook therefore does not exercise a *committed* record's
  replay; see the next case.
- **Replay of a completed record** (RQ 2: "Records that had already
  completed since the last checkpoint replay too"; D4; E1 N2 × crash):
  the record's writes are durable; the replay reads `answers[name]`, the
  digest of the replayed record equals the recorded digest (same bytes),
  `:recorded`, nothing written, the ack carries the recorded answer and
  stamp (to no waiting client, harmless). Same answer, same stamp, nothing
  admitted twice, no stamp given twice. Holds. The plan's test list names a
  crash "before the writes and after them" but not a completed record
  replayed by a later crash on the same task; fix F13 adds it (crash on
  offer B after offer A completed on the same layer; assert A's answer,
  stamp and single log entry unchanged, A's code ran at least twice, R4).
- **A client's resend racing the replay**: the client's append throws (RQ
  4), it looks up (nil or the record), resends the same map; the resend is
  a later depot record on the same partition, replayed in order after the
  original under `:all-after`; whichever is decided first records, the
  other is `:recorded`. Holds.
- **Two contents, one name, one streaming batch, batch fails**: both
  discarded, both replay in order, same outcome. Holds.
- **Why `:all-after` and not `:individual`**: with `:individual`, a record
  failed by a timeout (`topology.stream.timeout.seconds`) without a crash
  is retried while later records on the partition proceed; a later replace
  could then be decided before the earlier act it replaces, refused stale
  where depot order would have admitted it. `:all-after` keeps depot order
  under retry; every replayed decided record costs 1 seek (after F4).
  Checked against stream.md and depot-reference.md. PASS.

### 4. No input can make topology code throw (I-G1, RQ 3)

The plan's argument (plan 444-456): source binds the raw record; `parse`
is total and wraps its body in `try`; `decide` is total; every written
value has the schema's class; the ack map is plain data. Holes found:

- **`try` catching `Exception` does not catch `StackOverflowError`.** A
  client appends a value nested 200,000 deep (EDN data, so it passes the
  whitelist by type); `parse`'s recursive walk and `canonical`'s recursive
  print overflow the stack; an `Error` is not an `Exception`; the worker
  dies (fatal, RIG.md finding 1), the record is poison, and `:all-after`
  replays it for ever. **FAIL** → fix F6: the walk carries an explicit
  depth bound (32) and refuses deeper values as malformed, so it never
  recurses deep; and the outer guard catches `Throwable`. (A hostile
  record can still overflow Rama's own deserializer before the source
  binding; that is outside topology code, and the rig's client is
  in-process; noted, not fixable in the plan.)
- **`vec` does not rebuild a `subvec`.** `(vec x)` returns `x` itself when
  `x` is already a vector (Clojure 1.12 `vec`: `(if (vector? coll) (with-meta
  coll nil) ...)`), and a `subvec` is a vector, so the plan's "rebuilds every
  vector with `vec`" (plan 273-274) does not guarantee `PersistentVector`.
  In practice the record arrives through nippy, which thaws every vector as
  `PersistentVector`, so no subvec reaches the gate; but the stated
  guarantee is false. **FAIL** (claim, low practical risk) → fix F6:
  normalise with `(into [] ...)`, and numbers in Long positions with
  `long`.
- **`ops/explode` over an empty collection emits nothing and skips
  everything downstream** (syntax.md "operations that emit 0 tuples cause
  all downstream code in that branch to be skipped"). The plan names
  `ops/explode` over the replacing facts to collect the heads reads (plan
  379-381); an act with no replaces would emit zero times and skip the
  decision and the writes: no answer, no ack, the client times out and
  resends for ever. Not a throw, but a silent loss of every ordinary act.
  **FAIL** → fix F7: the heads reads are a `loop<-` whose termination
  emits the (possibly empty) map; `ops/explode` is struck. Also: `*perm`
  and `*heads` must be bound (nil) on the exempt and no-replace branches,
  or the attach point fails to compile ("Attach point missing needed
  logvar", dataflow.md).
- **A caught failure in `decide` has no named outcome.** The plan says
  `decide` is "likewise wrapped" (plan 447) but not into what. **FAIL**
  (minor) → fix F6: an unrecorded face refusal `:gate-error`, so a resend
  after a fixed gate can be decided.
- **`:who :store` from a client.** The parser accepts `:who :store` (plan
  63) and `exempt?` skips every permission check for it; a client can
  claim it under the `:offer` scheme. Under "no signing" every `:who` is a
  claim, but `:store` is not a person and no store-placed act exists in
  this stage. **FAIL** (a bypass, not a throw) → fix F6: `:who :store` is
  refused on its face (`:reserved-who`) from the client-appended depot in
  this stage; stage 4 decides how the store's own steps enter.
- **The whole-entry read** `(local-select> [(keypath *layer)] $$layers :>
  *entry)` (plan 370-371) selects a fixed-keys value whose four children
  are subindexed; on the local task that is a value carrying RocksDB handles
  (troubleshooting.md "Serializer not defined ... RocksDBWrapper" is the
  cross-boundary symptom). It is passed to a plain fn that reads only
  `:settings`. Not a throw on this path, but fragile and one seek for a
  handle-laden value when `[(keypath *layer :settings)]` returns the plain
  map at the same cost and is nil for an unmade layer too. → fix F4.
- Everything else holds: the source binds the whole record; keyword and
  vector path keys after normalisation; `nil` permitted in every nullable
  field; `(long ...)` on stamps; `ack-return>` of plain data; `STAY` on a
  class PState with `:initial-value 0`; `TopologyUtils/currentTimeMillis`
  behind a `defn`. Checked against paths.md, pstate-schema.md, dataflow.md.

After F6 and F7 the invariant is credible; the property test the plan
names (generated garbage into `parse` and `decide`, plan 871-873) must
include deep nesting and a `subvec` in a vector position, and the IPC test
must include `:who :store` (fix F13).

### 5. The digest is keyed; the client holds the secret

The rig constraint "Fingerprints over values are keyed" (PROGRESS 170) is
what makes a fingerprint safe to keep: without a secret, a digest over a
low-entropy value (a `:note` "yes") is a confirmation oracle for that value
after the value is forgotten, and the answer record's digest survives every
forget (it is the name's answer for ever, D4). The plan puts the secret in
code and has the client compute the digest for `lookup` (P6, plan 689-698).

For the rig: **acceptable**, on three counts, all now written into P6 (fix
F11): (a) the rig keeps no records and its client is in-process test code;
(b) a client without the secret can still get its answer, by resending, in
which case the gate computes both sides (RQ 4's road), so the client-side
digest is a convenience for a read-only lookup, not a requirement; (c) the
consequence is named for stage 2: the record keeps a keyed fingerprint over
plaintext values that outlives a forget, and with the secret in the client
the guess-confirmation protection the constraint exists for is gone in the
rig; a kept store keeps the secret in the store. One more hand-off named in
the same fix: the depot retains every offer's plaintext values for ever
(plan 627-628; "no trimming policy"), which is I-L4's "the queue of offers a
gate reads from" and open item O2, stage 2's to answer.

### 6. The Rama claims the plan leaves unsettled

- **Vector keys in a subindexed map** (`clojure.lang.PersistentVector` as
  the key class of `:answers`, `:log`, `:heads`, `:permissions`, `:stood-on`).
  pstate-schema.md requires a resolvable JVM class and says class-typed
  positions match exactly; `PersistentVector` is resolvable; whether Rama
  accepts a vector as a subindexed map *key* (serialized as the RocksDB key)
  is not stated in any reference read. Interfaces are accepted as schema
  types (the `IEvent` example), so `clojure.lang.IPersistentVector` is the
  fallback the plan names, and a string key the last resort.
- **A fixed-keys value with subindexed children created on the first
  nested write.** The schema shape is documented as legal ("Subindexed
  structures can be nested anywhere: ... inside fixed-keys-schema";
  "Fixed-keys schemas cannot themselves be subindexed, but their values can
  contain subindexed structures"). Whether `[(keypath layer :answers name)
  (termval rec)]` on an absent `layer` creates the record and the child is
  the ordinary nil-navigation semantics ("keypath on nil → nil", transforms
  create) and paths.md says subindexed structures navigate like plain ones;
  not shown for this exact shape.
- **A whole-vector `termval` into a subindexed vector location** (after
  F1): not stated in the references; the per-index `termval` form is.
- **`{:pkey layer}` routing of `$$clock`** to the same task as `hash-by
  :layer`: depot-design.md's colocation example rests on the depot
  partitioner and the PState key partitioner agreeing; for a class PState
  with `:pkey` this is assumed.
- **Prefix contiguity of vector keys** (stage 5's `[e k]` range over
  `:heads`): not needed by this stage; the build should still report what
  it sees.

**What the build must check first** (fix F14 names it): before the module,
a `create-test-pstate` (testing.md, module-free, seconds) with the exact
`$$layers` schema: write `[(keypath :alice :answers [:alice :by-layer :offer
u]) (termval rec)]` on an empty PState and read it back; write and read a
`:heads` row under a nested vector key `[:e0 :note [[:alice :by-layer :offer
u] 0]]` and delete it with `NONE>`; write a three-row act into `:log` by one
whole-vector `termval` and by per-index `termval`s, read it with `ALL`;
iterate `:answers` with `ALL` and record the key order; and, once the module
is up, `foreign-select-one STAY $$clock {:pkey :alice}` against the task
the gate wrote. On a failed key class, `IPersistentVector`, then a string
key; on a failed nested creation, the five-PState fallback with the same
paths (plan 1017-1021).

## Spec coverage — trace every operation and constraint

The world in every trace is the model's: layers `:alice` (personal, home
task hash(:alice) mod N, call it T0), `:alice-hand` (hand, T1),
`:alice-agent` (agent, T2); own permissions `[:alice L L]` for the three;
session permissions `[:alice L :alice-hand]` for L in `:alice :alice-agent
:group :base`, all kept in `:alice-hand`. Names are written `n1`, `n2` for
`[layer class :offer uuid7]`. Fault-tolerance (FT) and race checks are
answered per block; the mechanism for FT is the same everywhere (one atomic
group on one task; a replay finds the record or decides on unchanged state)
and is stated once under "Replays" above, so each block says what differs.

### Sid's phase 1, clause by clause (the user-facing spec)
- **Source**: "A depot for offers, a stream gate topology, the envelope as
  ruled (small positional core, the named parts, a version marker, unknown
  parts refused), one-owner layers placed by layer, N tasks fixed at launch.
  Per task: the log, answers by name, the partition's stamp. Names random,
  made by the offerer, tagged with layer and class; the class checked
  against the layer's class fact; a reuse with different content refused by
  digest; a retry answered from the record; acts whole or not at all; layer
  on the act. Tests: the model's stream cases."
- **Trace**: depot `*offers` (plan 299); topology `gate`, stream (plan 337);
  envelope: positional core `{:e :k :v :replaces}` + index by position,
  named parts (who, layer, permission, session, stood-on, because-of,
  claimed-when, subjects), `:version 1`, unknown part → `:malformed`
  unrecorded (plan 58-89, P5, P7); placement by layer via `hash-by :layer`
  (plan 299-304); N from the launch config `{:tasks n}` (plan 876); per
  task the log (`:log` + `:answers` + `:stood-on`, trace 1), answers by name
  (`:answers`), the stamp (`$$clock`); names `[layer class :offer uuid7]`
  made by `make-name` client-side (plan 827); class check against
  `:settings :class` (plan 397-400); reuse by digest → `:taken` (plan
  391-392); retry → `:recorded` (plan 388-390); whole or not at all: one
  reason for the act, one event (plan 164-173); layer on the act:
  `:fact-outside-the-acts-layer` (plan 84-86). Tests: R7's list mapped in
  plan 876-896.
- **FT**: per "Replays". **Race**: per OP1. **Flaws**: the test list lacked
  the completed-record replay (F13). **Verdict**: PASS after F13.

### OP1. Offer an act into a one-owner layer
- **Source**: "Admitted whole on the layer's home task, all facts at one
  stamp (I-G6). Every fact names the act's layer, else the act is refused
  ... The offer's class equals the layer's class fact, else refused ... The
  cited permission covers this person and this layer, lives in this layer,
  exists and is not revoked ... A replace names an unreplaced fact of the
  same layer, entity and key; no two facts of one act replace the same fact
  ... The stamp keeps I-O2. The answer, with digest and stamp, is kept on
  the home task."
- **Trace**: Alice offers n1 into `:alice`, class `:by-layer`, permission
  `[:alice :alice :alice]`, facts `[{:e :e0 :k :note :v "a"} {:e :e1 :k
  :mention :v {:persons [:bob]}}]`. Client: `hash-by :layer` → T0. Gate on
  T0: parse ok; tag `[:alice :by-layer]` = name's; record nil; settings
  `{:class :by-layer ...}`; clock 0, wall W; permission row present,
  unrevoked; no replaces. `decide` → yes, stamp s1 = max(W, 1). Writes:
  `answers[n1]` = {yes, s1, digest, who, class, permission, ...};
  `log[n1]` = the two rows; `heads[[:e0 :note [n1 0]]]` = s1,
  `heads[[:e1 :mention [n1 1]]]` = s1; `$$clock` = s1; ack `{:answer :yes
  :stamp s1 :name n1}`. Each check of the source maps to a reason in the
  plan's order (plan 396-409), the same order as `refusal` (model.clj
  405-453) with the rig's two reasons placed (`:no-such-layer` before
  `:class-mismatch`, `:stale-revoke` after `:stale-replaces`) plus, after
  F8, `:layer-already-made` and `:unsupported-reclass`.
- **FT**: one atomic group; per "Replays". **Race**: two offers into
  `:alice` at once are ordered by the depot partition; the second sees the
  first's writes. **Flaws**: the parser's totality (F6) and the explode
  hazard (F7) sit on this path. **Verdict**: PASS after F6, F7.

### OP2. Resend under the same name; look up an answer
- **Source**: "Same content, same name: the recorded answer and stamp,
  nothing decided again, even when the reason for a no has since gone away
  ... Other content, same name: refused, name taken; the first record is
  untouched ... Layer or class other than the name's tag: refused on its
  face, reading nothing ... Neither a name-taken nor a face refusal is
  written as the name's answer ... 'No answer yet' is distinguishable from
  every refusal."
- **Trace**: n1 as above, decided yes at s1. Resend of the same map: gate
  reads `answers[n1]`, computes the digest of the record's content → equal
  → `:recorded`, ack `{:yes s1}`, nothing written, `$$clock` unchanged.
  Other content under n1 (a second client, or a rebuilt map with a new
  `:claimed-when`): digest differs → `:taken`, ack `{:no :name-taken}`,
  record untouched. Offer under n1 with `:layer :alice-hand`: parse's tag
  check → face refusal, nothing read, nothing written. Lookup: `foreign-
  select-one [(keypath :alice :answers n1)]` → the record; nil → no answer
  yet. A refused n2 (`:permission-revoked` at s2) resent after a re-grant:
  `:recorded` → the same no (E1 N3 × resend).
- **FT**: a resend racing a replay, per "Replays". **Race**: two contents
  at once, per Production readiness. **Flaws**: `lookup` required a digest
  (F13). **Verdict**: PASS after F13.

### OP4. Replace and retract within a chain
- **Source**: "A replace names the unreplaced fact of the same layer,
  entity and key, and a fact is replaced once ... A retract is a new fact,
  undoable by another new fact; time travel shows it ... it passes the gate
  like any act and never erases a value."
- **Trace**: n2 replaces `[n1 0]` on (:e0 :note): heads read `[:e0 :note
  [n1 0]]` → s1 (present) → replaceable; stamp s2 ≥ s1 + 1; writes `NONE>`
  on `[:e0 :note [n1 0]]`, put `[:e0 :note [n2 0]]` = s2. n3 replaces
  `[n1 0]` again → heads row absent → `:stale-replaces`, recorded, stamp
  s3, nothing else written. n4 with two facts both replacing `[n2 0]` →
  the count check → `:stale-replaces`. n5 replaces `[n2 0]` with `:v nil`
  → a retract, head `[n5 0]`; n6 replaces `[n5 0]` with `"a"` → the undo
  (P13). A replace naming a fact in `:alice-hand` from `:alice`: the key is
  looked up in `:alice`'s heads → absent → stale (the model filters by
  layer). A replace naming a fact of the same act: not admitted yet →
  absent → stale (the model: not in `all-facts`).
- **FT**: the two heads writes and the record are one group. **Race**:
  first decided wins. **Flaws**: none found, with reasoning: the one seek
  on `[e k r]` encodes `replaceable?`'s three conditions. **Verdict**: PASS.

### OP5. Make a layer
- **Source**: "A layer's first facts give its kind; its owner (one-owner)
  ... its class fact ... its grain (per value); and its root permission ...
  Layers, people, entities, keys and tools are data, never code ... An
  offer racing the layer's making: refused as data (no class fact yet)."
- **Trace**: the operator offers n0 into `:alice`, `:class :by-layer`,
  facts `:kind :personal`, `:owner :alice`, `:class :by-layer`,
  `:lock-grain :per-value`, and the grant `[:alice :alice :alice]`. Tag:
  `:kind`/`:owner`/`:permission` are not placed keys → tag class
  `:by-layer` = the offer's. Settings nil → class in force = the act's own
  `:class` fact (P10) → no mismatch; operator exempt; yes at s0; settings
  and the permission row projected. An offer racing it (arrives first at
  T0): settings nil and no class fact in the act → `:no-such-layer`,
  recorded (the offerer makes a new name). A second making act carrying
  `:owner :bob` later: the plan admitted it and overwrote the owner, which
  would change every later wrap (stage 2, owner required) — **flaw** → F8
  refuses `:kind`/`:owner` on a made layer (`:layer-already-made`).
- **FT**: one group. **Race**: covered. **Verdict**: PASS after F8.

### OP6. Grant and revoke a permission
- **Source**: "A permission is a fact in the layer it governs ... A revoke
  is a fact in the permission's layer, standing on the permission fact ...
  Once admitted, every later decision of that gate citing the permission is
  refused, revoked. Decisions before it stand ... A revocation and the
  writes it governs are ordered by one gate."
- **Trace**: grant `[:alice :alice :alice]` in `:alice` (seed). Alice's n7
  cites it → yes. Operator revokes it: n8 in `:alice`, fact `{:k :revoke :v
  {:permission pid}}`, `:stood-on {grant-fid s0}`; row present and
  unrevoked → yes at s8, row `:revoked` = `[n8 0]`. Alice's n9 citing it,
  decided after n8 on T0 → `:permission-revoked`. n7 stands. A second
  revoke → `:stale-revoke` (P8, O6). Revoking `[:alice :alice :alice-hand]`
  by an act in `:alice`: no row in `:alice` → `:stale-revoke`; the client
  must place it in `:alice-hand`. A write into `:alice` citing `[:alice
  :alice :alice-hand]` → `:permission-from-another-layer` (the model's
  D case); citing `[:alice :group :alice-hand]` → `:permission-does-not-
  cover-this` (layer differs), the model's order. Ordering: revoke and
  writes on one task, in depot order.
- **FT**: one group. **Race**: n8 and n9 in one batch: depot order decides,
  reads agree. **Verdict**: PASS.

### OP7. Re-class a layer
- **Source**: "The re-class is a fact placed on the layer's home task and
  ordered there with the layer's writes ... Every write decided before it
  stays in the stream store; every stream offer decided after it is refused,
  class mismatch ... Values written after it keep their layer kind's lock
  placement ... Later names of the layer are tagged by entity."
- **Trace**: operator n10 into `:alice`, name tag class nil (the only fact
  is `:class`), offer `:class :by-layer` (current) → no mismatch → yes,
  `:settings :class` = `:by-entity`. Alice's n11 tagged `:by-layer` →
  `:class-mismatch`, recorded; she makes n12 tagged `:by-entity`, which the
  client routes to the micro depot (stage 3); if it reaches `*offers`
  anyway → `:wrong-gate`, face. A grain switch n13 (tag nil) after the
  re-class → still decided here (the plan's choice; the model's micro gate
  would): the checks are the same `refusal`, the stamp from T0's clock; D6's
  hazard (a stale grain view on the micro gate admits at the wrong grain)
  becomes real for re-classed layers — the plan flagged it for phase 8 but
  not as a rig choice → F9 names it P16 with the consequence, and stage 3
  may move it. A re-class back (`:class :by-layer` when in force is
  `:by-entity`): the plan's check would admit it; IMPLICIT_SPEC E3 L3 says
  "until one is made, the offer is refused as data" → F8 adds
  `:unsupported-reclass`.
- **FT**: one group. **Race**: ordered on T0. **Verdict**: PASS after F8,
  F9.

### OP8. Switch lock grain
- **Source**: "A setting on the layer, a fact the person can change ... It
  affects only values written after it ... For a layer placed by layer the
  switch sits on the home task with the layer's writes, so every write is
  decided with the setting in force."
- **Trace**: Alice's n14 into `:alice`, tag nil, permission `[:alice :alice
  :alice]`, fact `:lock-grain :per-act` → yes, `:settings :grain` =
  `:per-act`; the next act sees it (stage 2 reads the grain at admission);
  a switch back is the same shape. On a shared layer: stage 3.
- **FT**: one group. **Race**: ordered on T0. **Verdict**: PASS.

### RD1. The answer to an offer, by name plus layer
- **Source**: I-G4 "on the layer's home task when the layer is placed by
  layer ... One read when the name's tag gives layer and class ... Never a
  scan."
- **Trace**: `[(keypath :alice :answers n1)]` routed by `:alice` → T0; the
  record or nil. Store-placed names (tag nil) are on the home too in this
  stage; the name row is stage 3's. **Verdict**: PASS.

### RD10. A layer's settings; a permission's state
- **Trace**: `[(keypath :alice :settings)]`; `[(keypath :alice :permissions
  pid)]`. Both plain values. **Verdict**: PASS.

### I-G1. A gate never throws
- **Source**: "A gate never throws on an offer. Every refusal is data with
  a reason. An exception in topology code restarts the worker."
- **Trace**: trace 4 above. **Verdict**: PASS after F6, F7.

### I-G2. Every gate write idempotent under replay
- **Source**: "A replayed offer, finished or not, gets the same answer, the
  same stamp and the same admitted facts as its first decision; nothing is
  admitted twice, no stamp is given twice."
- **Trace**: trace 3 above; every write a set. **Verdict**: PASS after F13
  (the test).

### I-G3. Resend and take the answer from the record
- **Trace**: `offer-until-answered!` (plan 838-840): on an exception,
  lookup, then the same map again, bounded. **Verdict**: PASS after F13.

### I-G4. Answer by name plus layer, never a scan
- **Trace**: RD1. **Verdict**: PASS.

### I-G5. Order of the gate's checks
- **Source**: "First, the name's tag ... Then a record under this name with
  another digest ... Then a record with the same digest ... Otherwise the
  gate decides, and the first failing check gives the reason, in this
  order: a fact outside the act's layer; a class that differs ...; a
  permission that does not cover the write ...; a permission kept in
  another layer; a permission that does not exist; a revoked permission; a
  stale or doubled replace ... The operator and the store's own steps skip
  the four permission checks ... where [the rig's own refusals] sit in this
  order is open (O1)."
- **Trace**: plan 396-409 lists the order; the rig's reasons placed by P7;
  F8 places the two new ones after `:stale-revoke`. The face refusals
  (malformed, reserved scheme, wrong gate, tag mismatch, and after F6
  `:reserved-who`, `:gate-error`) precede the record read, as O4 allows.
  `exempt?`: the operator only, after F6 (`:store` refused from the client
  depot in this stage). **Verdict**: PASS after F6, F8.

### I-G6. Acts whole or not at all, one stamp
- **Trace**: one reason for the act; one event; all rows share the record's
  stamp. **Verdict**: PASS.

### I-G7. Names random, tagged; reuse refused by digest; derived schemes
- **Trace**: `make-name` uses `ops/random-uuid7` client-side (unique-ids.md:
  client-side for a stream consumer); the tag is in the name; `:crossing`
  and `:landing` reserved and refused from offerers (`:reserved-scheme`);
  the digest check. Two offers with one uuid under different layer tags are
  *different* names in the rig (the name is the whole vector) where the
  model's plain-string name would be mis-tagged: phase 8 must translate a
  model reuse into the same rig name, not the same uuid; noted for phase 8,
  not a plan defect. **Verdict**: PASS.

### I-G8. The envelope; unknown parts refused
- **Trace**: P5, P7; `:malformed` unrecorded. **Verdict**: PASS after F6.

### I-G9. Per task: the log, answers by name, the task's stamp
- **Trace**: trace 1; `$$clock`. **Verdict**: PASS after F12.

### I-O1. Stream gate for one-owner layers; placed by layer; N fixed; class
checked; re-class
- **Trace**: OP1, OP7. **Verdict**: PASS after F8, F9.

### I-O2. Clock promises
- **Trace**: trace 2. **Verdict**: PASS after F10 (as a rig choice, stated).

### I-O4. Order between the stores only through stood-on
- **Trace**: the stream side carries stamps for stood-on facts in the other
  store (P9), a read stamped in the checking store; no check is atomic with
  the other store. **Verdict**: PASS.

### I-P1. Every offer by a person names a permission; in its layer; a gate
checks only its own layers' permissions
- **Trace**: OP6; `:permission nil` for a person → `:permission-does-not-
  cover-this` (the model's outcome). **Verdict**: PASS.

### I-P3. Owner derivable from author, layer and permission
- **Trace**: the record keeps `:who`, `:permission`; the layer is the key.
  **Verdict**: PASS.

### I-P4. Operator trusted, no signing
- **Trace**: `:who :operator` exempt; no slot. **Verdict**: PASS.

### I-P5. A pick that decides how something is written sticks
- **Trace**: the class in force at decision; grain read at admission (stage
  2); earlier facts stay where placed after a re-class. **Verdict**: PASS.

### RQ 1-4 (what Rama showed)
- **Trace**: RQ 1 (one event, one group): no partitioner. RQ 2 (completed
  records replay): the record answers. RQ 3 (a throw is fatal): trace 4.
  RQ 4 (the client can see an error for an offer that went in):
  `offer-until-answered!`. **Verdict**: PASS after F6, F7, F13.

### E1. An offer's name and its answer record — N0 (unused)
- first offer admitted / refused: recorded with a stamp; RD1 yes or no;
  RD1x name taken; gate clock ≥ s. Tag mismatch: nothing recorded, ack
  carries it (O4). **Verdict**: PASS.

### E1 — N1 (in flight)
- resend of the same content: one decision (depot order, uncommitted
  visibility); other content: first decided holds the name; tag mismatch:
  unaffected; crash mid-decision: one answer, one stamp. **Verdict**: PASS.

### E1 — N2 (decided yes)
- resend: same yes and stamp, no second admission; other content: name
  taken, unrecorded; tag mismatch: face; crash, replay of the finished
  record: same yes and stamp, no new stamp (F13 tests it). **Verdict**: PASS
  after F13.

### E1 — N3 (decided no)
- resend: the same no even when the cause has gone; other content: name
  taken; tag mismatch: face; crash, replay: the same no, no stamp.
  **Verdict**: PASS.

### E2. A chain — gate-side rows (C0 new fact; C0 replace of nothing; C1
new fact without replace; C1 replace of the head; C3 replace of a replaced
fact; C1 two facts of one act replacing the head; C1 retract)
- **Trace**: OP4; C1 × new fact without replace makes two heads and the
  latest by stamp is the chain head (P13, O5); C0 × replace of nothing →
  stale; C3 → stale; two replaces → stale (doubled); retract → `:v nil`
  replacing fact, undo replaces it. **Verdict**: PASS.

### E3. A layer — L0 × offer; L0 × make; L1 × offer; L1 × grain switch; L2
× switch back; L1 × re-class; L3 × re-class back
- **Trace**: OP5, OP8, OP7. L3 × re-class back: refused after F8.
  **Verdict**: PASS after F8.

### E4. A permission — P0 × write citing it; P0 × grant; P1 × write in its
layer; P1 × write into another layer citing it; P1 × revoke; P2 × write; P2
× revoke again; P1 (session root) × revoke
- **Trace**: OP6; P0 × write → `:no-permission`; P0 × revoke →
  `:stale-revoke` (O6 pick); P2 × revoke again → `:stale-revoke`; no
  cascade (P8). **Verdict**: PASS.

### Anti-pattern check
- No FT check was skipped; no race check was skipped; "tests are single-
  threaded" was never used as a reason.

## Self-consistency check

Re-read of every entry above for "a gap", "doesn't fully solve", "not
ideal", "a tradeoff": each such phrase sits in a FAIL item with a numbered
fix (F1-F14), never in a PASS. The one "known not to be optimal" (one
layer's throughput is one task's thread) is ruled (ruling 2's re-class) and
is not a check the template fails. The verdict below is consistent with
the items.

## Verdict, and the fixes applied to `PLAN-stream-store.md`

**minor-fail.** Each failure is a localized edit; the depot, the topology,
the partitioning, the decision order and the two PStates stand. The fixes,
applied after the verdict:

- **F1** `:log` entry's row vector subindexed (size tracking off); read by
  `ALL`; written by one whole-vector `termval` if accepted, else per-index
  `termval`s in the same event; client `facts` uses `foreign-select`.
- **F2** `:stood-on` off the answer record into its own subindexed map by
  name (inner map subindexed); writes per act + |stood-on|.
- **F3** `:subjects` capped by the parser (256; over → malformed,
  unrecorded), rig choice.
- **F4** read `[(keypath *layer :settings)]`, not the whole entry; record
  first, then settings, clock, permission, heads only if undecided; the
  partitioning table recounted with the clock seek (weighted 3.91, flat).
- **F5** (folded into F4) the wall clock read is not a seek; said so.
- **F6** parser totality: depth bound 32, guard catches `Throwable`,
  `(into [] ...)` not `vec`, `long` in Long positions, `:who :store`
  refused on its face (`:reserved-who`), a caught failure in `decide` is an
  unrecorded `:gate-error`.
- **F7** heads reads by `loop<-` only; `ops/explode` struck; `*perm` and
  `*heads` bound nil on the other branches.
- **F8** two recorded refusals added after `:stale-revoke`:
  `:layer-already-made` (a `:kind` or `:owner` fact on a made layer) and
  `:unsupported-reclass` (`:class :by-layer` when in force is `:by-entity`).
- **F9** rig choice P16: nil-tagged acts of a one-owner layer stay with
  the stream gate after a re-class, D6 consequence named, stage 3 may move
  them; the client routes nil-tagged names by the layer's kind.
- **F10** P9 sharpened: which stood-on stamps the clock covers regardless,
  which rest on the carried value, the audit copy, the kept store's read.
- **F11** P6 sharpened: the client's secret is a convenience (resend is the
  road without it); the record's keyed fingerprint outlives a forget; the
  depot retains plaintext (O2, stage 2).
- **F12** "What later stages consume": the log is the pair by name;
  admission order = stamp order per task; rebuild by merge (2 seeks +
  iteration per layer); purge by `[name idx]` one seek; every index kept in
  `$$layers` is written from this gate's event (PState ownership).
- **F13** tests: the completed-record replay; `lookup` with a nil digest;
  deep nesting and a subvec in the property test; `:who :store` in the IPC
  test.
- **F14** "could not settle": the build's first check named as a
  `create-test-pstate` with the exact schema and the listed operations;
  the fallbacks.
