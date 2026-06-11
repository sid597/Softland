# Implementation Validation

<!-- Phase 4 (RETROSPECTIVE). Module: src/app/server/rama/dogfood/space.clj (space-kernel-module).
     Validated adversarially against IMPLICIT_SPEC.md and PLAN.md under the skill's production rules.
     llm.clj read read-only to trace cross-kernel obligations (dispatch dedup, fork-binding gate,
     control folding). All line numbers refer to the files as of this validation.
     Retro note: fail verdicts do not trigger a fix loop; this artifact and verdict ARE the result. -->

Semantics relied on (cited):
- Stream retry replays a record from the `source>` block start, **even after all PState writes committed**, if progress tracking fails afterward (`references/stream.md:9`, `:90-91`).
- Default stream retry mode is `:individual` (`references/stream.md:158`). The space source sets none (space.clj:1198), so `:individual` applies — at-least-once.
- Transaction scope = between partitioners. Writes between two partitioners commit as one atomic group at the partitioner boundary; groups on different tasks commit independently; committed groups are NOT rolled back when a later segment fails (`references/core-concepts.md:10`, `references/stream.md:96`, "When PState writes commit").
- Partition ordering is only pairwise per sender/receiver task pair (`references/stream.md:31`); events traveling via different intermediate tasks, and pipelined records, interleave arbitrarily between segments.
- Mirror depot appends route to the source module; `:append-ack` confirms durable append only (`references/mirrors.md:6,22`).

---

## Redundant conditionals

**Check:** if every branch of an `<<if`, `<<cond`, or `<<switch` does the same operation with only a variable differing, replace with a single operation using that variable directly.

**FAIL.**
- space.clj:1235-1244 — the compose branch uses `(<<if (idempotency-hit? *idempotency-row) ...)` immediately followed by `(<<if (not (idempotency-hit? *idempotency-row)) ...)`: a negated pair that should be one `<<if` with an else block. Both branches contain the identical three-op sequence `(decision-id *decision :> *decision-id)` → `(|hash *decision-id)` → `(local-transform> [(keypath *decision-id) (termval *decision)] $$space-decisions-by-id)` (1237-1239 vs 1242-1244).
- All six `<<cond` branches repeat that same decision-write trio with only `*decision` differing: 1210-1212 (create), 1242-1244 (compose), 1361-1363 (fork), 1419-1421 (control), 1481-1483 (space-turn), 1633-1635 (default). The trio is hoistable below the decision computation.
- space.clj:630-632 — `interpret-control-turn` evaluates `(validate-or-reject request)` twice (as `cond` test and as result). Pure but wasteful; same shape as the dataflow duplication above.

Runtime trace: every request type executes one of these branches; each duplicated trio compiles to identical partition hop + write. Behavior is correct; the structure is redundant. Fixable by restructuring the `<<cond` body — localized edits.

## Consecutive keypath

**Check:** `(keypath *a) (keypath *b)` → `(keypath *a *b)`.

**FAIL.** Consecutive single-arg keypaths appear throughout:
- space.clj:1332, 1334 (`$$artifact-graph`/`$$artifact-graph-in`, thread-turn edge), 1339-1340, 1342, 1344 (turn-bundle / turn-llm-run edges), 1349, 1351 (bundle-llm-run edge) — compose branch.
- space.clj:1401 — fork branch, `[(keypath *parent-thread-id) (keypath *event-thread-id) (termval *child-edge)]` on `$$space-graph`.
- space.clj:1469, 1471 (control branch), 1522, 1524 (space-turn branch), 1555, 1557 / 1585, 1587 / 1615, 1617 (raw-source edge writes in slice/comment/derivative sub-branches).

All should be `(keypath *a *b)`. Pure style/navigation efficiency; one-line edits.

## Select-compute-transform

**Check:** `local-select>` followed by computation followed by `local-transform>` with `termval` — replace with `+compound` and an aggregator, or a single transform, when possible.

**FAIL.** Recurrent pattern:
- Relations-projection read-modify-writes: space.clj:1329-1331, 1466-1468, 1519-1521 (`local-select> $$projection-object-relations` → `add-projection-out-edge` → `local-transform> termval`), and 1552-1554, 1582-1584, 1612-1614 (raw-object variants). Each is replaceable with a single no-read transform of the shape `local-transform> [(keypath *id) (nil->val (empty-object-relations-projection *id)) :out (keypath *edge-id) (termval *edge)]`.
- Fork slice keep-existing: space.clj:1409-1411 — select existing, `(or existing slice)` (`keep-existing-slice-row`, 573-575), transform. Replaceable with `(term (fn [cur] (or cur *slice-row)))` in one transform.
- Turn-order maintenance: select at 1272 / 1434 / 1491 → `add-turn-id` (`conj-distinct`, 707-716) → transform at 1314 / 1452 / 1512. Beyond the style issue, the 1491→1512 instance spans partitioner hops and is a correctness bug (see Spec conformance S2).

Also a skill-rule violation in the same family ("never trade I/O for simplicity"): pointer indexes `$$context-bundles-by-turn` (1318) and `$$llm-run-by-turn` (1319) duplicate fields already stored on the turn row (`turn-row` carries `:context-bundle/id`, 665-677) — extra writes and extra PStates for data answerable from `$$turns` in the same seek.

## Unnecessary nil->val

**Check:** do not add `nil->val` unless the next navigator requires a non-nil value.

**PASS.** No `nil->val` appears anywhere in space.clj. Nil-handling is done in Clojure helper fns (`thread-row` 651-663, `add-projection-*-edge` 1132-1142, `apply-patch-decision` 1086-1097), not in paths.

## :allow-yield?

**Check:** `local-select>` iterating a subindexed structure that can exceed ~100 entries needs `{:allow-yield? true}`.

**PASS (vacuously), with a pointer to the real defect.** No PState in the module is subindexed and no `local-select>` uses a range/ALL navigator — every read is a whole-value point read via `keypath` (e.g. 1205, 1234, 1272, 1409, 1627). `:allow-yield?` does not apply to single-value deserialization. The reason there is nothing to yield over — entire unbounded collections stored as single values — is the failure recorded in the next check, not a pass of this one's intent.

## Non-subindexed collections without size limits

**Check:** every write to a non-subindexed inner collection must have an explicit maximum size enforced in code; otherwise it must be subindexed.

**FAIL (major — schema restructuring required).** No code path caps any of the following, and the spec declares them unbounded:
- `$$turns-by-space` (declared 1178, `{String Object}`): a flat vector of turn ids per space, rewritten wholesale on every turn (1314, 1452, 1512). Spec op 2: "Turns per space are unbounded (long-lived chats: hundreds–thousands)". Each append is O(n) (`conj-distinct` linear scan, 707-712) plus full-vector serialize. Plan called for a subindexed seq→turn-id sorted map.
- `$$projection-chat-canvas` (1193): embeds the full `:turn-order` vector per space (`chat-canvas-projection`, 1106-1116) — a second unbounded copy rewritten per turn. The plan explicitly rejected materializing this projection for exactly this reason.
- `$$space-graph` (1176): parent → `{child-id → edge}` map (1401). Spec op 7: "expected shallow-but-wide (many children per heavily-explored space)" — unbounded fan-out, no cap.
- `$$artifact-graph` / `$$artifact-graph-in` (1187-1188): object-id → `{edge-id → edge}` maps. Spec op 8: "relations grow monotonically; existing edges never removed" — unbounded, no cap.
- `$$projection-object-relations` (1195): `{:out {edge-id edge} :in {...}}` per object (1132-1142) — same unbounded growth, duplicated from the artifact-graph PStates.

Runtime trace for the hot case: a space with 2,000 turns pays, per new turn, a full read + deserialize + linear scan + full write of a 2,000-element vector in `$$turns-by-space`, twice more for the canvas — on the interactive send path. These must become subindexed structures with point/range access; that changes schemas and every reader/writer of them.

## Stream topology idempotency

**Check:** for each stream topology, trace every write and side effect under record retry.

**FAIL (major).** The single stream topology (space.clj:1197-1635) is not retry-safe. Default retry mode `:individual` (no override at `source>`, 1198); a record can replay after any subset of its partitioner-bounded write groups committed (stream.md:9,96).

1. **No journal/dedup for `:space/create`, fork, control, and space-turn requests.** A retry re-executes the full branch against current state:
   - **Create retry mutates the audit trail.** First attempt commits `$$spaces` (1225). Retry re-reads `*existing-thread` (1205) → now non-nil → `space-event` (319-337) emits `:space/touched` instead of `:space/created`. The decision (same `:decision/id`, 1212) and the event (same id `<req>/event/space`, 1223-1224) are overwritten with **different content**. Spec ("Entity: Request / Decision / Event audit records"): "decided × redelivery/retry of the same request — decision: stable". Violated.
   - **Fork retry re-appends the LLM dispatch** (`depot-partition-append! *llm-depot *llm-request :append-ack`, 1414-1415) with no `(|direct (ops/current-task-id))` commit boundary before it and no consumer dedup — see item 3.
   - **Control retry re-appends the control** (1476-1477). Traced into llm.clj: `fold-control` (1249-1274) dedups `controls-by-id`/`control-order` (`record-control`, 1161-1165) but `add-compaction` (1231-1238) and `add-steer` (1240-1247) `conj` unconditionally onto vectors → **duplicate compaction/steer history entries on redelivery**. Spec/plan cross-kernel contract: "compaction history is appended keyed by control id (re-delivery collides, no duplicate entries)". Violated.
   - **Patch-proposal-create retry resets resolutions.** `patch-proposal-row-from-request` hardcodes `:status :pending` (1056-1069) and the write is an unconditional `termval` (1619-1623). Replay of the ingest record after a resolution has been applied resets the proposal to `:pending` and erases `:resolution/turn-id`/`:reason`. Spec op 4: "redelivery after resolution must not reset a resolved proposal back to `:pending` … flagged as a must-hold". Violated.
2. **Compose has a dedup mechanism that makes retry worse, not safe** — traced under "Partial failure" below: the `$$send-by-idempotency` row written mid-tree (1324-1325) causes the retry to take the decision-only branch (1235-1239) and permanently skip uncommitted downstream writes, and it overwrites the decision with `:idempotency/replayed? true` (`idempotent-decision`, 597-617) for what is actually a same-request retry — decision not stable.
3. **Cross-module dispatch is at-least-once with NO consumer dedup.** The dispatch carries deterministic ids and an `:idempotency/key` (space.clj:439-445), but the LLM intake never consults them: llm.clj:1304-1337 writes the decision (`termval`, 1309), then `initial-turn-run-row` (385-435: `:status :pending`, `:last-seq -1`, empty items/approvals/controls) is written **unconditionally** over `$$llm-turn-runs` (1329) and the pending entry re-added (1335). Runtime trace of a duplicate dispatch (fork retry, or any redelivery): a `:running`/`:succeeded` run row is **reset to `:pending` with its accumulated state wiped and re-enters the executor queue**; `grantable-claim?` (llm.clj:641-647) accepts `:pending` → the executor re-claims and **re-runs the model** — a duplicate physical side effect. This violates the spec's dispatch obligation ("a retried/redelivered request collides with itself instead of minting duplicates") and the template question "Does the receiving topology deduplicate?" — it does not.
4. IDs: all topology-side ids are pure functions of the request (events 244-250, turn/bundle 179-189, run/dispatch 410-426, control 483-487, materials 878-894). `random-id`/`now-ms` appear only in client-side builders. This sub-item is satisfied; it does not rescue the rest.

## Partial failure in stream topologies

**Check:** for each stream event writing multiple PStates across partitions, can failure + retry leave writes permanently unexecuted?

**FAIL (major).** Compose-and-send: the event tree is ~14 partitioner-bounded groups. Write order: decision (1243-1244) → events ×4 (1263-1270) → space/turn-order/canvas (1313-1315) → turn + pointer indexes (1316-1319) → bundle (1320-1321) → dispatch row (1322-1323) → **idempotency row (1324-1325)** → objects/object-detail/relations ×4 + artifact edges ×8 (1326-1355) → mirror append to `*llm-depot` (1356-1357). Groups commit independently at partitioner boundaries (stream.md "When PState writes commit").

Failure scenario (specific): worker dies after the group containing the `$$send-by-idempotency` write commits but before the `$$objects`/artifact-graph groups and the `*llm-depot` append execute. Retry replays from `source>` → idempotency lookup (1233-1234) hits → branch 1235-1239 writes **only the decision** and emits nothing else. Permanently lost: all four catalog objects, all relation projections, all eight artifact-graph edge writes, and — worst — the **LLM run dispatch**. The turn and frozen bundle exist, the decision says accepted, and the run never reaches the LLM kernel. No reconciliation path exists. This violates the spec's all-or-nothing fact-family invariant (op 2: "no observable state where the turn exists but the bundle or dispatch does not" — here it is permanent, not transient) and the skill rule that retry must converge to the complete event tree. Fixing it requires a real journal/dedup spine that re-emits downstream effects on replay (as PLAN specified) — restructuring, not a line edit.

Other branches converge under retry only because retries re-execute everything — at the cost of the non-idempotent effects documented in the previous check.

## Single depot append per client operation

**Check:** each client write operation must call `foreign-append!` exactly once.

**FAIL.** `append-llm-observation!` (space.clj:1712-1737) performs two foreign appends for one logical operation: `llm/append-observation!` (foreign-append! to the LLM obs depot, llm.clj:1480-1485) followed by `append-space-action!` (foreign-append! to `*space-action-depot`, space.clj:1705-1710). Runtime trace of a client crash between them: the observation is durable in the LLM kernel (items/status/usage update) but the patch proposal never reaches Space — permanently, since Space's topology sources only `*space-action-depot` (1198) and never sources any observation depot. The mirrors declared at 1168-1169 are append targets only. The fix the spec/plan shape requires — Space sourcing the LLM observation depot server-side and deriving proposals in-topology — is architectural.

## Application-state caches survive restart

**Check:** every TaskGlobal or in-process cache must have a durable source and rebuild path.

**PASS.** space.clj declares no TaskGlobals, no `declare-object`, and holds no in-process application state; all module state is PStates (1172-1195), which are durable replicated storage in their own right; topologies resume from persisted offsets on worker restart (SKILL.md "Worker restart does NOT replay depot history"). The executor/spawn machinery lives in llm.clj, outside this module. Nothing to rebuild.

## No reimplementation of built-in operations

**Check:** scan for custom code duplicating Rama built-ins.

**FAIL (minor).** `select-pstate-one` (space.clj:1739-1741) — `(first (foreign-select path pstate))` — reimplements `foreign-select-one`. Every read helper in the module (1743-1843) routes through it. One-line fix per call site. (Same pattern exists at llm.clj:2003-2005 — read-only context, noted for completeness.) `conj-distinct`, `content-hash`, etc. do not duplicate built-ins.

---

## Spec conformance findings (beyond the template categories)

**S1 — Idempotency check-then-act is not atomic; concurrent same-key sends both mint facts. FAIL.**
The `$$send-by-idempotency` read (1233-1234, segment at `|hash *idempotency-key`) and write (1324-1325, ~10 partitioner hops later) bracket the entire fact-minting pipeline. Stream records pipeline concurrently; segments of two events interleave on the key's task. Trace: sends A and B, same key, both in flight → both read absent (no row committed yet) → both take the fresh branch → two turns, two bundles, two runs, two LLM dispatches minted; last `dedupe-row` write wins. Spec op 2: "Under concurrent replays of the same key, at most one set of facts is ever minted." Violated. (Sequential replay — second send appended after the first's row committed — does work: `idempotent-decision` returns original event ids and replay's fresh ids stay absent, matching the matrix.) The fix is making the key check-and-claim atomic with acceptance in one segment — the journal/colocation design PLAN specified.

**S2 — Concurrent space-only turns lose turns from the order. FAIL.**
In the space-turn branch the `$$turns-by-space` read (1491) and write (1512) are split by a round-trip through the `*turn-event-id` partition (1508) and back (1510). Trace: turns T1, T2 to the same space concurrently in flight. T1 reads order `[t0]`, hops away; T2's segment runs on the space task, reads `[t0]`, hops away; T1 returns, writes `[t0 t1]`; T2 returns, writes `[t0 t2]`. **t1 is permanently dropped from the turn order** (it exists in `$$turns` but in no order, canvas, or count); `:turn-count` is also wrong in `$$spaces` (1511) and the canvas (1513). Spec op 2: concurrent sends "must serialize into one well-defined turn order"; matrix: "no torn intermediate where a turn is in turn order but unreadable" — here the inverse tear is permanent. Note the compose branch does this correctly — read and write in one segment (1271-1315) — and the control branch too (1433-1453); only the space-turn branch hops between read and write. Moving the event write after the order write fixes the race but the branch ordering must be restructured.

**S3 — Patch resolution lifecycle. FAIL.**
`apply-patch-decision` (1086-1097) is unconditional:
- Unknown or missing proposal id **creates a phantom resolved proposal row** (`(or existing {:patch-proposal/id proposal-id})`), including under the literal key `nil` when the request carries no id (1625-1629 always run for `:turn/patch-accept`/`:turn/patch-reject`). Spec matrix "does-not-exist × turn/patch-accept with no/unknown proposal id": "read-patch-proposal: absent/unchanged — nothing mutated". Violated.
- No first-resolution-wins or status guard: a second resolution overwrites the first, and a **retry of an earlier resolution overwrites a later one** (accept processed → fails downstream → reject processed → accept retries → proposal flips back to `:accepted` with the stale turn id). Spec op 5 concurrency requires a defined, non-corrupting winner; under at-least-once the winner here depends on retry timing — corrupting.
- Ingest redelivery resets resolved proposals to `:pending` (see Stream idempotency item 1).

**S4 — Patch-proposal ingestion path contradicts spec op 4. FAIL.**
The spec: patch-like observations *streamed from an LLM run* become pending proposals; ingest mints **no decision** and **creates no turn** ("read-turns-by-space: unchanged"; "observations are not requests — no space decision minted"). The implementation: (a) Space never sources observations — proposals exist only when a client calls the dual-append wrapper; the actual executor path (llm.clj:1947-1948, `run-one-pending-with-adapter!` → `append-observation!`) appends to the LLM kernel only, so **executor-streamed patch proposals never materialize in Space at all**; (b) when the wrapper IS used, the ingest is a `:turn/patch-proposal-create` request routed through the space-turn branch (1479+), which mints a decision AND appends a turn to the order (1512); (c) if the observation's space id doesn't resolve (`(or (:space/id obs) (:llm-thread/id obs))`, 1721), `interpret-space-turn` rejects `:space/not-found` and — because the proposal write (1619-1623) is inside the accepted branch — **the proposal is silently dropped**. Every sub-path violates the op-4 contract.

**S5 — Fork-binding gate. PASS (with the dedup caveat already counted).**
Traced in llm.clj: `fork-binding-required?` (450-454) — true when the run row carries `:fork/from-native-thread-id` and the backend-appropriate native binding is absent. `run-one-pending-with-adapter!` (1913-1956) checks the gate **before** claiming (1921: claim only `when-not` gated) and returns `{:spawned? false :reason :fork-binding-not-durable}` (1929-1934) with no claim appended, no adapter invocation, run left `:pending` and queued — re-checkable and side-effect-free, exactly the spec's shape. Binding durability: run row (PState) via `bind-run-to-observation-thread` (902-913, first-binding-wins) and thread row via `upsert-thread-row` (474-495, preserves native ids once set); `bind-run-to-existing-thread` (497-506) re-derives the binding from the thread row at intake. A durable binding cannot regress to absent through these paths. The one hole — a redelivered dispatch resetting the run row (and with it an observation-derived binding) — is the consumer-dedup failure already counted under Stream idempotency item 3.

**S6 — Bundle-freeze determinism under replay. PASS.**
`context-bundle-row` (386-401) is a pure function of the request: prompt + refs rendered by id with no existence checks (`render-ref`/`render-model-input`, 363-384; `object:`, `slice:`, `user-authored-derivative:` forms per spec), execution options selected by allowlist (`bundle-option-keys`, 353-361) so non-bundle-owned keys (e.g. `:executor/pool` — consumed only into the dispatch record, 452-453) are structurally excluded; timestamp from `:request/time-ms`. The hash is sha256 of `pr-str` of an ≤8-key map built literally (insertion-ordered array-map) — deterministic. The decision's hash equals the bundle's by construction (`context-bundle-event` 403-408 → `accepted-decision` 252-277). Replay rewrites a byte-identical bundle via `termval` at the same bundle id; no mutation path exists. Frozen-forever holds for same-request replay. (Foreign reuse of a bundle id by a *different* request clobbers — no if-absent guard at 1321 — but duplicate-id-across-requests is spec Ambiguity 1, left open; recorded as a divergence from PLAN's never-clobber rule, not a spec fail.)

**S7 — Determinism / rebuildability. FAIL.**
No wall-clock, randomness, or iteration-order leaks in the space topology itself (all times from `:request/time-ms`/payload; ids deterministic; hashed maps insertion-ordered). But materialized state is **interleaving-dependent**: whether a compose's decision/event says `:space/created` vs `:space/touched` depends on whether a concurrent/pipelined earlier event's `$$spaces` group committed before this event's read at 1205; canvas/turn-count content depends on the S2 race; same-key concurrent sends (S1) mint different fact sets per interleaving. Spec: "Replaying the same appended inputs from empty state must reproduce **identical** canonical state… (full-snapshot equality across two independent runtimes)." Two replays of the same depots can legitimately produce different `$$space-events-by-id`, `$$space-decisions-by-id`, `$$projection-chat-canvas`, `$$turns-by-space` contents. Violated.

**S8 — Projection purity (chat canvas). FAIL.**
The create branch writes the canvas as `(chat-canvas-projection *thread-row nil nil)` (1220, written 1226) — turn-order hardcoded empty. Trace: `space/create` on an id that already exists with N turns (duplicate create is accepted; `interpret-thread-create` 535-539 never rejects), or a create retry after pipelined sends landed: the canvas is overwritten to `:turn-order []` while the thread row preserves `:turn-count N` (`thread-row` 651-663 keeps the existing count) — a projection state inconsistent with canonical `$$turns-by-space` until the next turn rewrites it. Spec op 9: "every projection must be a pure function of canonical accepted state". The create-branch canvas is a function of the wrong inputs.

**S9 — Always-readable / absent-not-error. PASS.** Every read is a `keypath` point lookup returning nil on absence (read helpers 1743-1843); empty-collection defaults applied client-side ([] at 1766, {} at 1771/1808/1813, empty projection at 1826). Rejected requests create nothing readable (all fact writes gated on `decision-accepted?`: 1214, 1245, 1364, 1422, 1484).

**S10 — Space-only turns produce no LLM side effect. PASS.** `interpret-space-turn` (619-626) emits only a turn event; the decision's event list is exactly `["<req>/event/turn"]` (`accepted-decision` keeps only present roles, 252-277); the branch (1479-1629) contains no bundle, dispatch, control, or mirror append; turn row's bundle pointer is nil (1489). Rejection on missing space with reason `:space/not-found` (623-624) matches the matrix.

**S11 — Control turns. PASS with notes.** Discrimination is on `:request/type` alone (`request-control-type` 473-481; `<<cond` order is load-bearing: the control case at 1417 precedes the space-turn case at 1479, and `space-turn-request-types` (68-69) still *contains* the control types — fragile but correct today). Deterministic control id `<req>/llm-control` (483-487); decision carries `:llm-control/type` (252-277); `control-by-turn` and `llm-controls` both written (1458-1462); native correlation id passed through verbatim (504); patch resolutions carry no control fields (S10 path). Validation rejects missing run id / approval id (637-642). LLM-side fold (llm.clj:1249-1274) resolves approvals (status mapping incl. `:expired`, pending cleared in-row 1202 and in `$$llm-approvals-pending` 1418-1421, run → `:running` or `:failed` with structured `:approval/declined` error 1204-1219), cancels (run `:cancelled`, removed from pending on terminal 1422-1424). Note (cross-kernel contract gap, llm-side): `fold-control` has **no terminal-state guard** — `cancel-run` (1221-1229) flips a `:succeeded` run to `:cancelled`; the plan's "terminal states are never overwritten by late controls" obligation is unenforced. Recorded here because Space's spec relies on it; the write lives in llm.clj.

**S12 — Fork. Divergences noted, not counted as spec fails.** Parent existence is never checked (`interpret-fork-from-span` 557-571 accepts with nil thread; spec flags this open, PLAN chose reject). The anchor `slice:<id>` ref appears in the rendered bundle only if the caller passes it in `:refs` (372-384) — the kernel does not enforce the spec's "rendered input includes the anchor slice ref". Lineage is bidirectional (child `:parent-space/id` via 653-663; parent's `$$space-graph` edge 1398-1401). Anchor slice immutability holds in the fork branch via keep-existing (1409-1411) — but the plain slice-create branch overwrites unconditionally (1536), so foreign slice-id reuse clobbers (Ambiguity 1 again).

**S13 — `:space/reconcile` validates but default-rejects as `:request/type-invalid`.** It is in `space-request-types` (57-60) so validation passes, yet no `<<cond` case matches and `(default>)` (1631-1635) mints `:rejected :request/type-invalid`. One-request-one-decision holds, but a type the validator declares legal is rejected as type-invalid — inconsistent contract surface. Minor.

---

## Falsification pass (write → render → truth → clear lifecycles)

**Architecture.** One stream topology owns 25 `{String Object}` PStates: canonical rows, audit (requests/decisions/events), pointer indexes, catalog + double-stored relation graphs, and three *materialized* projections. Cross-kernel hand-off via two mirror depot appends. No journal, no internal fan-out depot, no query topologies — PLAN's dedup spine, subindexed schemas, `*accepted-facts` catalog topology, and projection-as-query-topology are all absent. Client helpers poll PStates for awaiting.

**Failure modes attempted.** (1) retry after idempotency-row commit → permanent loss of catalog/edges/dispatch — CONFIRMED; (2) concurrent same-key sends → double mint — CONFIRMED; (3) concurrent space-only turns → turn dropped from order — CONFIRMED; (4) fork/any dispatch redelivery → LLM run row reset + duplicate model run — CONFIRMED; (5) proposal ingest redelivery after resolution → resolution erased — CONFIRMED; (6) patch-accept with nil/unknown id → phantom row under nil key — CONFIRMED; (7) create retry → decision/event content flips — CONFIRMED; (8) duplicate create → canvas wiped to empty order — CONFIRMED; (9) fork-binding premature claim → correctly gated, no side effect — held; (10) bundle replay divergence → held (pure + deterministic hash).

**Writers/readers/clearers.** Nothing is ever cleared (correct per skill rule — no deletes). Multiple-writer hazards: `$$turns-by-space` and `$$spaces` written by four branches (compose/fork/control/space-turn) — last-writer-wins races documented in S2/S7; `$$space-patch-proposals` written by ingest and both resolution types with no guard (S3); LLM-side `$$llm-turn-runs` written by intake AND claim AND obs AND control folds, with intake unconditionally resetting (Stream idempotency 3).

**Async ordering risks.** Decision commits (1243-1244) several groups before canonical facts — a caller that acts on the decision alone can get `:space/not-found` for a dependent request against a space whose create decision it just read; tolerated only because the contract makes callers await per-fact materialization. All other ordering risks are the counted failures S1/S2/S7.

**Error-path cleanup.** No in-flight flags or locks exist to leak (no TaskGlobals). The "cleanup" gap is the inverse: nothing re-emits lost downstream effects after the poisoned compose retry.

**Open doubts.** (a) Whether IPC test usage ever exhibits the pipelined interleavings — irrelevant under the skill's production rules (design for production, never downgrade for tests). (b) The exact reset surface of a redelivered dispatch on a *terminal* run depends on subsequent observation redelivery, which the obs `:all-after` source partially mitigates llm-side; the run-row reset itself is unconditional regardless.

---

## Verdict

**major-fail** — multiple failures require structural rework, not line edits: no retry/dedup spine (permanent partial application of compose under retry; unstable decisions/events), cross-kernel dispatch with no consumer dedup (run-row reset → duplicate model execution), non-atomic idempotency check-then-act and a split read-modify-write that loses turns under concurrency, unbounded non-subindexed collections on the hot path requiring schema redesign, and a client-side dual-append ingestion path that must move server-side. The strongest single failure (permanent fact-family loss under stream retry) is alone sufficient for major-fail.
