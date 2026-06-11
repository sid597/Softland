# Implementation Validation

<!-- Phase 4. Fill in after Phase 3 produces the module source. -->

> **RETROSPECTIVE MODE.** Module under validation: `src/app/server/rama/dogfood/llm.clj`
> (2126 lines, read in full), built BEFORE the skill existed. Validated adversarially against
> `PLAN.md` (retro, code-blind) AND `IMPLICIT_SPEC.md`, under production rules: at-least-once
> stream replay, transaction scope between partitioners, worker restart without depot replay,
> retry-safe side effects. Executor-side helper fns (Claude/Codex process spawning, streaming,
> adapters) are part of the module file and in scope. A fail verdict does not trigger a fix
> loop; this artifact and its verdict ARE the retro result.

Semantics relied on (cited):
- `references/stream.md:9` — "A stream topology can retry a record even after all PState writes
  have completed and committed … All writes for that record execute again."
- `references/stream.md:96` — streaming batches on different tasks (across partitioners) commit
  independently; on retry the entire event replays from `source>`; committed writes on other
  tasks are NOT rolled back.
- `references/core-concepts.md:10` — stream transaction scope = between partitioners (per-task
  atomic); `:ack` blocks until PState updates visible; `:append-ack` does not.
- `references/stream.md:87-88` — `:individual` / `:all-after` are both at-least-once.
- `SKILL.md` — "Worker restart does NOT replay depot history"; "Never trade fault tolerance for
  code simplicity"; subindex collections that grow beyond ~100 elements.
- `references/core-concepts.md:34` — `foreign-select-one` is a built-in client operation.

---

## Redundant conditionals
<!-- if every branch of an <<if, <<cond, or <<switch does the same operation with only a variable differing, replace with a single operation using that variable directly -->

**Check:** if every branch of an `<<if`, `<<cond`, or `<<switch` does the same operation with
only a variable differing, replace with a single operation using that variable directly.

Dataflow conditionals in the module: `<<if` at llm.clj:1310 (decision accepted), 1344
(grantable claim), 1358 (known run, obs), 1386 (approval materialized), 1391 (indexable item),
1403 (known run, control), 1418 (control has approval), 1422 (terminal run). Traced each: every
`<<if` has a single then-branch performing work that the else case must not perform (writes
gated on acceptance/grant/known-run). No `<<cond`/`<<switch` appears in dataflow code. The
repeated status-promotion logic across `case` arms in `apply-observation-effect`
(llm.clj:921-1072) is plain Clojure, not dataflow branching, and the arms differ in their other
operations.

**PASS.**

## Consecutive keypath
<!-- (keypath *a) (keypath *b) → (keypath *a *b) -->

**Check:** `(keypath *a) (keypath *b)` → `(keypath *a *b)`.

Violations found by tracing every `local-transform>` path:
- llm.clj:1327 `[(keypath *thread-id) (keypath *run-id) (termval *run-summary)]`
- llm.clj:1335 `[(keypath *executor-task-id) (keypath *run-id) (termval *pending-entry)]`
- llm.clj:1352 `[(keypath *executor-task-id) (keypath *run-id) NONE>]`
- llm.clj:1383 `[(keypath *thread-id) (keypath *run-id) (termval *run-summary)]`
- llm.clj:1384 `[(keypath *thread-id) (keypath *run-id) (termval *items-by-id)]`
- llm.clj:1424 `[(keypath *executor-task-id) (keypath *run-id) NONE>]`

All six should be single `(keypath *a *b)` navigators.

**FAIL** (each fixable as a one-line edit).

## Select-compute-transform
<!-- local-select> followed by computation followed by local-transform> with termval — replace with +compound and an aggregator when possible -->

**Check:** `local-select>` followed by computation followed by `local-transform>` with
`termval` — replace with `+compound` and an aggregator when possible.

This pattern is the module's universal write strategy, and because nothing is subindexed it
operates on entire monolithic values:

- Claim: select full run row (1342) → `grant-claim` → termval full row + full view + full
  detail (1348-1350).
- Observation: select full run row (1357) → `fold-observation` (1359) → **eight** full-value
  termvals on the run task (1369-1376: row, view, detail, items-by-id, raw-response-items,
  tool-calls, approvals, token-usage), then hop and select full thread row + full cost rollup
  (1378-1379) → **four** more full-value termvals (1382-1385).
- Control: select full run row (1402) → `fold-control` (1404) → five full-value termvals
  (1411-1415).
- Request: select thread row (1316) → `upsert-thread-row` (1325) → termval (1326).

Runtime trace, one `:codex/item-completed` observation on a run with 1,000 items: the event
deserializes the entire run row (containing 1,000 items, 1,000+ raw provider payloads in
`:raw-response-items`, all tool calls, approvals, controls), folds one item, then re-serializes
and rewrites that entire structure ~8 times on the run task and 4 derived structures on the
thread task — roughly 13 full-value writes whose size grows linearly with run length. Per the
spec this happens 10–100 times/sec per active run, giving O(n²) cumulative write volume per
run. The plan's design (subindexed inner maps, targeted single-key writes, hot path zero-hop)
avoids this entirely; aggregator-based or targeted-path writes are possible for every one of
these folds.

**FAIL** (requires the check-6 schema restructuring; not a line edit).

## Unnecessary nil->val
<!-- navigators handle nil as empty collection — do not add nil->val unless the next navigator requires a non-nil value (e.g., (nil->val 0) before (term inc)) -->

**Check:** do not add `nil->val` unless the next navigator requires a non-nil value.

Grep of the module: no `nil->val` anywhere. Nil-handling is done in Clojure helper fns with
`or` (e.g. `upsert-thread-row` llm.clj:477-485, `cost-rollup-for-thread` llm.clj:570-577),
which is outside this check's scope.

**PASS** (vacuous).

## :allow-yield?
<!-- local-select> or select> that iterates over a subindexed structure on a non-mirror PState should include {:allow-yield? true} whenever the iteration count can exceed ~100 entries. -->

**Check:** `local-select>`/`select>` iterating a subindexed structure with possible >~100
entries must include `{:allow-yield? true}`.

There are NO subindexed structures in the module (see next check) and no range navigators —
every `local-select>` (llm.clj:1316, 1342, 1357, 1378, 1379, 1395, 1402) is a whole-value point
read. There is therefore no read to which `:allow-yield?` can apply. This check passes only as
a vacuous consequence of the schema design that fails the next check; within this check's own
scope there is no violation. (Noted: `drain-observation-buffer` llm.clj:1082-1091 is an
unbounded in-memory loop, but it iterates a deserialized map, not a PState read, so it is
outside this check; its cost is part of the check-6/check-3 monolithic-row failure.)

**PASS** (vacuous).

## Non-subindexed collections without size limits
<!-- for every write to a non-subindexed inner collection (map, set, vector), verify the application explicitly enforces a maximum size. -->

**Check:** every write to a non-subindexed inner collection must have an explicitly enforced
maximum size; otherwise it must be subindexed.

Every PState is declared `{String Object}` or `{String String}` (llm.clj:1282-1302) — no
`map-schema`, no `{:subindex? true}` anywhere. Tracing every inner collection write:

| Collection | Writer | Bound? |
|---|---|---|
| run row `:items-by-id` + `:item-order` | `add-item` llm.clj:808-814 | NO — spec: hundreds–thousands of items per run |
| run row `:raw-response-items` | `add-raw-response-item` llm.clj:880-882 — one entry per observation, ALL types | NO — unbounded |
| run row `:obs-buffer` | `fold-observation` llm.clj:1120 | NO — unbounded under reordering/stalled gap |
| run row `:tool-calls-by-id` | llm.clj:941-945, 999-1015 | NO |
| run row `:approvals-by-id` / `:approvals-pending` | `add-approval` llm.clj:829-834 | NO |
| run row `:controls-by-id` + `:control-order` | `record-control` llm.clj:1161-1165 | NO |
| run row `:patch-proposals-by-id` + order | `add-patch-proposal` llm.clj:874-878 | NO |
| run row `:compactions`, `:steers` | llm.clj:1042-1048, 1231-1247 | NO |
| run row `:observation-errors` | `add-observation-error` llm.clj:685-693 | YES — `append-bounded` cap 50 (the only bounded collection in the module) |
| `$$llm-threads` `:turn-run/ids` vector | `upsert-thread-row` llm.clj:495 | NO — grows per run forever; `conj-distinct` is also an O(n) scan per call |
| `$$llm-turn-runs-by-thread` inner map | llm.clj:1327, 1383 | NO |
| `$$llm-items-by-thread` `{run-id items-by-id}` | llm.clj:1384 | NO — both levels unbounded |
| `$$llm-cost-by-thread` `:runs` | `cost-rollup-for-thread` llm.clj:574-575 | NO — unbounded runs/thread |
| `$$llm-pending-by-task` inner map | llm.clj:1335 | NO — backlogged lane unbounded |
| `$$llm-views` / `$$projection-run-detail` values | embed full item vectors (llm.clj:522) | NO |

The spec's own scale statements ("Hundreds to thousands of observations per run", "a thread's
run list grows unbounded", "per-thread item history unbounded") make these guaranteed, not
pathological. Fix requires changing PState schemas to subindexed `map-schema`s and rewriting
every fold from whole-row read-modify-write to targeted path writes — a restructuring of the
module's core.

**FAIL — major.**

## Stream topology idempotency
<!-- for each stream topology, trace through what happens if any event retries. -->

**Check:** trace every write and side effect under record retry.

**(a) TurnRunRequest retry clobbers live runs — catastrophic.** llm.clj:1329
`(local-transform> [(keypath *run-id) (termval *bound-run-row)] $$llm-turn-runs)` writes a
freshly-built `initial-turn-run-row` (`:status :pending`, `:last-seq -1`, empty items/buffer,
nil claim — llm.clj:385-435) with NO existence/status guard. Trace per stream.md:9: attempt 1
commits fully; Rama's progress tracking fails; the record retries. Between attempt 1 and the
retry (`:retry-mode` is the default `:individual` on `*llm-depot`, so other records keep
processing), an executor has discovered the run, claimed it (status `:claimed`, token stored),
and streamed observations seq 0..10 (last-seq 10, items materialized). The retry recomputes the
same accepted decision (pure fn of the request — llm.clj:370-376) and termval-overwrites the
row: claim token wiped, status reset to `:pending`, `:last-seq` reset to −1, items/buffer
erased. Consequences: (1) llm.clj:1335 re-adds the run to `$$llm-pending-by-task` → a second
executor discovers and claims the reset row → **duplicate provider-process spawn for one run**
(retry-unsafe external side effect chain); (2) the first executor's next observation (seq 11)
buffers against `last-seq −1` and the gap 0–10 can never refill (those depot records' offsets
are already consumed) → the run stalls permanently. PLAN.md (TurnRunRequest step 3) required
exactly the guard that is missing ("guarded by `:status` nil — never clobbers a row that later
records already advanced"); step 4's inbox-resurrection guard is also absent.

**(b) No request dedup at all.** Nothing checks for an existing run/decision before writing
(llm.clj:1305-1337). A replayed request with the same `:request/id`/`:idempotency/key` but a
fresh run id (the `turn-run-request` constructor mints run ids randomly, llm.clj:178) mints a
second run, second inbox entry, second thread-list entry, and overwrites `$$llm-turn-run-by-turn`
(llm.clj:1333). Spec Op 1: "replaying the same request id / idempotency key … must not mint a
duplicate run, inbox entry, or thread entry."

**(c) Resolved-approval resurrection in `$$llm-approvals-pending`.** llm.cl j:1386-1390: on any
redelivery of an approval observation, `observation-approval-materialized?` (llm.clj:1122-1127)
checks only obs type + approval id + `seq ≤ last-seq`, then termvals the ORIGINAL `:pending`
approval row (built from the obs, llm.clj:816-827). Trace: approval obs folds; an
`:approval/resolve` control removes the pending row (llm.clj:1418-1421); the obs record then
retries (legal after full commit, stream.md:9) → the check passes (seq ≤ last-seq) → the
`:pending` row is rewritten into `$$llm-approvals-pending`, resurrecting a resolved approval
for every reader of `read-pending-approval`.

**(d) Non-idempotent appends on redelivery.** `add-observation-error` (llm.clj:685-693) conj's
an error entry with no per-record key — every redelivery of an invalid observation appends a
duplicate entry (bounded at 50, so capped corruption, but non-idempotent). `add-steer` and
`add-compaction` (llm.clj:1231-1247) plain-`conj` onto vectors; on control-record retry after
commit, `fold-control` re-runs and `record-control`'s `conj-distinct` dedups `:control-order`
but `:steers`/`:compactions` gain duplicate entries.

**(e) What IS retry-safe** (for completeness of the trace): claim grant — retry sees `:status
:claimed`, `grantable-claim?` (llm.clj:641-647) is false, no write; observation fold — `(<
seq-id expected)` (llm.clj:1113-1114) returns the row unchanged and all downstream termvals
rewrite identical values; cost rollup — delta against stored per-run usage (llm.clj:550-577) is
zero on retry; `conj-distinct` guards; `keep-existing-item-row` guard (llm.clj:1147-1149). No
IDs are generated inside dataflow (all `random-id` calls are in client-side constructors); no
`depot-partition-append!` exists.

**FAIL — major** (findings a–c are correctness-destroying under documented retry semantics).

## Partial failure in stream topologies
<!-- writes to multiple PStates across multiple partitions; can partial failure + retry leave writes permanently unexecuted? -->

**Check:** for each multi-partition stream event, can partial failure + retry leave any write
permanently unexecuted?

**(a) Claim: pending-inbox removal permanently skipped — yes.** Event shape (llm.clj:1339-1352):
run task commits the grant (row `:claimed`, llm.clj:1348) → `|hash *executor-task-id`
(llm.clj:1351) → remove pending entry (llm.clj:1352). Per stream.md:96 the run-task streaming
batch commits independently; if the executor-task batch fails, the record retries from
`source>`. On retry, `local-select>` reads `:status :claimed`, `grantable-claim?` is false, the
entire `<<if` body — including the removal at 1352 — is skipped. The run stays in
`$$llm-pending-by-task` forever. Downstream livelock: `first-pending-entry` (llm.clj:1496-1503)
sorts the lane by run-id and takes the FIRST entry; `run-one-pending-with-adapter!`
(llm.clj:1913-1956) picks the stuck entry every poll, its claim returns `:conflict-or-past`,
nothing is spawned, and no other pending run on that lane is ever attempted — the lane is
head-of-line blocked permanently. The plan's design (re-grant on token equality so the retry
re-traverses the removal hop) was not implemented.

**(b) Self-healing paths, traced for completeness:** observation thread-hop (llm.clj:1377-1385)
— retry dedups the fold, recomputes identical derived values, and re-executes the hop, so a
failed thread-task batch is completed by retry; control terminal-removal hop (llm.clj:1422-1424)
— `fold-control` re-runs deterministically, `terminal-run-row?` still true, hop re-executes;
request hops re-execute unconditionally (with the check-7(a) clobber hazard).

**FAIL — major** (finding (a): permanent inconsistency plus lane livelock from a single
transient failure).

## Single depot append per client operation
<!-- each client write operation must call foreign-append! exactly once -->

**Check:** each client write operation must call `foreign-append!` exactly once; additional
depot writes happen server-side.

- `append-turn-run-request!` / `append-claim!` / `append-control!` (llm.clj:1466-1492): one
  append each. PASS for these.
- Observation streaming: one append per provider event (each event is its own logical
  operation per the spec's Op 3). Acceptable.
- `mark-stale-approvals!` (llm.clj:1988-2001): **FAIL** — one client operation ("mark this
  run's stale approvals") issues one `foreign-append!` PER pending approval in a client-side
  `doseq`. Trace a crash between appends with 3 pending approvals: approval 1's
  `:approval/resolve` control folds, `resolve-approval`'s terminal-decision branch
  (llm.clj:1203-1219) sets the run `:failed` immediately; approvals 2–3 remain `:pending` in
  the run row and in `$$llm-approvals-pending` — durably inconsistent ("every unresolved
  approval on the run is marked :expired" is violated) until someone manually re-invokes. The
  plan's `StaleApprovalsMark` record (one append, server-side fan-out) was not implemented.

**FAIL** (requires a new record type + topology branch, not a line edit).

## Application-state caches survive restart
<!-- for each TaskGlobal or in-process cache holding application state: durable source + concrete rebuild path -->

**Check:** every TaskGlobal or in-process cache holding application state has a named durable
source and a concrete rebuild path.

Module-side: no TaskGlobals, no in-process module caches — all module state is PStates. That
half passes.

Executor-side (the executor helpers live in this module file and the spec's Op 6 makes restart
recovery a requirement): the executor's knowledge of which runs it has claimed exists ONLY in
process memory (the return values of `claim-run!`/`run-one-pending-with-adapter!`,
llm.clj:1518-1531, 1913-1956). There is no durable source to rebuild it: the pending entry was
removed at grant (llm.clj:1352), and the plan's `$$executor-active-runs` recovery index does
not exist. Worse, even an executor that durably remembered its run id and token cannot verify
its grant after streaming starts: `claim-state` (llm.clj:659-667) returns `:granted-to-us` only
while `:status = :claimed`, and the first item promotes status to `:running`
(llm.clj:925-927), after which the rightful owner reads `:conflict-or-past`. Trace: executor
crashes mid-stream → restarts → has no way to discover its orphaned `:claimed`/`:running` runs
→ runs leak in non-terminal states forever; the spec's "executor restart mid-run → resume or
fail per recorded policy" and the stale-approval crash protocol can never be triggered for
unknown runs.

**FAIL** (requires a new PState + claim/terminal-fold writes — structural).

## No reimplementation of built-in operations
<!-- custom code duplicating com.rpl.rama built-ins is FAIL -->

**Check:** no custom code duplicating Rama built-ins.

`select-pstate-one` (llm.clj:2003-2005) — `(first (foreign-select path pstate))` — is a
hand-rolled `foreign-select-one` (built-in, core-concepts.md:34) and is used by all 21 read
helpers (llm.clj:2007-2097). Everything else (`conj-distinct`, `append-bounded`,
`normalize-task-id`) does not duplicate Rama operations.

**FAIL** (one-line fix per call site).

---

## Plan Divergences (Phase doc step 3)

The implementation diverges from PLAN.md on nearly every structural decision:

1. **Four depots instead of one** (llm.clj:1277-1280 vs plan "One depot, not five"): destroys
   the mechanical per-run total order the plan derived from spec invariant 4 (see S1).
2. **`{String Object}` everywhere, plain maps** vs plan's defrecords/`ILLMRecord`/fixed-keys
   schemas ("No `Object` appears anywhere").
3. **No subindexing** vs plan's 15 subindexed structures (check 6).
4. **No write guards** on the run-row request write vs plan's status-nil guard (check 7a).
5. **No `ack-return>`** anywhere; W1/W2/W5 results are recovered by client polling
   (`await-materialized` llm.clj:2099-2110) instead of the plan's `:ack` + ack-return.
6. **No `$$orphaned-records`** — unknown-run observations and controls are silently dropped
   (S1) instead of durably retained.
7. **No `$$executor-active-runs`** — executor restart recovery impossible (cache check).
8. **No approval-timeout scheduler / tick depot** (S4).
9. **Immediate cancel** vs plan/spec two-phase cancel (S3).
10. **No query topologies; no microbatch trail topology** — derived views are fully
    materialized on the stream hot path instead (checks 3, 6); cross-key indexes
    (`$$llm-item-by-id`, `$$llm-control-by-id`, `$$llm-approvals-pending`) are stream-side hops.
11. **Decisions keyed by run-id** (llm.clj:1309) not request-id — permitted by the spec's read
    table ("by run/request id"), but rejected requests lacking a run id all collide on the
    `nil` key and overwrite each other (llm.clj:1307-1309), degrading "rejected requests remain
    queryable as decisions."
12. **No `:native-conflicts` recording**; additionally `upsert-thread-row` (llm.clj:491-494)
    gives the run-row's native id precedence over the stored thread value, so two
    pre-native-binding runs on one thread that observe different native ids silently FLIP the
    thread binding (run-1 binds X; run-2's obs binds Y; thread hop writes `(or Y X)` → Y) —
    violating the spec's "a CONFLICTING id must not silently flip the binding."

## Spec Validation (beyond template checks)

**S1. Per-run total order + never-drop — VIOLATED (major).** Spec invariants 4 and 5. Four
depots with the same `hash-by` give no cross-depot ordering: a claim, control, or observation
can be processed before its run's request. When that happens, `<<if (known-run-row? *run-row)`
(llm.clj:1358, 1403) has no else branch: the observation or control is consumed and **silently
dropped** — no orphan store, no error row, and the control never reaches `$$llm-control-by-id`
(llm.clj:1417 is inside the known-run branch), violating "every fact … queryable by … control
id" and "Never drop observations." Executor/bridge causality (claim only after pending visible)
narrows but does not close this: at-least-once redelivery and `:individual`-mode reordering
across depots make it reachable without any client misbehavior.

**S2. Sticky terminal statuses — VIOLATED (major).** Every terminal transition is unguarded:
`:codex/run-finished` (llm.clj:952-956) and `:claude/result` (llm.clj:1056-1063) set
`:succeeded` unconditionally — a run-finished folding after a cancel flips `:cancelled` →
`:succeeded`; `cancel-run` (llm.clj:1221-1229) sets `:cancelled` unconditionally — a late
cancel control flips `:succeeded` → `:cancelled`; `add-approval` (llm.clj:829-834) sets
`:blocked-awaiting-approval` unconditionally — a late approval observation un-terminates a
closed run; `resolve-approval`'s terminal-decision branch (llm.clj:1213-1219) sets `:failed`
on any run, including `:succeeded` ones. The spec requires "Terminal states are sticky … must
not regress the status" and "first fold wins."

**S3. Two-phase cancel + cancel responsiveness — VIOLATED (major).** Spec Op 4: durable
`:cancel-requested` marker first; truth transitions to `:cancelled` only on the provider's
abort-confirmation observation. Implementation cancels truth immediately (llm.clj:1221-1229,
1264-1265) with no `:cancel-requested` state. And the executor helpers never observe controls
mid-turn: `run-claude-process->events` (llm.clj:1840-1880) blocks on `waitFor` and collects all
output before any observation is appended, with no control-polling loop — so a cancel during a
live turn neither interrupts the provider (~1s responsiveness unmet) nor survives: the
provider's eventual run-finished then overwrites `:cancelled` with `:succeeded` (S2).

**S4. Approval lifecycle — VIOLATED (major).**
- *Second resolution wins instead of first*: `resolve-approval` (llm.clj:1183-1219) overwrites
  the stored approval row unconditionally; a `:denied` resolve after an `:approved` one flips
  the row and fails the run. Spec: "the first wins; the second is a no-op."
- *Resurrection*: an `:expired` approval re-resolved `:approved` is overwritten to `:approved`
  and can unblock the run. Spec: "no resurrection."
- *Invention*: resolve for an unknown approval id creates a row from scratch (the `existing`
  default map, llm.clj:1189-1192). Spec: "no-op; nothing invented."
- *run-finished with unresolved approvals*: nothing expires them (llm.clj:952-956) — they stay
  `:pending` on the run row and in `$$llm-approvals-pending` forever after a successful close.
- *No approval timeout*: the spec requires unresolved approvals to be durably declined/expired
  after a wall-clock timeout ("the provider waits forever; the kernel side must not"). No
  timeout mechanism exists anywhere in the module (no tick depot, no scheduler).

**S5. Out-of-order approval loses the pending index — BUG.** `observation-approval-materialized?`
(llm.clj:1122-1127) tests only the CURRENT record. An approval observation that arrives ahead
of a gap is buffered (no pending-index write — correct so far); when the gap-filler arrives and
`drain-observation-buffer` folds the approval, the current record is the gap-filler, the check
is false, and the `|hash *approval-id` hop (llm.clj:1386-1390) never runs — the
`$$llm-approvals-pending` row is never written for any approval that entered via the buffer.
The run row shows blocked, but the approval-by-id read surface is permanently missing the row.

**S6. Deterministic fold — VIOLATED (minor).** Wall-clock fallbacks inside the fold path:
`apply-observation-effect` llm.clj:917, `observation-error` llm.clj:683, `add-observation-error`
llm.clj:687, `grant-claim` llm.clj:651 — all `(or (:…-ms record) (now-ms))`. Any record lacking
its timestamp (validation does not require `:received-at-ms` or `:claimed-at-ms`) makes the
fold non-deterministic across replays and instances, violating spec invariant 1
(writer-asymmetry) and producing different values on retry.

**S7. Bounded live view — VIOLATED.** `run-view` (llm.clj:512-523) embeds the FULL item vector
and is rewritten into `$$llm-views` on every observation (llm.clj:1370). The spec requires the
live per-run stream view to stay bounded; there is no window, truncation marker, or count.

**S8. Live streaming — VIOLATED (major, executor helpers).** Spec Op 3: this is the streaming
path; provider-token → queryable under ~200 ms. The Claude executor path appends NOTHING until
the provider process exits or times out (default 120 s): `run-claude-process->events`
(llm.clj:1840-1880) drains stdout via futures, `waitFor`s, converts all lines, and only then
does `run-one-pending-with-adapter!` (llm.clj:1947-1948) append observations in a batch. The
product's "watching reasoning arrive live" is structurally impossible with these helpers.

**S9. Restart-policy is decorative — minor.** `mark-stale-approvals!` returns the run's
recorded `:run/restart-policy` (llm.clj:1999) but never consults it; failure is hardcoded via
`resolve-approval`'s terminal-decision branch. Behavior happens to match the only MVP policy
(`:fail-on-stale-approval`), and the structured error shape (llm.clj:1216-1219:
`{:reason :approval/declined :decision …}`) matches the spec, so this is recorded as a design
debt, not a behavioral spec break today.

**S10. Secret hygiene — VIOLATED (minor).** The good: child env stripped of all auth-bearing
vars in both Claude modes (`claude-child-env` llm.clj:1554-1568 over `sensitive-env-keys`
llm.clj:86-97); api-key injected only at spawn from a secret resolver; Claude stream lines
redacted before observation construction (`redact-provider-payload` applied at llm.clj:1630,
1638-1640); the unredacted process env never enters any append. The violation: provider STDERR
is appended durably unredacted — `:stderr-tail (take-last 20 @stderr-lines)` inside the
run-failed observation (llm.clj:1865-1872) bypasses `redact-provider-payload` entirely, and the
spec's invariant is "provider stream lines are redacted before storage." Additionally nothing
enforces redaction for non-Claude adapters (`adapter-event->observation` llm.clj:1900-1911
stores `:raw/json` as given), leaving the invariant purely conventional on that path.

**S11. What the spec validation found CORRECT (traced, for honesty of the audit):** request
validation surface matches Op 1 (execution-options smuggling → `:payload/execution-options-not-bundle-owned`
llm.clj:307-309; `:passive-observe` rejected llm.clj:297-299; missing backend → `:codex`
llm.clj:133-135); decision determinism (`decided-at` = request `time-ms`, event ids derived
from request id — llm.clj:324-368); follow-up runs are new run ids on the same thread and
inherit native ids at acceptance (`bind-run-to-existing-thread` llm.clj:497-506); grant-before-
execute with token verification and fork-binding gate honored by the executor helper
(llm.clj:1920-1934, `:spawned-after-grant?` llm.clj:1954); claim never invents or regresses a
run (llm.clj:641-647 + no-write else branch); seq buffering drains in order and never drops
in-protocol observations for known runs (llm.clj:1082-1120); cost rollup is delta-based with
no double-count on re-report (llm.clj:550-585); token usage keeps separate fields including
cached-input (llm.clj:836-843, 1615-1623); reads are nil/empty-safe in every state
(llm.clj:2007-2097); partition alignment is correct before every `local-select>`/
`local-transform>` (traced all four sources hop-by-hop).

## Self-consistency check

Re-read performed. Both PASS-with-context entries (`:allow-yield?`, redundant conditionals,
nil->val) contain no acknowledged gap within their own scope; everything phrased as a gap,
tradeoff, or bug ("permanently skipped", "resurrecting", "silently dropped", "decorative",
"bypasses redaction") sits inside a check or S-finding marked FAIL/VIOLATED. No check is
certified while its body concedes a deficiency.

## Verdict

**major-fail** — multiple failures require restructuring, not line edits: the schema must be
rewritten with subindexed structures and targeted writes (check 6/3); the request flow needs
existence guards and dedup to stop retry-clobber and duplicate-spawn (check 7a/b); the
claim/inbox flow needs a retry-traversable removal path (partial-failure check); cross-record
ordering/never-drop needs a single depot or an orphan store (S1); terminal-stickiness,
approval-resolution, and two-phase-cancel guards are missing across the fold (S2-S4); and the
kernel has no approval-timeout machinery and no executor-restart recovery surface at all
(S4, cache check). Under the rubric — "pick major-fail whenever any single failure requires
more significant changes" — at least six independent findings individually qualify.
