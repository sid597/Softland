# Implementation Validation — POST-FIX (Transcript Kernel, May-era remnant)

> Retrospective Phase-4 re-validation (2026-06-12), fresh context, blame-scoped to the May-era
> acquisition paths as fixed in this session's `FIX_PLAN.md`. Adversarial default-fail posture:
> every check below was code-traced with line citations before being ticked. Validated against
> `IMPLICIT_SPEC.md` (the contract) AND `FIX_PLAN.md` (the plan of record). Items the plan
> DEFERS WITH FLAGS are reported as "deferred per plan", not as new failures.
>
> Target module @ working-tree HEAD: `src/app/server/rama/dogfood/transcript.clj`
> Shared guards: `src/app/server/rama/core.clj:518-811`
> Contract evidence (not modified): `dogfood_transcript_test.clj`, `dogfood_transcript_probe_test.clj`
>
> **Runtime corroboration:** both test namespaces run green at HEAD —
> `Ran 4 tests containing 57 assertions. 0 failures, 0 errors.` The probe namespace encodes the
> adversarial scenarios (parse-error redaction, dup-submit, partial-tail, multi-tool, sticky
> terminals, claim arbitration, per-run counters, orphan-obs). Static traces below are primary;
> the green run is independent confirmation.

---

## A. Plan-vs-implementation conformance (FIX_PLAN batches)

| Plan item | Implemented at | Conforms? |
|---|---|---|
| B1 `redact-text` (string→{:text :redactions}) w/ unterminated-pair pattern | `transcript.clj:391-436` | YES |
| B1 `redact-payload-with-redactions` mirrors `llm/redact-provider-payload` | `transcript.clj:452-487` vs `llm.clj:788-805` | YES (same `sensitive-key?`, same `"[REDACTED]"`) |
| B1 `redacted-preview` = redact-FIRST-then-truncate | `transcript.clj:438-450` | YES |
| B1 parse-error branch stores redacted preview + its text-pattern redactions | `transcript.clj:517-527` | YES |
| B2 `reduce-jsonl-observations` bounded streaming reader | `transcript.clj:610-657` | YES |
| B2 `read-complete-appended-lines` snapshot-bound, TOCTOU gone | `transcript.clj:1296-1311` | YES |
| B2 non-OC harvest streams appends, per-file try/catch | `transcript.clj:1242-1294` | YES |
| B2 non-OC watch keyed by inode file-key, cursor-after-append | `transcript.clj:1430-1522` | YES |
| B3 ledger `write-if-absent`, file-cursor monotonic fold, per-run seen-marked counters | `transcript.clj:780-802` | YES |
| B3 conversation unconditional keypath, tool `explode`-all w/ `:tool-call/input` | `transcript.clj:803-811`, `370-385` | YES |
| B3 `fold-request` (first-write-wins + fingerprint) | `transcript.clj:216-236` | YES |
| B3 `fold-claim` (replay / authorize / grant / dead-letter) | `transcript.clj:238-288` | YES |
| B3 executors mint executor-id, await grant, verify ownership, materialization barrier | `transcript.clj:1206-1294`, `1416-1522` | YES |

No silent divergence from the plan. Deferrals (obs-depot partitioning, TR-08 schema reshape,
`:since`, empty-file ledger note, parse-error-conversation-under-path-fallback, A4 EOF note) are
explicitly carried in the plan's "Deferred with flags" section and are reproduced as flags below.

---

## B. Template check matrix

### B.1 Redundant conditionals
**Check:** if every branch of `<<if`/`<<cond` does the same op with only a variable differing, collapse it.
**Trace:**
- Request source `<<if (empty? *errors) (initial-run-row …) (else>) (rejected-run-row …)` —
  `transcript.clj:742-745`. The two branches call *different* row builders, not the same op with a
  swapped var. Not redundant.
- Observation `<<if (not (identical? *ledger-row *existing-line)) (local-transform> …))` and the
  `*folded-file-state` twin — `transcript.clj:785-793`. Single-armed guards (skip-write), not
  same-op-both-arms. Not redundant.
- Counter `<<if (some? *run-row) … (<<if (nil? *seen) …))` — `transcript.clj:796-802`. Existence/
  seen gates. Not redundant.

**PASS** — no redundant-branch conditionals.

### B.2 Consecutive keypath
**Check:** `(keypath *a)(keypath *b)` → `(keypath *a *b)`.
**Trace:** the counter write uses `(keypath *request-id *line-key)` already collapsed
(`transcript.clj:797,800`); conversation uses `(keypath *conversation-id *line-key)`
(`transcript.clj:804`). No split-keypath instances remain (the plan called out fixing the
consecutive-keypath hygiene en route).
**PASS.**

### B.3 Select-compute-transform → +compound/aggregator
**Check:** `local-select> → compute → local-transform> termval` should use `+compound` + aggregator where possible.
**Trace:** the observation event deliberately uses read-fold-write per write site
(`transcript.clj:780-802`) because the fold logic (`write-if-absent`, `fold-source-file-state`,
seen-marker + increment) is conditional and idempotency-critical — it is the documented
idempotent-per-write-site pattern (`stream.md:175-187`: "short-circuit if already seen before
applying mutations"). A `+compound`/aggregator path would not express the seen-marker barrier
that makes the counter increment replay-safe. This is the correct shape for at-least-once
delivery, not a missed cleanup. The `(filter> (not (identical? …)))` and `<<if (not identical?)`
guards specifically skip no-op writes, which is the recommended convention.
**PASS** (intentional read-fold-write; not a collapsible select-compute-transform).

### B.4 Unnecessary nil->val
**Check:** navigators treat nil as empty; don't add `nil->val` unless the next navigator needs non-nil.
**Trace:** No `nil->val` appears anywhere in the module. Nil handling is in pure folds
(`fnil inc 0` at `transcript.clj:697-699`; nil-safe `fold-source-file-state` at `676-680`),
which is correct — those run in Clojure fns, not as path navigators.
**PASS.**

### B.5 :allow-yield?
**Check:** subindexed range/ALL iteration over ~100+ entries on a non-mirror PState needs `:allow-yield?`.
**Trace:** The new subindexed PState `$$transcript-run-seen-lines {String (map-schema String Boolean {:subindex? true})}` (`transcript.clj:729-730`) is touched ONLY by point reads/writes:
`(local-select> [(keypath *request-id *line-key)] …)` and `(local-transform> [(keypath *request-id *line-key) (termval true)] …)` (`transcript.clj:797,800`). A fully-keyed `[outer inner]`
keypath is a single-entry lookup, NOT a range/ALL scan — no iteration, so `:allow-yield?` is
neither required nor beneficial here (the template warns against adding it to small-bounded reads).
All other selects in the topology (`$$transcript-runs`, `$$transcript-source-ledger`) are
single-key point reads. No range navigator (`ALL`/`MAP-KEYS`/`MAP-VALS`/`sorted-map-range*`)
appears in the in-scope May topology.
**PASS.**

### B.6 Non-subindexed collections without size limits
**Check:** every write to a non-subindexed inner collection must enforce a max size, else the schema must be subindexed.
**Trace, per write site:**
- Run-row bounded vectors: `:applied-claim-ids` (limit 50, `transcript.clj:205,270-271`),
  `:claim-errors` (limit 50, `205,286-287`), `:request-conflicts` (limit 10, `207,234-236`),
  `:progress` (limit 50, `203`) — all via `append-bounded` (`transcript.clj:178-184`). Bounded. PASS.
- File-cursor row, conversation entry, tool-call row, ledger row — each is a whole-row `termval`
  overwrite at its key, not an accumulating inner collection. No unbounded growth at the row.
- `$$transcript-run-seen-lines` inner map — IS subindexed (`transcript.clj:730`), so unbounded
  per-run line count (10^5–10^6) is the correct shape. PASS.
- **`$$transcript-observed-conversations` inner map** — written at
  `(keypath *conversation-id *line-key) (termval …)` into a `{String Object}` PState whose inner
  value is a plain (non-subindexed) map keyed by `*line-key` (`transcript.clj:724,804`). A long
  session's conversation grows this inner map without a cap and without subindexing. **This is a
  real liability** — BUT it is the explicit TR-08 deferral: `FIX_PLAN.md:152-156` records "The
  unbounded non-subindexed inner conversation map REMAINS a known liability at production scale,"
  and the module header documents it (`transcript.clj:710-716`). **Deferred per plan** — not a
  new failure; reported as the standing TR-08 liability.

**PASS for everything this session owns; the one uncapped collection is the sanctioned TR-08 deferral.**

### B.7 Stream topology idempotency
**Check:** trace every write/side-effect under event retry; non-idempotent ops need a dup-prevention mechanism.
This is the load-bearing check; traced per source branch.

**Request source (`transcript.clj:738-749`)** — partitioned by `hash :transcript/request-id`,
single-task, no internal partitioner. Retry replays from `source>`. `fold-request`
(`transcript.clj:216-236`) is first-write-wins: `nil existing` → stamp row + fingerprint;
same-fp → returns IDENTICAL existing row; different-fp → bounded conflict note. The
`(filter> (not (identical? *folded-run *existing-run)))` (`748`) skips the write on every no-op
replay. **Idempotent.** Probe `dup-harvest` / `dupdiff-harvest` confirm.

**Claim source (`transcript.clj:754-761`)** — single-task on `request-id`. `fold-claim`
(`transcript.clj:256-288`) front-gates on `:applied-claim-ids` membership → exact replay of a
folded claim-id returns the IDENTICAL row (no double progress/audit). `authorize-mutation` makes
terminals sticky and arbitrates ownership. `(filter> (not (identical? …)))` (`760`) skips no-op
writes. **Idempotent.** Probe `late-harvest`, `race-harvest`, `failcancel-watch` confirm.

**Observation source (`transcript.clj:772-811`, `:retry-mode :all-after`)** — the multi-partition
event. Five partitioner hops: `|hash *line-key` (779) → `|hash *file-state-key` (788) →
`|hash *request-id` (794) → `|hash *conversation-id` (803) → `|hash *tool-call-id` (809). Per
write site:
1. **Ledger line** (`780-787`): `core/write-if-absent` (`core.clj:609-616`) — first obs wins,
   replay returns existing row, `<<if (not identical?)` skips. Idempotent termval.
2. **File cursor** (`788-793`): `fold-source-file-state` (`670-680`) keeps the row with the
   higher `:source/last-byte-offset` (row-level monotonic watermark). Replays/reorders cannot
   rewind. `<<if (not identical?)` skips. Idempotent.
3. **Counter** (`794-802`): `<<if (some? *run-row)` then `<<if (nil? *seen)`: seen-check +
   `(termval true)` seen-write + counter `(termval *counted-run-row)` all on the `*request-id`
   task between the `|hash *request-id` (794) and `|hash *conversation-id` (803) boundaries — one
   event-transaction (`stream.md:164,173`). A replay after this commit reads `*seen` non-nil →
   no double-count. A partial replay (failure before the next partitioner) re-executes the whole
   group, but the seen-marker + increment commit together, so they cannot tear. Idempotent.
   Probe `obsdup-harvest`, `rerun-2`, `orphan-request` confirm.
4. **Conversation** (`803-805`): unconditional `(termval *conversation-entry)` at
   `(keypath *conversation-id *line-key)` — deterministic per line identity; rewriting the same
   value is a no-op for the materialized count. Idempotent.
5. **Tool calls** (`806-811`): `explode` over `tool-call-index-rows`; each row
   `(termval *tool-call-row)` at `(keypath *tool-call-id)` — deterministic per id. Empty input
   → `explode` zero-emit skips this terminal write (`dataflow.md:238`), which is exactly the rows
   with no tool_use. Idempotent.

IDs are NOT minted inside the topology (executor-id is client-minted at `transcript.clj:1224,1421`;
line identity is byte-derived). No internal `depot-partition-append!`. **PASS.**

### B.8 Partial failure in stream topologies
**Check:** an event writing across multiple partitions that fails+retries after some writes commit must not leave any write permanently unexecuted.
**Trace (the emphasis check):** `:retry-mode :all-after` (`transcript.clj:772`) means on failure the
event replays from `source>`; per `stream.md:168`, writes already committed on OTHER tasks via
partitioner hops are NOT rolled back, only the failing task's batch is discarded. Walk a failure
after EACH hop:
- Fail after ledger commit (line task), retry: ledger `write-if-absent` re-reads its own row →
  no-op; flow proceeds to file-state, counter, conversation, tool as if first time. No loss.
- Fail after file-state commit, retry: monotonic fold re-applies → no rewind; downstream re-runs.
- Fail after counter commit, retry: seen-marker present → counter not re-incremented; conversation
  and tool re-run (idempotent termvals). The one window that would double-count (increment
  committed, seen-marker NOT) cannot occur — both are termvals in the same inter-partitioner event
  transaction (`794-802`), committed atomically.
- Fail after conversation commit, retry: conversation termval re-applies identically; tool re-runs.
- Tool is last; a failure there replays the whole event, every prior site idempotent.

No write can be permanently lost (every site re-executes on `:all-after` replay) and none can be
double-applied (every site is termval/write-if-absent/monotonic/seen-marked). **PASS.**

### B.9 Single depot append per client operation
**Check:** each client write op calls `foreign-append!` exactly once; extra writes happen server-side.
**Trace:** `append-transcript-request!` (`835-840`), `append-transcript-status!` (`842-847`),
`append-transcript-observation!` (`849-854`) each call `foreign-append!` exactly once. The
executors call these as discrete, individually-idempotent operations: harvest appends 1 request +
1 running claim + N observations + 1 complete claim, but each is a standalone idempotent append
whose retry/replay is absorbed by the folds — a crash between them leaves the run mid-lifecycle
(`:pending`/`:running`), recoverable by idempotent rerun (spec I4/A.0). No multi-append atomic
op is claimed. **PASS.**

### B.10 Application-state caches survive restart
**Check:** every TaskGlobal / in-process cache holding app state needs a durable source + a concrete rebuild path.
**Trace:** The in-scope May code uses NO TaskGlobal executor. The only in-process state is the
non-OC watch executor's `offsets`/`known-at-start`/`stop?` atoms (`transcript.clj:1430-1455`),
which live in the foreign client (test/Electric server), NOT in the module. Durable source: the
`$$transcript-source-ledger` file-state rows; rebuild path: `initialize-offset`
(`transcript.clj:1439-1450`) reads `read-source-file-state` and resumes from
`:source/last-byte-offset`, falling back to EOF baseline for unknown-existing files. A killed
harvest is re-issued from scratch (idempotent rerun), not cursor-resumed — spec A.0 explicitly
excludes executor-side resume. **PASS.**

### B.11 No reimplementation of built-in operations
**Check:** no hand-rolled duplication of `com.rpl.rama.ops` (etc.) built-ins.
**Trace:** `reduce-jsonl-observations`/`read-jsonl-observations` are byte-level UTF-8 readers, not
Rama built-ins — they exist precisely because `RandomAccessFile.readLine` corrupts multi-byte
offsets (`transcript.clj:557-574`); no Rama op does newline-gated byte-correct file reading.
`append-bounded` is a Clojure-side bounded-vector helper, not a Rama navigator. `select-pstate-one`
wraps `foreign-select-one` (`856-858`) — a thin foreign-client convenience, not a reimplementation.
`explode` uses the built-in `ops/explode`. No built-in is reimplemented.
**PASS.**

---

## C. Spec invariant conformance (I1–I9) — in-scope May paths

- **I1 Observation-only** — non-OC harvest/watch only `read` files and `foreign-append!`
  observations; no spawn, no file mutation, no downstream trigger
  (`transcript.clj:1242-1294,1456-1484`). **PASS.**
- **I2 Nothing persists unredacted** — parsed branch redacts via `redact-payload-with-redactions`
  BEFORE building `:transcript/redacted-payload` (`transcript.clj:505-515`); tool-call inputs come
  off the already-redacted payload (`transcript.clj:372-378`); parse-error branch persists only the
  redacted preview + text-pattern redactions, never raw bytes, with `:transcript/redacted-payload
  nil` (`transcript.clj:517-527`); conflict notes/dead-letters carry fingerprints/reasons, not
  content. `:transcript/redactions` metadata populated on both branches. Probe `secret-harvest`
  (parse-error with `api_key` secret), `tools-harvest` (multi-tool with secret), and the contract
  test's `never-store-me` scan confirm no readable projection leaks the secret. **PASS.**
- **I3 Source-record identity** — `source-line-key` = `(source, file-id, byte-offset, line-hash)`
  (`transcript.clj:317-321`); `file-id` is device+inode with canonical-path fallback
  (`302-311`); hash is over exact content bytes (`line-hash-bytes`, `294-300`). **PASS.**
- **I4 Logs record, folds deduplicate** — ledger `write-if-absent`, conversation/tool deterministic
  termvals; re-harvest leaves conversation count at 2 (contract test `69`), probe
  `obsdup-harvest` keeps views stable. **PASS.**
- **I5 Byte-correct, newline-gated** — `reduce-jsonl-observations` bound IS the newline gate;
  a line whose terminator is not inside the bound is withheld and `:next-offset` stays at line
  start (`transcript.clj:648-651`). `read-complete-appended-lines` snapshots `len` once as the
  bound, killing the live-EOF TOCTOU (`1296-1311`). UTF-8 offsets byte-accurate. Probe
  `utf8-harvest` (multi-byte offsets/length/hash) and `partial-harvest` (cursor stops at last
  complete line) confirm. Watch contract test split-at-byte-20 confirms partial withholding. **PASS.**
- **I6 Build/persist boundary** — `read-jsonl-observations` is pure; contract boundary test
  (`dogfood_transcript_test.clj:71-96`) confirms run nil + conversation empty after build, then
  `:pending` after request append, then count 1 after one obs append. **PASS.**
- **I7 Monotonic run lifecycle** — `sticky-status` (`core.clj:618-624`) +
  `authorize-mutation` terminal fence; probe `late-harvest` (`:running` after `:complete` stays
  `:complete`) and `failcancel-watch` (`:cancelled` after `:failed` stays `:failed`) confirm.
  Watch `stop!` appends `:cancelled` which the fence rejects if already terminal
  (`transcript.clj:1513-1522`). **PASS.**
- **I8 Fail-isolated** — non-OC harvest per-file try/catch records a progress claim and continues;
  loop-level failure → `:failed`, never stuck `:running` (`transcript.clj:1249-1294`). Watch
  per-file try/catch + fatal-stop (`1456-1522`). **PASS.**
- **I9 Already-emitted observations never retracted** — all writes are additive
  termvals/append-bounded; nothing deletes ledger/conversation/tool rows. **PASS.**

## D. Entity State × Write matrix (in-scope rows)

Traced rows that the in-scope code implements (citations above): Run does-not-exist×submit
(`:pending`, `742-749`); pending×claim (`:running` + owner, `276-279`); pending×dup-submit
(no reset, `224-236`); running×record-obs (counters + conversation + tool, `794-811`);
terminal×late-progress (no regression, `260-288`); complete×new-id re-harvest (independent
lifecycle, dedup views — probe `rerun`). Source-line unobserved×parsed / ×parse-error /
×partial-tail (withheld) all match. Conversation unknown→populated and dup-no-op match.
Tool-call unknown→indexed, multi-block, dup-no-op match (probe `tools-harvest`). File-cursor
known×partial (cursor frozen) and inode-keyed recreate match. **No matrix row in scope contradicts.**

---

## E. Deferred-with-flags (sanctioned per FIX_PLAN.md:146-161 — NOT failures)

1. **F4 obs-depot partitioning** — `*transcript-obs-depot` hashes by
   `:transcript/ingest-request-id` (`transcript.clj:720`); the capture contract wanted source-file
   partitioning. A harvest burst funnels through one task before fan-out. Documented in the module
   header (`710-716`) and `FIX_PLAN.md:148-150`. Realignment needs a depot migration — out of
   session scope. Correctness is unaffected (per-line fan-out happens immediately via `|hash
   *line-key`); this is a throughput/locality deviation only.
2. **TR-08 conversation/ledger schema reshape** — `$$transcript-observed-conversations` inner map
   is non-subindexed and uncapped (B.6 above); ledger not re-keyed per file with subindexed offset
   maps; messages not order-keyed for source-ordered range reads. Standing production-scale
   liability, explicitly deferred (`FIX_PLAN.md:151-156`).
3. **`:since` floor** — not implemented (`FIX_PLAN.md:157-158`).
4. **Empty-file ledger note** — an empty/zero-line file produces no ledger row (spec wants
   "file still ledger-recorded as seen"); deferred (`FIX_PLAN.md:157-158`).
5. **Parse-error conversation visibility under path fallback** — the parse-error branch sets
   `:transcript/conversation-id (.getPath file)` (`transcript.clj:520`), so a parse-error row WILL
   group under a file-path-keyed conversation entry; spec A9 flags this as unspecified. Deferred
   (`FIX_PLAN.md:157-159`). (Note: the *parsed*-with-conversation-id path is correct; this is only
   the parse-error fallback, and the tested contract — parse-error lines excluded from the
   *session-id* conversation — still holds because the path fallback id differs from the session id.)
6. **A4 EOF-baseline silent skip** — existing-but-unknown files baseline at EOF with no ledger
   note; unchanged tested contract (`FIX_PLAN.md:161`).

---

## F. Self-consistency check

Re-read of all entries: every check ticked PASS is backed by a line-cited trace with no hedge.
The two places I wrote "liability"/"deviation" (B.6 conversation map, E.1 partitioning, E.2
schema) are explicitly the plan's sanctioned deferrals, reproduced as flags — they are NOT
checks I passed while privately doubting. No check body contains an un-deferred "gap", "tradeoff",
or "not ideal" attached to a PASS. The TR-08 uncapped map is the one production-scale hazard, and
it is carried by the plan, the module header, and this report as a deferral — consistent.

---

## Verdict

Every in-scope check passes under code-tracing and is independently corroborated by a green run of
both contract namespaces (57 assertions, 0 failures); the only uncapped/unaligned items are the
TR-08, F4-partitioning, `:since`, empty-file, and parse-error-fallback deferrals that
`FIX_PLAN.md` explicitly sanctions and documents in-module, so none counts as a new failure.

pass — all blame-scoped checks pass with line-cited traces and a green test run; the remaining liabilities (uncapped conversation map, obs-depot partitioning, `:since`/empty-file/parse-error-fallback) are all plan-sanctioned deferrals flagged in-module, not new defects.

PHASE_VALIDATION:pass
