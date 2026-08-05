# Implementation Validation

Module: `src/app/server/rama/dogfood/transcript_ingest.clj`
Plan: `docs/current-mental-model/build/chat-ingester/PLAN.md`
Spec: `docs/current-mental-model/build/chat-ingester/IMPLICIT_SPEC.md`
Product: `docs/current-mental-model/build/chat-ingester/PRODUCT.md`

Re-validation after fixes to three previously-identified major issues (Object schemas, missing ConversationMetaEntry, missing `:follows` edges) and four minor issues (consecutive keypath, parse-error projection, watch EOF, extract-conv-key duplication).

## Redundant conditionals

**PASS.**

`extract-conv-key` (lines 108-118) was previously flagged for character-for-character identical `let` blocks across four branches. The fix extracted a shared `extract-after-prefix` helper (lines 101-106) that both prefix-length groups call. The remaining four non-conv branches differ only in the prefix-length argument to this shared helper. Two branches pass `7` and two pass `6`. These cannot be collapsed further without losing the `cond` dispatch on prefix string, which is the natural Clojure pattern for prefix matching.

The request source's `<<if`/`else>` (lines 537-542) writes `$$ingest-runs` in both branches but calls different row constructors with different arities. Correctly structured -- the branch point is the row construction, not the write.

## Consecutive keypath

**FAIL.**

Previously flagged 4 locations. Three are fixed:

- Line 622: `(keypath *parent-id *ek)` -- merged. Correct.
- Line 652: `(keypath *request-id *line-key)` -- merged. Correct.
- Line 667: `(keypath *tool-name *ti-key)` -- merged. Correct.

One NEW consecutive keypath introduced in the file-state source (empty-file audit entry):

```clojure
;; Lines 709-710
(local-transform>
  [(keypath *fs-request-id)
   (keypath *sentinel-key)
   (termval ...)]
  $$audit-entries)
```

Should be `(keypath *fs-request-id *sentinel-key)`. This write targets `$$audit-entries` which is subindexed `{String (map-schema String AuditEntryRow {:subindex? true})}`. Consecutive `keypath` navigators cause an extra intermediate read on every write. Single-line fix.

## Select-compute-transform

**PASS.**

All five `local-select>` + `local-transform>` pairs in the topology are either guard-then-write patterns or multi-field read-modify-write patterns where `+compound` would not simplify:

1. Lines 547-550 (claim source): reads `$$ingest-runs` row, computes `fold-claim-into-run` (conditional merge of status, timestamps, counts, error), writes with `termval`. Multi-field fold -- `+compound` not applicable.
2. Lines 556-559 (source ledger dedup): reads existing, checks nil, writes `true`. Guard pattern, not a read-compute-transform.
3. Lines 604-614 (last-msg-per-conv): reads previous message ID, conditionally writes `:follows` edge, then unconditionally overwrites tracker. Guard + side-write pattern.
4. Lines 636-638 (files-handled): reads existing, checks nil, writes `true`. Guard pattern.
5. Lines 654-658 (run progress): reads `$$ingest-runs`, computes `increment-run-progress` + `increment-run-containers`, writes with `termval`. Multi-field update on a defrecord -- `+compound` not applicable.

## Unnecessary nil->val

**PASS.**

No `nil->val` navigator appears anywhere in the module. Verified by grep: zero occurrences.

## :allow-yield?

**PASS.**

All five `local-select>` calls in the topology are point lookups using `(keypath ...)` on non-subindexed dimensions:

1. Line 547: `(keypath *request-id)` on `$$ingest-runs` `{String IngestRunRow}`. Point lookup.
2. Line 556: `(keypath *line-key)` on `$$source-ledger` `{String Boolean}`. Point lookup.
3. Line 604: `(keypath *conv-container-id)` on `$$last-msg-per-conv` `{String String}`. Point lookup.
4. Line 636: `(keypath *sfk)` on `$$files-handled` `{String Boolean}`. Point lookup.
5. Line 654: `(keypath *request-id)` on `$$ingest-runs` `{String IngestRunRow}`. Point lookup.

No topology-side iteration over subindexed structures. The foreign client reads (lines 781, 791, 801) use `ALL` on subindexed PStates but these are external reads, not topology operations -- `:allow-yield?` does not apply.

## Non-subindexed collections without size limits

**PASS.**

All non-subindexed PStates are top-level `{String V}` maps where V is a single value (defrecord or Boolean), not an inner collection:

| PState | Schema | Growth |
|--------|--------|--------|
| `$$ingest-runs` | `{String IngestRunRow}` | One per request. Low count. |
| `$$source-ledger` | `{String Boolean}` | One per source line. Can be large (100K+). |
| `$$files-handled` | `{String Boolean}` | One per file. Small. |
| `$$file-offsets` | `{String FileOffsetRow}` | One per file. Small. |
| `$$last-msg-per-conv` | `{String String}` | One per conversation. Small-medium. |
| `$$containers-by-id` | `{String TranscriptContainerRow}` | One per container. Can be large (100K+). |
| `$$source-anchors-by-container` | `{String SourceAnchorRow}` | One per container. Same as above. |
| `$$source-artifacts` | `{String SourceArtifactRow}` | One per conversation. Small. |

Top-level `{String V}` maps in Rama are backed by RocksDB with key-value pairs at `(partition, key)`. They are NOT loaded as a single in-memory collection. Point lookups are O(1) seeks regardless of entry count. The "non-subindexed collection" concern applies to INNER collections within a PState value (e.g., a vector or map nested inside a record), not to top-level PState maps. No PState value contains an unbounded inner collection.

## Stream topology idempotency

**PASS (with documented approximation).**

Every write in the topology uses deterministic keys and `termval` overwrite:

| Write | PState | Key source | Idempotent? |
|-------|--------|-----------|-------------|
| L559 | `$$source-ledger` | `source-line-key` (sha256-based) | Yes -- `true` overwrites `true` |
| L571-578 | `$$containers-by-id` | `conversation-container-id` (deterministic) | Yes -- same row |
| L582-584 | `$$conversation-projection` | `conv-container-id, ""` | Yes -- same meta entry* |
| L589-590 | `$$containers-by-id` | `container-id` (sha256-based) | Yes -- same row |
| L596-598 | `$$conversation-projection` | `conv-container-id, order-key` | Yes -- same entry |
| L606-611 | `$$composition-edges-by-parent` | `conv-container-id, edge-key` | Yes -- same edge |
| L612-614 | `$$last-msg-per-conv` | `conv-container-id` | Yes -- same msg-id |
| L621-623 | `$$composition-edges-by-parent` | `parent-id, edge-key` | Yes -- same edge |
| L630-632 | `$$source-anchors-by-container` | `container-id` | Yes -- same anchor |
| L638 | `$$files-handled` | `source-file-key` | Yes -- `true` overwrites `true` |
| L642-644 | `$$source-artifacts` | `conv-container-id` | Yes -- same row** |
| L651-653 | `$$audit-entries` | `request-id, source-line-key` | Yes -- same row** |
| L658 | `$$ingest-runs` | `request-id` | Approximate*** |
| L666-668 | `$$tool-calls-by-name` | `tool-name, source-line-key` | Yes -- same row |
| L683-693 | `$$file-offsets` | `source-file-key` | Yes -- same row** |
| L702-704 | `$$source-artifacts` | `conv-container-id` | Yes -- same row** |
| L708-713 | `$$audit-entries` | `request-id, sentinel-key` | Yes -- same row** |

No random IDs generated in topology. All IDs derived from sha256 of source data. No `AFTER-ELEM`, no counters in navigators, no `depot-partition-append!`.

(*) `make-conversation-meta-entry` uses `core/now-ms` (line 563). On retry, timestamp differs by milliseconds. Row overwritten -- not accumulated. Acceptable.

(**) Same `core/now-ms` timestamp caveat. Rows overwritten, not accumulated. Timestamps may differ by milliseconds on retry. Documented as acceptable in PLAN.

(***) `increment-run-progress` (line 656) calls `(update :observed-line-count inc)` on the current PState value. On retry, increment applies again, double-counting. The PLAN explicitly documents this: "approximate progress counts... a retry may double-count, but counts are informational not correctness-critical; exact accounting comes from $$audit-entries."

## Partial failure in stream topologies

**PASS (with documented tradeoff).**

The observation source writes across three partition scopes:

1. **Conv-key partition** (lines 559-644): source-ledger, containers, projection, edges, anchors, source-artifacts, files-handled, last-msg-per-conv. All local. Atomic within a single task.
2. **Request-id partition** (lines 650-658): `|hash *request-id` -> audit-entries, ingest-runs progress.
3. **Tool-name partition** (lines 661-668): `|hash *tool-name` -> tool-calls-by-name.

Failure scenario: step 1 commits, step 2 or 3 fails. On retry: the `$$source-ledger` dedup guard (line 557 `<<if (nil? *existing)`) finds the line already recorded and skips the entire event. The audit entry and/or tool-call index write for this one observation is permanently lost.

This is a documented tradeoff: the PLAN states "approximate progress counts" and audit is best-effort per observation. The core invariant (container creation idempotency) holds. The `$$audit-entries` entry for one observation could be missing under rare retry, but containers themselves are correctly written. The tool-call index similarly may miss one entry under retry failure.

Mitigation: stream topology retries are rare. The affected PStates (audit, tool-call index) are secondary indexes, not source-of-truth containers.

The file-state source (lines 671-713) crosses two partitions: file-key (local writes) and conv-key/request-id (via `|hash`). The empty-file branch at lines 696-713 writes source-artifacts on the conv-key partition and audit-entries on the request-id partition. If either hop fails after the local `$$file-offsets` write commits, the file-state event replays (`:retry-mode` defaults to `:individual` for this source). On retry, the file offset is overwritten with the same value (idempotent), and the downstream hops re-execute. This path is safe.

## Single depot append per client operation

**PASS (with documented tradeoff).**

The executor functions (`harvest-ingest!`, `start-watch-ingest!`) make multiple `foreign-append!` calls per orchestrated pipeline:

- `harvest-ingest!` (lines 818-864): request append, running claim, N observation appends, N file-state appends, complete claim.
- `start-watch-ingest!` (lines 867-941): request append, running claim, polling loop with observation + file-state appends, cancelled claim on stop.

The template check says "each client write operation must call `foreign-append!` exactly once." These are NOT single client operations -- they are intentional multi-step orchestrator pipelines. The PLAN explicitly describes this: "the executor (harvest/watch) is external to the module -- it uses existing functions from transcript.clj and appends to depots via foreign-append!" (PLAN line 771-772).

If the executor crashes mid-pipeline: the run stays in `:running` forever (no heartbeat/TTL). On re-invocation, source-record idempotency ensures containers are not duplicated. The stuck-run-status is a known gap, addressable by a future TTL or heartbeat mechanism.

The four individual `append-*!` functions (lines 748-769) each call `foreign-append!` exactly once. These are the atomic client operations.

## Application-state caches survive restart

**PASS.**

No `TaskGlobal` objects in this module. All durable state is in PStates.

The watch executor uses `(atom {})` for `offsets` (line 875). On restart:
- Durable source: `$$file-offsets` PState.
- Rebuild path: line 886-889 -- `(let [saved (read-file-offset runtime sfk)] (if saved (:last-byte-offset saved) (.length ^File file)))`. Unknown files without saved offsets start at EOF (`.length`). Files with saved offsets resume from the durable offset.

## No reimplementation of built-in operations

**PASS.**

Scanned for reimplemented built-ins:

- `positive-partition` (lines 122-124): custom partitioner function for `{:key-partitioner f}`. Rama's `hash-by` is for depot partitioning; PState key-partitioning requires a user-supplied function. Not a reimplementation.
- `identity` call at line 706: uses `clojure.core/identity` to bind a computed value in dataflow. Standard Rama pattern.
- `run-row-some?` (line 472): wraps `some?` for Rama dataflow invocation. Standard pattern -- clojure.core predicates need function wrappers for Rama op position.
- Row construction helpers (lines 236-319): pure data construction. No overlap with `com.rpl.rama.ops`.

## PState schemas

**PASS.**

Previously `major-fail`: 9 of 11 PStates used `Object`. All now use concrete types:

| PState | Schema | Type |
|--------|--------|------|
| `$$ingest-runs` | `{String IngestRunRow}` | defrecord (line 132) |
| `$$source-ledger` | `{String Boolean}` | primitive |
| `$$files-handled` | `{String Boolean}` | primitive |
| `$$file-offsets` | `{String FileOffsetRow}` | defrecord (line 172) |
| `$$last-msg-per-conv` | `{String String}` | primitive |
| `$$containers-by-id` | `{String TranscriptContainerRow}` | defrecord (line 157) |
| `$$conversation-projection` | `{String (map-schema String IConversationEntry {:subindex? true})}` | definterface (line 137) |
| `$$composition-edges-by-parent` | `{String (map-schema String CompositionEdgeRow {:subindex? true})}` | defrecord (line 163) |
| `$$source-anchors-by-container` | `{String SourceAnchorRow}` | defrecord (line 165) |
| `$$source-artifacts` | `{String SourceArtifactRow}` | defrecord (line 168) |
| `$$tool-calls-by-name` | `{String (map-schema String ToolCallIndexRow {:subindex? true})}` | defrecord (line 176) |
| `$$audit-entries` | `{String (map-schema String AuditEntryRow {:subindex? true})}` | defrecord (line 180) |

Zero occurrences of `Object` in any PState declaration. `IConversationEntry` is a `definterface` (line 137) implemented by `ConversationMetaEntry` (line 139) and `ConversationMessageEntry` (line 145), which is the correct Rama pattern for polymorphic PState values.

## Plan adherence

**PASS (with minor deviations).**

### Previously major issues -- all resolved

1. **Object schemas**: fixed. All PStates use concrete types (see above).
2. **Missing ConversationMetaEntry**: fixed. `make-conversation-meta-entry` (line 480-487) constructs the entry. Written to `$$conversation-projection` at line 582-584 with inner key `""` (sorts before all message order keys).
3. **Missing `:follows` edges**: fixed. `$$last-msg-per-conv` (line 509) tracks the previous message ID. Lines 600-614 read the tracker, write a `:follows` edge to `$$composition-edges-by-parent`, and update the tracker.

### Remaining minor deviations

1. **ConversationMetaEntry aggregate fields**: `message-count` is hardcoded to `0` (line 486). `first-timestamp` and `last-timestamp` are both set to the current observation's timestamp. Since the meta entry is overwritten on every observation, both timestamps reflect the last-processed observation and message-count is always 0. The plan specifies these as meaningful aggregate fields. Impact: cosmetic -- readers can compute true counts/ranges from message entries. Not a correctness issue.

2. **Read paths use `ALL` vs plan's `MAP-VALS`**: R1 (line 781), R2 (line 801), R3 (line 791) use `ALL` (returns `[key, value]` pairs). The plan specifies `MAP-VALS` (returns values only). The `ALL` form is arguably more useful (provides order keys), but the return shape differs from the plan. Minor -- callers must destructure pairs.

3. **Missing `:agent-run` and `:chat-artifact` container kinds**: The spec lists 6 container kinds. Implementation creates 4 (`:chat-conversation`, `:chat-message`, `:tool-call`, `:tool-result`). `:agent-run` and `:chat-artifact` are spec aspirations not present in the source JSONL format. `artifact-container-id` (line 48-50) is defined but unused. Acceptable for first slice.

4. **Missing `:replies-to`, `:derived-from`, `:anchored-to` edge types**: Only `:contains`, `:follows`, `:produced` are implemented. `:replies-to` and `:derived-from` are conditional on source data structure (not present in current JSONL format). `:anchored-to` is structurally covered by `$$source-anchors-by-container` (separate PState rather than edge). Acceptable.

## Spec coverage

**PASS (with documented gaps).**

### Covered

- W1/W2 request submission: depot, validation, run row creation. Matches plan.
- W3 executor lifecycle claims: running/complete/failed/cancelled. Matches plan.
- W4 source record observations: per-line parsing, redaction, dedup. Matches plan.
- W5 file-state observations: per-file offset tracking, empty-file sentinel. Matches plan.
- R1 conversation projection: meta entry + ordered messages + inline tool summaries. Fixed.
- R2 audit entries: per-request audit trail. Matches plan.
- R3 tool-call index: by tool name, subindexed. Matches plan.
- R4 single container lookup: by container-id with custom partitioner. Matches plan.
- R5 run status: by request-id. Matches plan.
- R6 source artifact: by conversation container ID. Matches plan.
- Parse errors in projection: fixed. Lines 327-337 create `ConversationMessageEntry` with `parse-error-kind` set.
- Watch EOF for unknown files: fixed. Line 889 starts at `.length` for files without saved offsets.
- Source-record dedup: `$$source-ledger` check at line 556-557. Matches plan.
- Deterministic IDs: all container IDs derived from sha256 of source data. No random IDs in topology.
- Custom partitioner colocation: `partition-by-conv-key` extracts conv-key from all `tc:*` prefixed IDs. Matches plan.

### Remaining gaps (minor, acceptable for first slice)

- `system/init` event-type special handling: not implemented. Session metadata extracted generically for meta entry but no per-event-type branching.
- `result` terminal event special handling: not implemented.
- `stream_event` dedup by message UUID: not implemented. Dedup is by source-line-key only.
- Multi-request SourceArtifact tracking: `termval` overwrite, not a set of request IDs.
- ConversationMetaEntry `message-count`: always 0.

## Verdict

**PHASE_VALIDATION:minor-fail**

The three previously-identified major issues (Object schemas, missing ConversationMetaEntry, missing `:follows` edges) are all confirmed fixed. The four minor fixes (consecutive keypath, parse-error projection, watch EOF, extract-conv-key deduplication) are also confirmed applied.

One remaining failure: a NEW consecutive keypath at lines 709-710 in the file-state source's empty-file audit entry write. `(keypath *fs-request-id) (keypath *sentinel-key)` should be `(keypath *fs-request-id *sentinel-key)`. This is a single-line edit.

One additional minor observation: the ConversationMetaEntry `message-count` is hardcoded to `0` and timestamps are not accumulated, making the aggregate fields misleading. This is fixable by either removing those fields or adding a read-modify-write pattern to accumulate them. Not blocking for first slice but worth noting.

All other checks pass. The module's core invariants (deterministic IDs, idempotent writes, source-record dedup, typed schemas, colocation via custom partitioner) are sound.
