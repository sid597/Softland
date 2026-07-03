# Implementation Validation — POST-FIX

<!-- Phase 4 (RETROSPECTIVE, post-fix re-validation). Module: src/app/server/rama/dogfood/space.clj
     (space-kernel-module), validated at HEAD on branch docs/current-mental-model-local.
     Independent adversarial re-trace against IMPLICIT_SPEC.md and PLAN.md under the skill's
     production rules. Default verdict = major-fail; lifted only by explicit line-level tracing.
     llm.clj + core.clj read READ-ONLY for cross-kernel obligations. All line numbers are as-built.
     Runtime evidence: dogfood_space_probe_test.clj — 1 test / 63 assertions / 0 failures (ran this
     session, exit 0). dogfood_space_test.clj covers the functional spec surface (S1–S13 behaviors).
     Fix-session scope from the orchestrator is honored: in-scope failures must hold; deferred residue
     is flagged, not counted. -->

Semantics relied on (cited):
- Stream retry replays a record from the `source>` block start even after PState writes committed; default retry mode `:individual` is at-least-once (`references/stream.md`).
- Transaction scope = between partitioners. All writes between two partitioners commit as one atomic group on one task; the single-threaded task makes check-then-act within a segment safe (SKILL.md "Every task is single-threaded … reads and writes to any number of PStates on the same task are atomic").
- `decision-dedup-gate` / `write-if-absent` / `bounded-dead-letter` are the shared guard helpers in `core.clj:576-675`, read this session.
- The LLM kernel is now a **microbatch** topology (`llm.clj:1463`) — its request/claim/obs/control folds are each one cross-partition exactly-once transaction.

---

## Architectural diff vs the pre-fix module

The pre-fix module computed a decision, then performed a long read-modify-write fan-out where the dedup signals (`$$send-by-idempotency`, turn-order) were read early and written ~10 partitioner hops later. The post-fix module replaces that with a **journal-entry-first fold**:

1. `(|hash *thread-id)` (space.clj:1505) enters **Segment 1** on the space task.
2. Reads journal, runs `core/decision-dedup-gate`, reads `$$spaces` / `$$turns-by-space` / (`$$projection-chat-canvas` for create, `$$send-by-idempotency` for compose), then a per-type `plan-*` pure fn computes a **write PLAN** (`:journal-entry :spaces-row :turn-order :canvas :idem-row :emit :record-request?`).
3. The five Segment-1 writes (journal 1535, spaces 1538, turns-by-space 1541, canvas 1544, send-by-idempotency 1548) are all `local-transform>` keyed by `*thread-id` with **no intervening partitioner**. The first partitioner after Segment 1 is `(|hash *request-id)` at 1555.
4. Downstream (`emit-fan-out?`-gated, 1565) re-emits the full fact family on every journal replay; every downstream write is an idempotent `termval` of replay-stable values, a `write-if-absent`, or a consumer-deduplicated depot append.

This is exactly the PLAN's Segment-1 design. I verified by line-trace that no partitioner is hidden inside the `<<cond` plan computation (1510-1530: only `local-select>` and pure `plan-*` calls) — Segment 1 is one transaction scope.

---

## Template checks

### Redundant conditionals
**PASS.** The pre-fix negated-pair `<<if (idempotency-hit?...)` / `<<if (not (idempotency-hit?...))` is gone. The six duplicated decision-write trios are gone — there is now exactly ONE decision write (1557-1559) shared by all branches, fed by `(get *plan :decision)`. The Segment-1 writes are uniform `(<<if (some? *x) (local-transform> ...))` guards (1534-1548), each writing a distinct PState; no branch does the same op with only a variable differing. `interpret-control-turn` (624-628) still evaluates `(validate-or-reject request)` twice (cond test + result) — a pure double-call, micro-waste, not a redundant-conditional violation of the check's shape (different branches, not identical ops). Not a fail.

### Consecutive keypath
**PASS.** `grep -E '\(keypath …\) \(keypath'` returns zero hits. Every multi-segment navigation uses the combined `(keypath *a *b)` form (e.g. journal 1535, send-idempotency 1548, artifact-graph 1655/1662/1764). The pre-fix consecutive single-arg keypaths were collapsed.

### Select-compute-transform
**PASS (with one bounded, justified read-modify-write).** The relations-projection still does `local-select> $$projection-object-relations` → `add-projection-out-edge` → `local-transform> termval` at three sites (thread-object edge accumulation: 1652-1654, 1761-1763, 1797-1799; raw-source edge: 1832-1834, 1864-1866, 1896-1898). This is **not** replaceable by `+compound`/aggregator here: the value being accumulated is a nested map `{:out {edge-id edge} :in {…}}` and the write must MERGE a new edge into an existing projection that may already hold edges from a prior request (the space-object accumulates one out-edge per turn). A blind `termval` of a freshly-built single-edge projection would clobber prior edges — the select-merge-write is load-bearing for correctness, and the read is one seek on the object's own partition. The turn/bundle/run-object relations are built in full per request (`turn-object-relations-projection` etc., 1624-1632) and written with a plain `termval` (no read) — correct. Pointer indexes `$$context-bundles-by-turn` (1643) / `$$llm-run-by-turn` (1644) still duplicate fields on the turn row; this is an I/O-efficiency style note, unchanged from pre-fix, not a correctness fail and out of the fix scope.

### Unnecessary nil->val
**PASS.** No `nil->val` anywhere in space.clj. Nil handling lives in Clojure helpers (`write-if-absent`, `add-projection-*-edge`, `resolve-patch-proposal`), not in paths.

### :allow-yield?
**PASS (with deferred pointer).** No `local-select>`/`select>` in the topology uses a range/ALL/MAP-VALS navigator — every read is a whole-value point read by `keypath`. So `:allow-yield?` does not apply. The NEW subindexed inner maps (`$$space-request-journal`, `$$send-by-idempotency`) are read by a **two-arg keypath point lookup** (`[(keypath *thread-id *request-id)]` 1506, `[(keypath *thread-id *idempotency-key)]` 1517) — a single inner-key seek, not an iteration, so no yield needed. The unbounded *flat-vector* reads (`$$turns-by-space` 1509) are whole-value deserializations, not subindexed iterations — they have nothing to yield over; that is the deferred SP-09 residue, not an :allow-yield? miss.

### Non-subindexed collections without size limits
**FAIL — but entirely DEFERRED (SP-09), zero in-scope content.** No code caps:
- `$$turns-by-space` (1472, `{String Object}`) — flat per-space turn-id vector, `termval`-rewritten wholesale (1541). Unbounded (spec: hundreds–thousands of turns). O(n) `conj-distinct` scan + full serialize per turn.
- `$$projection-chat-canvas` (1492) — embeds the full `:turn-order` vector (`chat-canvas-projection` 1102-1112), a second unbounded copy rewritten per turn.
- `$$space-graph` (1470), `$$artifact-graph`/`$$artifact-graph-in` (1484-1485), `$$projection-object-relations` (1494) — unbounded `{id → edge}` maps, no cap.
- `$$space-obs-dead-letters` (1491) and the LLM dead-letters ARE bounded (`obs-dead-letter-limit 100`, 1446/1454).

The orchestrator's DEFERRED list names exactly these ($$turns-by-space flat vector, canvas embedded turn order, artifact graphs) as out-of-scope residue, and notes the NEW `$$space-request-journal`/`$$send-by-idempotency` inner maps ARE subindexed (verified: 1467-1468, 1477-1478). So this check fails only on pre-existing PStates the fix session deliberately did not restructure. Flagged as deferred residue; not an in-scope failure.

### Stream topology idempotency
**PASS (in-scope), with the dispatch/control dedup now living at the consumer.** One stream topology (1496-1937), default `:individual` retry. Trace every write/side-effect under record retry:

1. **Segment-1 writes are journal-gated, not idempotent-by-luck.** A journal hit means the whole Segment-1 group committed (atomic), so replay takes `plan-replay` (1234-1241) → `:emit :full`, `:journal-entry nil`, all Segment-1 write-vars nil → the five `(<<if (some? …))` guards all skip; only downstream re-emits. The non-idempotent operations (turn-count = `(count order)`, turn-order append) are computed ONCE in Segment 1 and pinned in the journaled decision; replay never recomputes them. **SP-01/SP-02/SP-04 closed.**
2. **Create retry no longer flips `:space/created` ↔ `:space/touched`.** First attempt journals the decision (with the `:space/created` event) atomically with `$$spaces`. Redelivery hits the journal, the gate returns `:replay` with the STORED decision (`decision-dedup-gate` 596-598), and `plan-replay` re-emits the stored event verbatim — no re-interpretation against now-non-nil `$$spaces`. The `$$space-events-by-id` write (1576) uses the stored `*thread-event` from the stored decision. Audit trail stable. **Pre-fix Stream-idempotency item-1 (create) closed.**
3. **Fork/compose dispatch redelivery is consumer-deduplicated.** The LLM intake is now microbatch and gates on `$$llm-decisions-by-run-id` with `decision-dedup-gate` (llm.clj:1505-1508) then `(filter> (= :proceed *gate-status))`. A redelivered `*llm-request` (same `<req>/llm-request` id, same run id) is classified `:replay`, filtered out → **no run-row clobber, no `:pending` regression, no inbox re-add, no re-spawn.** P1 probe asserts a redelivered dispatch leaves a `:succeeded` run untouched (claim token preserved). **Pre-fix Stream-idempotency item-3 closed.**
4. **Control redelivery no longer duplicates compaction/steer history.** The control append (1772) re-emits on journal replay, but `fold-control` (llm.clj:1415-1421) now guards on `controls-by-id` containing the control id → duplicate delivery returns the row unchanged, so `add-compaction`/`add-steer` never double-`conj`. **Pre-fix Stream-idempotency item-1 (control) closed.**
5. **Patch-proposal ingest redelivery cannot reset a resolution.** Proposal create is `write-if-absent` by proposal id (1909, 1936): `:pending` is only ever written into ABSENCE, so a redelivered ingest after resolution keeps the resolved row. Resolution is guarded by `resolve-patch-proposal` (1409-1420): blank id → nil; absent → nil; same resolving request id → nil (replay no-op); non-`:pending` status → nil (first-resolution-wins). **Pre-fix Stream-idempotency item-1 (patch) + S3 closed.** P7/P8 probes confirm.
6. **IDs.** All topology-side ids remain pure functions of the request (events 248-250, run/dispatch 416-426, control 483-487, materials 874-890). `random-id`/`now-ms` only in client builders. Satisfied.

### Partial failure in stream topologies
**PASS (in-scope).** The pre-fix poison was: a committed `$$send-by-idempotency` row mid-tree caused retry to take a decision-only branch and permanently skip the dispatch/catalog writes. That is structurally impossible now: the idempotency row is written in Segment 1 atomically with the journal entry (1548 alongside 1535). Either the whole Segment-1 group committed (→ journal hit → `plan-replay` re-emits the FULL downstream fan-out, 1561-1918) or none of it did (→ retry redoes Segment 1 fresh). There is no interleaving where the idempotency row is durable but the journal entry is not — they are in the same atomic group on the same task. The all-or-nothing fact-family invariant holds: transient absence is possible (downstream hops commit independently), permanent inconsistency is not, because a journal hit forces a complete re-emit on every replay until the tree completes. **SP-01 partial-application closed.** P1 (replay after the run finished) asserts the full fact family — bundle, dispatch, turn object, run object — is present after replay.

### Single depot append per client operation
**PASS.** `append-llm-observation!` (space.clj:2016-2026) now performs exactly ONE foreign append — `llm/append-observation!` (llm.clj:1777-1782, a single `foreign-append!` to `*llm-obs-depot`). The second append to `*space-action-depot` is gone; patch proposals are derived SERVER-SIDE by the space topology sourcing `*llm-obs-depot` (1924-1937). A client crash can no longer strand a proposal between two appends. `append-space-action!` (2009-2014) is a single append. **Pre-fix single-append FAIL closed.**

### Application-state caches survive restart
**PASS.** space.clj declares no TaskGlobals, no `declare-object`, no in-process application state. All state is durable PStates (1462-1494). Topologies resume from persisted offsets on restart. Nothing to rebuild.

### No reimplementation of built-in operations
**FAIL (minor, pre-existing, out of fix scope).** `select-pstate-one` (2028-2030) — `(first (foreign-select path pstate))` — still reimplements `foreign-select-one`; every read helper routes through it. One-line-per-call-site style fix. Unchanged from pre-fix; not in the fix-session scope. Genuinely-shared helpers `conj-distinct`, `content-hash`, `canonical-str` do not duplicate built-ins.

---

## Spec conformance findings (re-traced against the NEW code)

**S1 — Idempotency check-and-claim is now atomic. PASS.** The `$$send-by-idempotency` read (1517) and write (1548) are both in Segment 1 on the `*thread-id` task; the single-threaded task serializes concurrent same-key sends — the first to run reads absent, claims the row, mints facts; the second reads the now-present row and takes `plan-compose-and-send`'s idempotency branch (1338-1345). "At most one set of facts is ever minted." The key is space-scoped (`[(keypath *thread-id *idempotency-key)]`) per PLAN A-keys. **Closes pre-fix S1.** P2 (same key/material → replay, fresh ids never materialize), P3 (same key/different material → conflict, original untouched), P4 (same key/different space → independent) all green.

**S2 — Concurrent space-only / control turns no longer drop turns. PASS.** Order/count/canvas are read (1509) and written (1541/1544) in the SAME Segment-1 scope; `plan-ordered-turn` (1258-1275) computes `order = (add-turn-id existing-turn-order turn-id)` and `count = (count order)` from the value read on this task, then the write commits before the next partitioner. No read→hop→write split exists for these PStates (grep confirms: writes only at 1541/1544, reads only at 1509/1512). **Closes pre-fix S2/SP-04.** P10 (5 rapid pipelined comment turns, none dropped, count = 5) green.

**S3 — Patch resolution lifecycle. PASS.** `resolve-patch-proposal` (1409-1420): no phantom rows under nil/unknown ids (blank-id → nil, absent → nil), first-resolution-wins (non-`:pending` → nil), replayed resolution is a no-op (same `:resolution/request-id` → nil). Retry of an earlier resolution can no longer flip a later one because the status guard plus the request-id guard make any second writer a no-op. **Closes pre-fix S3.** P6 (nil/unknown id → accepted turn, zero phantom rows) and P7 (first-resolution-wins, reject after accept is a no-op) green.

**S4 — Patch-proposal ingestion matches spec op 4. PASS.** Space now SOURCES `*llm-obs-depot` (1924). Only `:codex/patch-proposal` observations with a resolvable space id and proposal id become proposals (`bridgeable-patch-observation?` 1428-1432); they are written `write-if-absent` (1936) with **no decision and no turn** — the obs source block touches only `$$space-patch-proposals` and `$$space-obs-dead-letters`. Unroutable patch observations dead-letter (1925-1930, never-drop, bounded at 100). Non-patch observations fall through both `<<if`s untouched. **Closes pre-fix S4 (all three sub-paths).** P5 asserts: non-patch obs never becomes a proposal, ingest mints no space decision, turns unchanged.

**S5 — Fork-binding gate. PASS (unchanged, still holds).** `run-one-pending-with-adapter!` (llm.clj:2214-2258): claim attempted only `when-not (fork-binding-required? ...)`; a fork-binding-required run returns `:spawned? false :reason :fork-binding-not-durable` with no `claim-run!`, no adapter call, run left `:pending`. `fork-binding-required?` (452-456) is re-checkable and side-effect-free. The pre-fix one hole (redelivered dispatch resetting the run row and its binding) is closed by the intake decision gate (S-item 3 above).

**S6 — Bundle-freeze determinism under replay. PASS.** `context-bundle-row` (386-401) is a pure function of the request: refs rendered by id with no existence checks, options selected by allowlist (`bundle-option-keys` 353-355), timestamp from `:request/time-ms`; hash = `sha256(pr-str stable-material)`. The bundle is `termval`-written (1646) — byte-identical on replay; no mutation path. Foreign reuse of a bundle id by a different request would `termval`-clobber, but the request-id conflict gate (`decision-dedup-gate`) now rejects a different-payload request reusing the same request id BEFORE any fact write, and entity-id reuse across different request ids is spec Ambiguity 1 (open). The bundle itself is replay-stable.

**S7 — Determinism / rebuildability. PASS (in-scope determinism restored).** The two pre-fix interleaving leaks are closed: (a) create no longer flips created/touched under pipelining (journal-stored decision is re-emitted, not re-interpreted — S-item 2); (b) the S2 turn-drop race is gone, so canvas/turn-count are interleaving-independent; (c) same-key concurrent sends (S1) now mint one fact set. No wall-clock, randomness, or iteration-order leaks remain (times from payload; ids deterministic; `core/canonical-str` gives order-independent hashing for fingerprints and material hashes). The residual non-byte-identity of a full-snapshot replay (created-vs-touched event interleaving when two DIFFERENT requests create+send the same space, and object rows recomputed from current state) is the DEFERRED SP-08 residue named by the orchestrator, not an in-scope determinism break. The `projection-rebuildability-test` and `space-replay-equality` tests in dogfood_space_test.clj exercise the canonical-input replay equality.

**S8 — Projection purity (chat canvas). PASS.** `refreshed-create-canvas` (1277-1287): a duplicate/retried `space/create` against a space that already has turns refreshes only title/status/turn-count/updated-at on the EXISTING canvas — it never wipes `:turn-order` or `:latest-turn`. Only when no canvas exists does it build a fresh empty one. The pre-fix "create wipes canvas to `[]`" bug is closed. **Closes pre-fix S8.** P12 (duplicate create on a space with turns → turn order survives, count = 1) green.

**S9 — Always-readable / absent-not-error. PASS.** Every read helper (2032-2152) is a `keypath` point lookup returning nil on absence, with client-side empty defaults ([] 2056, {} 2061/2118/2123, empty relations projection 2136). Rejected requests gate all fact writes on `emit-fan-out?` (1565) which requires `decision-accepted?` and non-replay; rejected → decision-only.

**S10 — Space-only turns produce no LLM side effect. PASS.** `interpret-space-turn` (615-622) emits only a turn event; the space-turn fan-out branch (1774-1918) writes turn/objects/edges/materials but NO bundle, dispatch, control, or `*llm-depot`/`*llm-control-depot` append. Rejection on missing space → `:space/not-found` (620). `bundle-by-turn`/`run-by-turn` stay absent (turn row's `:context-bundle/id` is nil, 1778).

**S11 — Control turns. PASS (discrimination still load-bearing-correct).** `request-control-type` (473-481) dispatches on `:request/type` alone. The Segment-1 `<<cond` (1523 control before 1526 space-turn) and the fan-out `<<cond` (1728 control before 1774 space-turn) both order the control case first, so the overlapping keywords (`space-turn-request-types` 68-69 still contains the control types) resolve to the control branch. Control id `<req>/llm-control` deterministic; decision carries `:llm-control/type`; `$$llm-controls`+`$$llm-control-by-turn` written (1755-1757); native correlation id passed through `control-record` (llm.clj:617). Patch resolutions carry no control fields (S10 path). LLM-side `fold-control` now has the terminal-state guard the pre-fix note flagged as missing: `cancel-run` (llm.clj:1382-1395) keeps a `terminal-run-row?` unchanged, `resolve-approval` (1326-1380) is resolve-if-pending. **The cross-kernel terminal-guard gap noted pre-fix is closed.** P9 (never-requested approval → `:approval/unknown` audit error, run unmoved, no invented approval row) green.

**S12 — Fork. PASS for in-scope obligations; documented residue.** Lineage bidirectional (child `:parent-space/id` via `thread-row`; parent `$$space-graph` edge 1709-1712). Anchor slice immutability via `write-if-absent` (1721) — and the plain slice-create branch ALSO uses `write-if-absent` now (1815), closing the pre-fix "plain slice overwrites unconditionally" divergence. Fork dispatch to `*llm-depot` is consumer-deduplicated (intake gate). Residue (out of scope, flagged): fork materializes no child chat-canvas (explicit `:canvas nil` in `plan-fork-from-span`, commented at 1361 — orchestrator's deferred "fork child canvas not materialized"); parent-existence is not pre-checked (spec Ambiguity 6, open); anchor `slice:<id>` ref rendered only if the caller supplies it in `:refs`.

**S13 — `:space/reconcile`. STILL minor.** It is in `space-request-types` (57-60) so validation passes, but no `<<cond` case matches → `(default>)` → `plan-invalid-type` mints `:rejected :request/type-invalid` (1391-1397). A validator-legal type rejected as type-invalid is an inconsistent contract surface. Pre-existing, out of fix scope, minor.

---

## Falsification pass

**Architecture.** One stream topology owns 25 PStates plus the NEW `$$space-request-journal` (subindexed dedup spine) and `$$space-obs-dead-letters` (bounded never-drop ledger). Cross-kernel hand-off via three mirror depot appends (`*llm-depot` dispatch, `*llm-control-depot` control, and SOURCING `*llm-obs-depot` for the observation bridge). The fix added: journal-entry-first Segment-1 fold, the `plan-*`/write-PLAN indirection, `decision-dedup-gate` fingerprint classification (proceed/replay/conflict), `send-material-hash` for idempotency conflict-vs-replay, `write-if-absent` on all initial-insert rows, and the typed server-side observation bridge. The LLM kernel was converted stream → microbatch for cross-partition exactly-once.

**Failure modes attempted (against the NEW code):**
1. Retry after the idempotency row committed → re-emits the full fan-out (journal hit forces `plan-replay :emit :full`) — **HELD (closed).** Was the pre-fix permanent-loss bug.
2. Concurrent same-key sends → second reads the claimed row in Segment 1, takes the replay/conflict branch — **HELD.** P2/P3.
3. Concurrent space-only turns (5 pipelined) → order/count read+write in one segment, none dropped — **HELD.** P10.
4. Fork/any dispatch redelivery → intake decision gate filters `:replay`, no run-row reset, no re-spawn — **HELD.** P1.
5. Proposal ingest redelivery after resolution → `write-if-absent` keeps the resolved row — **HELD.** P8.
6. Patch-accept with nil/unknown id → `resolve-patch-proposal` returns nil, no phantom row — **HELD.** P6.
7. Create retry → stored decision re-emitted, no created↔touched flip — **HELD.** P1-class.
8. Duplicate create on a space with turns → `refreshed-create-canvas` preserves order — **HELD.** P12.
9. Duplicate request id + conflicting payload → gate `:conflict`, committed decision/request/facts untouched, conflict decision under `/conflict` id, `record-request? false` — **HELD.** P11.
10. Control redelivery → `fold-control` control-id guard, no duplicate compaction/steer — **HELD.**
11. Never-requested approval resolve → `resolve-approval` audit error, run unmoved — **HELD.** P9.
12. Fork-binding premature claim → gated, no side effect — **HELD.**
13. Bundle replay → pure + deterministic hash, byte-identical — **HELD.**

**Writers / readers / clearers (changed state).**
- `$$space-request-journal` — written once per request in Segment 1 (1535), keyed `(thread-id, request-id)`; read by the gate (1506); never cleared (correct — audit is forever-readable). Sole writer is Segment 1.
- `$$spaces` / `$$turns-by-space` / `$$projection-chat-canvas` — written ONLY in Segment 1 (1538/1541/1544) on the `*thread-id` task by all five request branches; the single-threaded task + single-segment write is the multi-writer resolution (last-writer races are impossible within one event; cross-event ordering is depot append order on the space task). Never cleared.
- `$$send-by-idempotency` — written if-claimed in Segment 1 (1548); read in Segment 1 (1517); space-scoped key; never cleared.
- `$$space-patch-proposals` — written by the obs bridge (`write-if-absent`, 1936), by the request-side proposal-create branch (`write-if-absent`, 1909), and by both resolution types (`resolve-patch-proposal` guarded, 1916). Three writers, all guarded so none can reset truth: create-if-absent for `:pending`, first-resolution-wins for transitions. Never cleared.
- `$$space-decisions-by-id` — committed decision under `<req>/decision`; conflict decision under `<req>/decision/conflict` (distinct key, can never alias). `record-request?` gates the request-row write so an impostor never overwrites the original request.
- LLM-side `$$llm-turn-runs` — intake writes only on `:proceed` (gate), so redelivery never resets; claim/obs/control folds are guarded (sticky terminals, resolve-if-pending). Single-owner microbatch.

**Async ordering risks.** Decision/request rows commit one partitioner after Segment 1; downstream fact rows commit in later independent groups. A caller acting on the decision alone can momentarily see a fact not yet materialized — tolerated by the spec's await-per-fact contract (tests poll). No torn state where a turn is in the order but the order's own write half-committed: order+count+canvas are one atomic Segment-1 group. The pre-fix S1/S2/S7 ordering races are eliminated.

**Error-path cleanup.** No in-flight flags, locks, or TaskGlobals exist to leak. The pre-fix inverse gap (nothing re-emits lost downstream effects after a poisoned retry) is closed: a journal hit unconditionally re-emits the full downstream fan-out on every replay, and `:ack`/`:append-ack` hold the caller until the tree completes.

**Open doubts.**
- (a) `decision-dedup-gate` fingerprints the FULL request including `:request/time-ms`. At-least-once depot redelivery replays the byte-identical record, so the fingerprint matches and the gate returns `:replay` — correct. But a CLIENT that re-mints `:request/time-ms` (or any envelope field) under the same request id would be classified `:conflict`, not `:replay`. `with-request-fingerprint`'s own docstring (core.clj:545-546) says such callers "should dissoc those fields before fingerprinting"; the space topology does not dissoc. This is a client-contract sharpening, not a fault-tolerance hole (true redelivery is byte-identical), and it fails CLOSED (conflict-rejected, original untouched), so it is not an in-scope failure — flagged for the client contract.
- (b) `$$turns-by-space` whole-vector rewrite cost on the interactive send path grows O(n) with turn count — the DEFERRED SP-09 scaling residue, not a correctness fault.

---

## Pre-fix counted-failure disposition

| Pre-fix failure | Disposition | Closing mechanism (file:line) |
|---|---|---|
| SP-01 retry → permanent partial fact-family loss | **CLOSED (in-scope)** | journal-gated Segment 1 + full re-emit on replay — space.clj:1505-1548, 1561-1566; emit-fan-out? 1399-1407 |
| SP-02 unstable decisions/events under retry | **CLOSED** | gate returns stored decision, plan-replay re-emits it — core.clj:592-600; space.clj:1234-1241, 1576 |
| SP-03 cross-module dispatch no consumer dedup | **CLOSED** | LLM intake decision-dedup-gate + filter :proceed — llm.clj:1505-1511 |
| SP-04 concurrent split read-modify-write drops turns | **CLOSED** | order/count/canvas read+write in one Segment-1 scope — space.clj:1509/1541/1544; P10 |
| Stream-idem: create created↔touched flip | **CLOSED** | replay re-emits stored event, no re-interpret — space.clj:1234-1241; core.clj:596-598 |
| Stream-idem: control duplicate compaction/steer | **CLOSED** | fold-control control-id guard — llm.clj:1417-1421 |
| Stream-idem: patch ingest resets resolution | **CLOSED** | write-if-absent + resolve guard — space.clj:1909/1936, 1409-1420 |
| Partial-failure poison (idempotency-row mid-tree) | **CLOSED** | idem-row in Segment 1 atomic with journal — space.clj:1548; P1 |
| Single-append (dual foreign-append!) | **CLOSED** | server-side obs bridge, single append — space.clj:2016-2026, 1924-1937 |
| S1 non-atomic check-then-act | **CLOSED** | space-scoped key check-and-claim in Segment 1 — space.clj:1517/1548; P2/P3/P4 |
| S2 concurrent turn drop | **CLOSED** | same as SP-04 |
| S3 patch resolution phantom/no-guard | **CLOSED** | resolve-patch-proposal — space.clj:1409-1420; P6/P7 |
| S4 ingestion contradicts op 4 | **CLOSED** | typed server-side bridge, no decision/turn — space.clj:1924-1937; P5 |
| S7 determinism interleaving leaks | **CLOSED (in-scope)** | S1/S2/create races all closed; SP-08 byte-identity residue DEFERRED |
| S8 create wipes canvas | **CLOSED** | refreshed-create-canvas — space.clj:1277-1287; P12 |
| S11 LLM-side terminal-guard gap | **CLOSED** | cancel-run/resolve-approval sticky terminals — llm.clj:1382-1395, 1326-1380; P9 |
| Non-subindexed unbounded collections | **DEFERRED (SP-09)** | $$turns-by-space flat vector, canvas turn-order, artifact graphs uncapped — schema redesign out of scope |
| SP-08 full-snapshot byte-equality on replay | **DEFERRED** | created-vs-touched interleaving + object rows recomputed from current state |
| SP-10 hygiene leftovers | **DEFERRED** | select-pstate-one reimplements foreign-select-one (2028-2030); pointer-index duplication |
| Observation claim-token verification on bridge | **DEFERRED** | space cannot synchronously read LLM claim truth |
| Fork child canvas not materialized | **DEFERRED** | plan-fork-from-span :canvas nil (space.clj:1361) |
| S13 :space/reconcile validator/decision mismatch | **STILL minor (out of scope)** | validates but default-rejects type-invalid — space.clj:1391-1397 |

No in-scope failure remains STILL OPEN.

---

## Verdict

**conditional-pass** — every in-scope mechanism holds under line-level tracing and is corroborated by the green probe matrix (63/63): the journal-entry-first atomic Segment-1 fold closes the retry/partial-failure/concurrency family (SP-01..04, S1, S2, S7, S8), idempotency conflict semantics are explicit and space-scoped, the typed server-side observation bridge replaces the dual-append and matches spec op 4, proposal-lifecycle and approval guards prevent phantoms/resets/first-resolution-loss, and the cross-kernel dispatch/control/terminal obligations are consumer-deduplicated at the (now-microbatch) LLM intake and folds. The remaining failures are exclusively the DEFERRED residue the fix session scoped out — unbounded non-subindexed pre-existing PStates (SP-09), full-snapshot byte-equality (SP-08), `select-pstate-one`/pointer-duplication hygiene (SP-10), bridge claim-token verification, fork child canvas, and the minor `:space/reconcile` surface — none of which is an in-scope mechanism.

conditional-pass
