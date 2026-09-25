# Implementation validation — stage 5a, the one-owner read exit

<!-- Phase 4 of the rama skill (phase-4-impl-validate.md, template
artifact-impl-validation.md), done inside the build session (phase-build.md
step 2), 26 September 2026, by the builder (Claude Opus 5.5, effort max) in
worktree /mnt/data/projects/Softland-rig-build-reads, branch rig-build-reads.
Default verdict major-fail. Reviewed: src/rig/store/reads.clj,
src/rig/store/read_exit.clj, and the stage's edits to module.clj and gate.clj
(git diff b31ad57e), against PLAN-read-exit.md (F1 to F12 in place), SPEC.md
phase 5 and "What Rama showed", RIG.md defaults 2 to 4 and For Sid 8 to 10,
and the brief's overrides (BUILD_NOTES-read-exit.md). Line numbers are
reads.clj's unless a file is named. The reviewer wrote the code; every PASS
below rests on a trace, and the findings the traces turned up were fixed in
the source before this artifact was finished (section "Findings"). -->

## Verdict

**minor-fail**, every failure fixed in place by a localized edit (four
findings, below). No finding needed a restructuring; the PStates, the
topologies, the queries and the exit are as planned, with the divergences
listed under "Plan conformance", each justified by a correctness issue.

## Findings (failures found by this validation, fixed in the source)

| # | check | failure | fix |
|---|---|---|---|
| V1 | stream idempotency / never throw | `index-op` normalised a put page's `:after` name with `(some-> after vec)`; `vec` hands a subvec back unchanged, and the envelope records that a subvec is refused as a PersistentVector key (F6 of phase 1). The name reaches the `:answers` range navigator at line 974 inside the gate topology, where a refusal is a throw, fatal to the worker | `(some->> after (into []))`, line 817 |
| V2 | `:allow-yield?` / cooperative multitasking | the `[:kv]` inner loop (line 1153) opens candidates with no yield point; tonight's open is pure, but phase 2's reads lock rows (1 to 3 seeks each), so a page of hundreds of candidates would hold the task | `(yield-if-overtime)` at the loop's head (line 1154); a query is read-only, so a yield there is safe (the moment was read before the first page) |
| V3 | minimise storage I/O | the ops source read `$$clock` for every record, but only put and sweep pages use it | the clock read moved into those two branches (lines 967, 1001) |
| V4 | plan conformance | the plan exports `reads/moment`; the build named it `moment-stamp` | renamed `moment` (line 358) |

Two findings of the implementing step are recorded under "Plan conformance"
(D3, D4): the rebuild pages must not yield, and a sweep must rewrite drifted
entries. Both were fixed before this pass and are traced there.

## Redundant conditionals

- Line 970 to 974 (put page) and 1006 to 1009 (sweep page): `(<<if (nil?
  *after) ... sorted-map-range-from-start ... (else>) ... sorted-map-range-from
  *after *opts)`. The branches use different navigators (from the map's
  start, or after an exclusive cursor), not one operation with a variable.
  Not redundant.
- Line 1021 to 1026: `(<<if (= *field :ix-kv) (local-select> ... :ix-of) (else>)
  (identity nil))`: one branch reads, the other does not. Not redundant.
- Lines 1076 to 1085 and 1118 to 1127 (the queries): `<<cond` over refused,
  not visible, re-classed, answered; each branch binds `*answer` from a
  different computation. Not redundant.
- **PASS.**

## Consecutive keypath

Every path uses one multi-arity keypath: `(keypath *layer :answers *nm)`,
`(keypath *layer :log *nm *idx)`, `(keypath *layer *ix *ia)`, `(keypath
*layer :ix-of *ofid)`, `(keypath *layer *field)` followed by a range
navigator (lines 972 to 1062, 1079 to 1188; module.clj 152 to 159). No
`(keypath a) (keypath b)` pair. **PASS.**

## Select-compute-transform

No accumulation: every write is a `termval` of an entry or set computed
from the offer (the gate, module.clj 152 to 159) or from a page's reads (the
ops source, 1054 to 1062), or a `NONE>`. The sweep's rewrites (line 309,
303) replace a whole entry with the one the log implies; there is no
counter or collection an aggregator could maintain. **PASS.**

## Unnecessary nil->val

No `nil->val` anywhere in the stage's code (grep: none). Absent keys read
as nil and every consumer is nil-safe (`yes-by?` line 600, `open-row` line
341, `name-idx` line 885, `purge-op-writes` line 897). **PASS.**

## :allow-yield?

- Query page reads, line 1147 `(sorted-map-range-from *pfrom *ppage)` with
  `{:allow-yield? true}` (line 1148): pages up to the scan budget (16,016 at
  the default limit). Has it. Trace of a yield between pages: the moment `*m`
  was read (line 1130 to 1132) before the first page; a fact admitted during
  the yield is stamped above the clock, so above `*m`, and `page-step` drops
  it (`stamp-ok?`, line 511); a purge during the yield leaves the entry read
  before (value) or after (tombstone), both one task's order.
- The tail read, line 1137: `{:max-amt 1}`, one entry. Correctly without.
- Point reads, lines 1100 and 1102: one key each. Correctly without; the loop
  over up to 1,000 fact ids has `yield-if-overtime` (line 1093).
- Step 6 loop (line 1174) and the `[:kv]` loop (line 1154, V2): `yield-if-overtime`.
- **The ops source, lines 972, 974, 982, 1007, 1009, 1046: range reads of up
  to 256 records, an act's rows, 512 entries, without `:allow-yield?`, and no
  `yield-if-overtime` in its loops. Deliberate, and required for
  correctness:** a page reads the log and writes what the read implies. Trace
  with a yield (the plan's sweep loop had `yield-if-overtime`): a sweep page
  of `:ix-of` reads fact f's record, row and open (value present), yields;
  phase 2's forget of f runs in the gap and deletes `:ix-of[f]` and f's
  `:ix-kv` entry (`purge-writes`); the page resumes and writes the set it
  implied before the forget, which names f's `:ix-kv` address, whose text is
  f's value. The forgotten value's text is back in the store. The same
  holds for a put page (it would write f's full entries) and for a sweep's
  rewrite (D4). Without yields each page is one atomic event: no forget can
  fall between its reads and its writes. The skill's own rule: "Only use
  yielding when the operation can tolerate interleaved modifications." The
  cost, named: a page holds the task for its length, bounded by the page
  size (the plan's estimate: 0.1 to 0.3 s for a put page, 0.5 to 0.8 s for a
  sweep page of 512); the operator's loop takes smaller pages to bound it.
- **PASS** (the ops source's absence of yields is a correctness requirement
  traced above, not a gap).

## Non-subindexed collections without size limits

- `:ix-of` values, `(set-schema String)`, not subindexed (line 134). Every
  write of one: `fact-writes` line 170, `[[fid #{kv}]]`, exactly one address
  for the one value-indexed key of a fact; `sweep-page-writes` lines 303 and
  318 write the implied set (at most that one address) or a subset of an
  existing set. Bound: one address per fact, enforced by the only writers.
- An entry's `:mark` set (row field): the parser admits only `#{:die-with-any
  :own-row}` subsets (envelope `marks`). Bounded at 2.
- **PASS.**

## Stream topology idempotency

The gate topology gains three write blocks in its decision event and a
second source.

- **Decision event (module.clj 149 to 159).** Every write is `(keypath
  *layer *ix *ia) (termval *ie)`, `(keypath *layer :ix-of *ofid) (termval
  *ias)` or `(keypath *layer *dx *da) NONE>`, at addresses `address`
  computes from the act's name, index, stamp, entity, key and value text
  (lines 96 to 105). A replay that reaches the writes (nothing committed,
  RIG phase 0 finding 2) recomputes the same stamp from the same state and
  writes the same entries; one that finds the record writes nothing
  (module.clj 104 to 108, phase 1's recorded path). No append, no counter,
  no id made in the topology.
- **The ops source (lines 955 to 1063).** Put pages `termval` implied entries
  (puts only); sweep pages delete non-implied addresses and rewrite drifted
  ones to the implied value; the test ops `termval` a tombstone, delete, or
  `termval` a checked entry. A replayed page (`:retry-mode :all-after`, line
  956) or a resent one (`read-exit/index-op!`, read_exit.clj 127 to 139)
  recomputes the same writes from the same log: a put page writes what is
  already there; a sweep page's second run finds nothing to delete. No
  `depot-partition-append!`.
- **Never throws on an offer or a record.** `index-writes` (line 181),
  `put-page-writes` (239), `sweep-page-writes` (281), `purge-writes` (202),
  `index-op` (798), `entry-moments` (772) and every helper the dataflow
  calls catch `Throwable`; an index failure in `decide*` is thrown on purpose
  (gate.clj 282) inside `decide`'s existing `try`, which answers the unrecorded
  face refusal `:gate-error` (gate.clj 305 to 314), phase 1's road. Every
  write list holds only the four field keywords and schema-valid entries
  (built from log rows plus `:fid` PersistentVector, `:stamp` Long,
  `:erased-at` Long, `:copy` Boolean), so no schema violation is reachable.
- **PASS.**

## Partial failure in stream topologies

No event of this stage crosses a partitioner: the decision event and every
ops page run on the layer's home task (depots `hash-by :layer`, module.clj
85 to 86) and commit in one group or not at all (SPEC "What Rama showed" 1).
The exit's query and its entry are two events joined by the client's order;
traced under "Plan conformance", the exit. **PASS.**

## Single depot append per client operation

- `read!` (read_exit.clj 87 to 125): one entry offer, sent by
  `client/offer-until-answered!`, which resends the same map under the same
  name after an error; no other append. A crash between the query and the
  append leaves nothing (the query wrote nothing).
- `rebuild!` (read_exit.clj 141 to 160): one append per page; each page is
  an independent idempotent operation and the rebuild is the operator's loop
  (plan F2: "driven one at a time by the operator's loop"). A crash mid-loop
  leaves a partly rebuilt layer whose re-run completes it.
- **PASS.**

## Application-state caches survive restart

No TaskGlobal, no cache. `open-double` (line 324) is a test-only hook (R3),
nil outside tests; `fp-secret` (673) is derived from a constant at load.
**PASS.**

## No reimplementation of built-in operations

`hmac`, `hex` (Java crypto), address building, the page step: none has a
Rama built-in. `page-entries` turns a returned submap into a vector for pure
code. **PASS.**

## Plan conformance

Matches the plan: no new PState; the four fields merged into `$$layers` by
one form (module.clj 72) around the row's field map (module.clj 15 to 23,
F6); entries are the row plus `:fid :stamp :erased-at :copy` (112 to 123);
the three write blocks after the heads writes in the yes branch (module.clj
149 to 159); `index-writes` from `decide*` for a yes (gate.clj 281) and
`entry-moments` in `stamp-for` (gate.clj 200, F1); the paged rebuild on
`*index-ops` (F2) with RE5 and RE6 as specified; the two queries with the
plan's signatures, leading `(|hash *layer)`, one emit, `|origin`; F3 (the
limit + 1st entry decides the mark only, `page-step` 570), F4 (a layer not
made is `:not-visible`, `visible?` 378), F5 (a `[:kv]` candidate counts only
when it opens to the pattern's value, 531 and 1153 to 1163), F7 (`:no-copy`
entries, 165; their rows read in step 6, 1183 to 1187), F8 (the scan budget,
492 to 502 and 554 to 560), F12 (`:for` decides visibility, 371 to 382; the
entry's `:who` is the reader, read_exit.clj 83); the exit's order and hooks
(read_exit.clj 106 to 125); hints as a parameter with the seed constant (34
to 41); the fingerprint as one function over the set of `[fid stamp]` pairs
under a derived secret (682 to 702); the entry's facts as FR5 to FR8 (748 to
770).

Divergences, each with its reason; none is "simpler" or "easier":

| # | divergence | why (correctness, or the brief) |
|---|---|---|
| D1 | No `src/rig/store/locks.clj`; the open step is one seam, `reads/open-row>` (346), phase 2's planned signature `[layer fid row stamp T]` and returns, passing values through | the orchestrator's brief overrides the plan's stub file, so the merge has no file to fight over |
| D2 | `:read-fp/v1` where FR12 says `:read-fp/1` (645) | `:read-fp/1` is not EDN: a keyword's name cannot start with a digit (the reader refuses it; `env/readable-keyword?` too), so every pattern line would be refused `:malformed` on its face and every pattern read hidden. First-record |
| D3 | No yields in the ops source; the plan's sweep loop has `yield-if-overtime` | traced under ":allow-yield?": a forget in a page's yield has a purged value written back; the plan's own `:ix-of` rewrite is enough to do it |
| D4 | A sweep rewrites an entry at an implied address whose content differs (309); the plan deletes it | after the put pass nothing refills a deleted implied address, so the plan's delete leaves a hole against its own end state ("exactly what the log implies"); e.g. a no-copy flag or hints change, or a stale entry at a real fact's address, would lose the fact from the id index |
| D5 | The fact id's part of an address is the name's canonical text, U+0000, and the index as 8 hex digits (88 to 94); the plan says the canonical text of the fact id | the canonical text of `[name idx]` sorts index 10 before 9, so an act's facts would come out of act order and `[:latest e k]`'s tie between two facts of one act would pick another fact than the model's chain-head (the higher index). Addresses are rebuildable; no record changes (RC4) |
| D6 | A point read of zero fact ids is refused `:bad-read` (402 to 414; read_exit.clj 41 to 46); the plan says it is "recorded as an entry with no rows" | an act with no facts is refused `:empty-act` on its face (envelope), so the plan's entry cannot exist; the read would always come back `{:refused :empty-act}`. Refused before anything is read, nothing recorded |
| D7 | A layer re-classed by entity is refused `:re-classed` on the read path (384 to 389, 1082, 1124) | plan silent; a by-entity layer's later facts are the micro store's (phase 3), so answering from `$$layers` would record `:complete` for a read that is not. Decided after visibility, so a reader who cannot see the layer learns nothing. Rig choice |
| D8 | A third test-only op, `:put` (839 to 844, 915 to 924) | T10 needs "a stale entry written into `:ix-ek` by a test-only put"; RC8 lists only `:purge` and `:drop`, and no other writer of `$$layers` exists. Built from checked parts, so it cannot violate the schema |
| D9 | `index-writes` takes an optional sixth argument, the facts' plaintext texts (193 to 199) | phase 2 seals values at the door, so the row's `:v` will not hold plaintext; the plan leaves "how the gate gets plaintext for an `:ix-kv` address" to phase 2, and without this the value index would silently stop after the merge. Tonight's 5-arity is the plan's |
| D10 | `[:kv k nil]` is refused `:bad-pattern` (460) | a retract is never indexed by value (RC11), so the read could only ever be empty; refused as outside the language. Rig choice |
| D11 | Input refusals are `:bad-read` for point reads and `:bad-pattern` for pattern reads (moment, limit, list) | the plan names the refusals but not which reason a malformed moment gets; not recorded, rig choice |
| D12 | The exit refuses a `:person` reader whose `:for` is another person (read_exit.clj 55) | F12: `:for` is "the reader itself for :person"; without the check a person could name another as `:for` and read their private layers |
| D13 | Rebuild pages open rows as of the task's clock (967, 1001, 991, 1022) | plan silent on the moment of a rebuild's open; "now" is the clock |

`reads/declare-index-ops-source!` (937) installs the ops source with a
second `<<sources` call, the plan's preferred form ([build checks], probed
by this build).

**PASS** after V1 to V4, with D1 to D13 each justified above.

## Against the spec, beyond the plan

- **Phase 5, "one read entry line per pattern read carrying pattern, moment,
  role and a keyed fingerprint of what matched, with a complete-or-partial
  mark".** `entry-facts` 765 to 769 carries each; the fingerprint is keyed
  (682 to 702); the mark is `pattern-answer` 724. Traced for an empty read:
  `[:e :nobody]` → no kept entries → rows `[]`, matched `[]`, mark
  `:complete`, fingerprint of the empty set → one line with `:count 0`.
- **"Every index over values rebuildable from the log or purgeable by value
  id, so a forget reaches it."** `:ix-kv` (values in addresses), `:ix-ek` and
  `:ix-ke` (value copies) and `:ix-of` (value-bearing addresses): each
  purged by `purge-writes` (202 to 224) from the value id alone, and each
  rebuilt by the ops source. A person forget reaching `:ix-kv` is phase 2's
  obligation, carried in RIG.md "Next" and the brief (it calls
  `purge-writes` per value); reads never match such a value meanwhile (F5).
- **"A gate never throws on an offer; every refusal is data."** Traced under
  idempotency. The queries too (a query exception is fatal to the worker,
  probed): every dataflow step calls a total function or a `local-select>`
  on a validated key.
- **Default 2 (hybrid stamps) and ruling 4.** F1: an entry decided on
  another task is stamped at least its moment + 1 (gate.clj 195 to 200,
  `entry-moments` 772 to 788), and its moment is at least every matched
  stamp (the query keeps only stamps ≤ m), so the entry is after all it
  names. Past R16's bound (a task clock pushed to 2^62 by carried stamps) a
  moment is not counted, the same limit phase 1 accepts for carried stamps.
- **Default 4 (one exit; agent reads recorded).** `read!` shows rows only on
  the entry's `:yes` (read_exit.clj 118 to 123); any working layer, the agent
  session layer included.
- **Ruling 9 (visibility).** `visible?` for `:for`; a private layer and a
  missing one answer alike (F4).

PHASE_VALIDATION:minor-fail
