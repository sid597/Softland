# Implementation Validation — stage 5b, "reads, the rest"

<!-- Phase 4 of the rama skill (phase-4-impl-validate.md, template
artifact-impl-validation.md), done in the build session, 26 September 2026,
by the builder (Claude Opus 5.5), adversarially, against PLAN-reads-rest.md
(validated, eighteen fixes) and SPEC.md phase 5. Source at the commits after
dab59795 on rig-build-reads-rest: src/rig/store/shared_reads.clj (new),
standing.clj (new), and the changes to reads.clj, read_exit.clj, gate.clj,
micro.clj, module.clj. Line numbers are those files' as validated. Default
verdict major-fail until each check is traced. -->

Scope reviewed: every dataflow op and query topology of `rig.store.shared-reads`,
the changed parts of `rig.store.reads` (index writes, purge, page step, the two
read queries' branch, the index-ops source's two new ops, `dying-rows>`,
`person-page>`), micro.clj's schema merge, block 2d's fork, the forget's
`:purge` writes, block 2a's route and the third source, the gate's clause, and
the client code (`rig.store.standing`, `read-exit`'s additions).

## Redundant conditionals

- `index-read>` (shared_reads.clj 733) and `tail-read>` (745) branch on
  `*store` with the same select in both branches, the PState differing. The
  check asks for one operation with the variable used directly; a PState
  reference cannot be a runtime variable in Rama dataflow
  (`this-module-pobject-task-global` is bound when the topology is built, in
  `<<with-substitutions`), so the branch is the only form. Not redundant.
- `open-one>` (721): the two branches call different ops (`open-entry>`,
  `reads/open-row-with>`). Not redundant.
- `micro-ops>` (1240) dispatches on the op to five different ops. Not
  redundant.
- PASS.

## Consecutive keypath

Every path writes one `keypath` with all its keys, e.g. `(keypath *L :ix-ek
*addr)` (1075-1122), `(keypath *layer :answers *cnm :session)` (entry-ids>,
standing-open>). No `(keypath a) (keypath b)` pair. PASS.

## Select-compute-transform

The only read-then-write pairs are the guards (`guarded-write>` 953: read the
fact's `:ix-ek`, then write the page's entries; `purge-fact>` 879: read
`:ix-id`, `:ix-ek`, `:ix-of`, then tombstones and deletes). Each writes values
computed from other data (the log, the date), not a function of the value read,
so no aggregator applies. PASS.

## Unnecessary nil->val

None is used. PASS.

## :allow-yield?

Traced every range over a subindexed structure:
- `index-read>` 733: pages of the read exit's doubling loop, up to 16 × (limit
  + 1) entries: `{:allow-yield? true}` on both selects. PASS.
- `put-page>` 984: the entity page (`MAP-KEYS`, at most 64): bounded, none;
  each entity's answers (up to 2,048) and each act's rows: yield on. PASS.
- `layer-page>` 1065 (sweep and person pages, up to 512): yield on. PASS.
- `prefix-entries>` 1578, `entry-ids>` 1622, `task-layers`: yield on. PASS.
- `line-value>`, `tail-read>`, `sweep-start>`, `next-layer>`: one entry
  (`{:max-amt 1}`). None needed. PASS.
- reads.clj `person-page>` 1259: up to 256 answer records of one layer and
  their rows, in the gate topology's `*index-ops` event, **without** yield, by
  the read exit's D3 (a page reads and writes in one atomic event so no forget
  lands between). Bounded by the page. PASS.

## Non-subindexed collections without size limits

- `:ix-of` sets (a fact's value-index addresses): one kind indexed by value
  tonight, so at most one address per fact, enforced by the hints. PASS.
- `$$micro-task :rebuild`: one fixed-keys row per task. PASS.
- Every other new collection is subindexed: the six `$$micro [L]` index maps,
  `$$layers [L :ix-s]`, `$$micro-task :layers`. PASS.

## Stream topology idempotency

The gate topology gains two `*index-ops` ops (reads.clj 1423-1444):
- `:replay-forget`: reads the record, row, open result and `:ix-of`, writes
  `replay-writes` through the three existing blocks: `termval`s and `NONE>`s at
  addresses computed from the row. A retried event recomputes the same
  lists. PASS.
- `:person-purge`: `(|direct task)`, then `person-page>`, whose only writes are
  `purge>`'s (termval tombstones, NONE deletes) per dying value. A retry after
  a partial commit recomputes the page from the same cursor; a value already
  purged is purged again to the same tombstone (its date is `wrap-closed`
  over the person entries, which do not change). No IDs made in the
  topology; no depot appends. PASS.
The micro topology is a microbatch topology (exactly once per batch). PASS.

## Partial failure in stream topologies

- `:person-purge` writes on one task after one hop (its `|direct`), in one
  event on that task; the ack waits for it. A failure retries the record
  whole. PASS.
- The value forget's purge (unchanged, wave 1: module.clj's decision group)
  and the person fan-out's purge (unchanged) are wave 1's.

## Single depot append per client operation

- `read!`, `subscribe!`, `deliver!`, `unsubscribe!`, `close-entry!`,
  `close-session!` (the close act): one act each, one append, named before it
  is sent and retried by name. PASS.
- `drop-reads!` (read_exit.clj 224): one forget act per dropped entry fact, by
  design (phase 2's rule that a lock control fact is its act's one fact; every
  forget a fact). A crash between two appends leaves some entries forgotten
  and some not; `resume-drops!` (255) finds the drop from the record and runs
  it again, and `entry-ids` lists only what is not yet erased, so the
  procedure converges. Each append is one operation with its own idempotent
  name. PASS (the multi-act procedure is the plan's, F4 and F11).
- `restore!`, `rebuild-micro!`, `purge-person!`: operator procedures of many
  idempotent pages, each resumable by running it again. PASS.

## Application-state caches survive restart

- The standing handle (client memory: `:scan`, `:line`, `:so-far`): its
  durable source is the entry's lines in the working layer; the rebuild path
  after a crash is `sg/close-session!` → `standing-open` (1595) and
  `standing-close` (1662), which close the entry `:crash` from the record. A
  new door does not resume a delta (it closes and reopens), as the plan says.
  PASS.
- No TaskGlobal. PASS.

## No reimplementation of built-in operations

`reads/task-count` wraps `ModuleInstanceInfo.getNumTasks` (interop kept out of
dataflow, the skill's pattern); `ops/current-microbatch-id`,
`ops/module-instance-info`, `ops/explode`, `ops/explode-map` are used where
they apply. PASS.

## Scenario traces (the spec's clauses, through the code)

**A. A shared pattern read as of a settled frontier (SPEC phase 5; R5 of the
rig; FRR8).** `read!` → `check-call` (read_exit.clj 58, moment maps accepted)
→ the `read-pattern` query, `(|hash :g)` → `parse-pattern` → `$$layers [:g
:settings]` is nil → `shared-layer?` (reads.clj 485) → `invoke-query
"shared-read-pattern"` (reads.clj 1544) on the same task → `shared-pattern>`
(shared_reads.clj 1288): this task's frontier Ft; `moment` → F = min(asked,
Ft), a stamp refused `:moment-kind`; `micro-visibility>` (1273): the last
settings version below F + 1 (one tail read) and the reader's member batch
≤ F; `micro-pp` sets `:by :batch`; `loop-pages>` over `$$micro [:g :ix-ke]`;
`reads/page-step`'s `stamp-ok?` reads `(:batch e)` ≤ F, so no entry of a
batch above F is kept; `show-kept>` opens each through `open-entry>` (698)
from the entry and `$$persons` on this task; `shared-answer` records
`{:frontier F}` and `:max-stamp`. The entry's facts carry both
(`entry-facts`, reads.clj 968) and `entry-moments` (998) counts `:max-stamp`,
so the gate stamps the entry above what it names. Nothing is shown before
the entry's yes (the exit, unchanged). Run: RT1's race (two writers, 201
reads, 0 violations), RT2, RT12 (runs/reads-rest-try2.log). PASS.

**B. Block 2d, written in the batch that decides the act, under a retry.**
micro.clj 1558-1576: on the arrival task `kv-digests` over the plaintext
`row-wraps` opened (the text never leaves the task, F12); after
`rows-written?` the fork `(ops/explode [:rows :index])` sends one branch to
`index-block>` (858): `index-writes` (pure, total, an error flagged not
thrown), `(|hash L)`, the layer added to `:layers`, `write-index>`. A failed
attempt's writes are discarded; the retry recomputes the same lists from the
same offers, lease rows and record, except the wraps' fresh nonces, and the
row (block 2b) and its entries' lock copies come from the same attempt's
`fact-rows`, so they agree in the committed attempt. PASS.

**C. A value forget in a shared layer reaches every index in its own batch
(path 2).** The fold's `forget-effect` returns `:erased` (micro.clj 830);
`decide-envelope` puts `[:purge L nil fid stamp]` per erased value (993);
block 2a routes them to `(|hash L)` → `purge-fact>` (879): three seeks, the
tombstones dated by the forget's stamp, the ledger's date, and the value
index deleted, atomic with the lock's excision or deletion on the entity
tasks. A second forget is `{:how nil}` (no purge; the first date stays). Run:
RT6. PASS.

**D. A person forget reaches every index (path 3).** One-owner: wave 1's
fan-out purge in the forget's own event (unchanged); this stage's paged
`:person-purge` (`person-page>`, reads.clj 1259, `dying-rows>` 1228) finds the
same values and writes the same tombstones (the date `wrap-closed` gives, a
ledger-erased value skipped). Micro: `person-purge-page>` (1154) over `:ix-s`,
each live entry whose lock names the person and whose wrap is closed purged
with that date. Both are the rebuild's writes (tombstone whatever the open
step reports erased). Run: RT7, with the loop interrupted and rerun. PASS.

**E. Rebuild and restore write what the purges wrote (the invariant).**
`implied-writes` (225) is the one function behind block 2d's fact writes (via
`fact-writes`), the put page (`put-row-writes` 578) and the sweep
(`fact-imp` → `sweep-writes` 248); tombstones come from `tombstone` (129) in
all three. The ops section of a batch can run beside the offers' blocks
(microbatch.md: each `source>` its own section), so `guarded-write>` (953)
never writes a live entry over a tombstone and `guard-imp>` (968) judges a
tombstoned fact by its tombstone; `rebuild-micro!` ends with the replay of
the forgets decided since it began (a forget of a fact its put page had not
yet indexed finds nothing to purge). Run: RT8, every field of both stores
equal to the snapshot after a restore and after a second one, but for the
restore's own acts in `:people`; the pure property
`purge-writes-what-a-rebuild-would`. PASS.

**F. A standing read (R6).** `subscribe!` (standing.clj) runs the opening read
through `read-delta` → `invoke-query "read-pattern"` → `opening-answer`, then
one act (opening fact, and a delivery line when it matched), rows only on
yes. `deliver!`: `delta>` (1466) → `delta-read>` from the handle's cursor
(`scan-from`: an address, or a sealed one) to the moment's bound, `:ix-s` with
the pattern filter for `[:all]`/`[:e]`/`[:k]`, the own index otherwise;
`next-scan` (415): past the moment, after the last shown row (limit cut), or
past the last scanned address sealed (budget cut). A line only when something
matched; `:so-far` chained under the fingerprint secret (`standing-link`,
reads.clj 909). `standing-close` reads one line. Runs: RT3, RT4, RT9. PASS
after the build's repair of F2 (below).

**G. The close act and the drop (R5, default 4).** The gate accepts `:reads
:keep | :drop` (gate.clj 103); `drop-reads!` forgets the session's entries
(`entry-ids`, filtered by the act record's `:session`) one forget act each,
`:because-of` the close act; interrupted, `resume-drops!` finishes it from the
record. Run: RT5 (70 point facts, two pages, interrupted and resumed). PASS.

**H. Every refusal is data.** Every pure function catches; every query checks
its arguments before any keyed read (`reader-args`, `delta-args`,
`upkeep-args`); a malformed micro op is dropped by `micro-op` before its
`|direct`. Runs: RT13, `nothing-throws` (200 generated inputs). PASS.

## Plan conformance

Each divergence, and why it stands:

1. **Purge rule** (plan item 9 not built): wave 1's rebuild tombstones any row
   the open step reports erased and its person forget tombstones the dying
   values; this stage applies the same rule in the micro store. Both rules
   keep the plan's invariant; the plan says phase 2's forms win at the merge,
   and wave 1's tests assert its rule. PASS.
2. **One forget act per dropped entry** (F4, RR14, FRR9): phase 2's gate
   refuses an act with a lock control fact beside another fact. The plan's
   own [build checks] foresaw a bound. First-record question for Sid. PASS.
3. **The entry names its session** (FRR10, first-record): the coordinator's
   finding from phase 7's validation; `entry-ids` finds a session's entries by
   their acts' records. Default 4's end to end. PASS.
4. **The shared read is a query invoked from the exit's queries**, not a
   branch calling this namespace's functions (plan, "What changes in the read
   exit's two queries", [build checks] fallback inline): `rig.store.reads`
   cannot require this namespace, which requires it; the inline fallback
   would put the shared path's dataflow into `reads.clj`, which phase 6 is
   rewriting beside this build (the brief: changes to shared files small and
   local). `invoke-query` resumes on the same task: no seek, no hop. PASS.
5. **The delta is its own query, `read-delta`**, not an `:after` argument of
   `read-pattern`, whose signature phase 6's runner and phase 8's replays
   call. Same reads and costs. PASS (the brief's rule).
6. **F2 as built:** the plan's resume at "the time part of the last address
   scanned" cannot pass one act with more facts than the scan budget (every
   fact of an act shares its stamp, so the time part never moves): RT9 found
   it (11 ticks of `:nothing-new`, the match never delivered,
   runs/reads-rest-try2.log). A budget cut now resumes past the last scanned
   address, sealed under a module lock (`seal-cursor`), which keeps F2's aim:
   nothing of an unshown fact is readable by the client. Plan wrong on
   correctness. PASS.
7. **`rebuild-micro!` guards and replay:** the plan's put guard is extended to
   sweeps, because the ops section runs beside the offers' blocks (a
   `source>` is its own section; the plan assumed block order). Correctness.
   PASS.
8. **Micro person purge over `:ix-s`**, not `:ix-kv`: rule 1 needs every dying
   entry, not only the value-indexed ones. PASS.
9. **Membership as of F** (`:members` is person → batch in the built schema,
   not a set): F7's `subselect (set-elem)` is moot; RR2 becomes membership at
   F. Built schema; better than the plan. PASS.
10. **Progress row and two queries added:** `:rebuild` carries a request id
   and a count; `micro-index-progress` and `task-layers` (leading `|direct`)
   read a task's progress and layers, since a `foreign-select-one` with a
   `:pkey` cannot name a task. Needed for the loop to know its page ran. PASS.
11. **An opening read cut by its limit is not caught up** by later deliveries
   (the plan's RR10 resume covers deltas; the opening reads the pattern's own
   index): its line and the closing mark say `:partial`. A rig choice (P5-9).
   PASS as a named choice.
12. **Block 2d is a fork**, not an `anchor>`/`<<branch`: Rama 1.6.0 refused a
   branch inside a `<<batch` block at module build (a MarkerSegment error in
   the pre-aggregation phase, runs/reads-rest-compile.txt of the first try).
   Same writes. PASS.
13. **The micro put page's row cap is 2,048**, as wave 1 halved the stream put
   page's for the cost of opening every row. PASS.

## Verdict

**minor-fail**, fixed in place. The adversarial read found, and the build
fixed before the tests settled: `loop-pages>` emitted nothing after its loop,
so every shared read answered no result and the exit's query failed as a
worker fatal (fixed: the op emits its loop's output); the plan's F2 resume
(fixed, item 6); a put page racing a forget of a fact not yet indexed (fixed:
the replay at the end of `rebuild-micro!`); a missing `:ix-place` schema field
(added, unwritten: absent means 1). No check fails after the fixes; every
divergence above is justified by correctness, by wave 1's code, or by the
brief's rule for shared files.

PHASE_VALIDATION:minor-fail
