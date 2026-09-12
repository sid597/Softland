# Implementation Validation — POST-FIX (fix session 4)

<!-- Phase 4, RETROSPECTIVE mode, POST-FIX re-validation. Subject: the kernel
     contract layer AFTER fix session 4 landed (uncommitted in the working tree
     at validation time):
       src/app/server/rama/text_kernel.clj   (text-kernel-module, V0/V1)
       src/app/server/rama/core.clj          (shared envelope + guarded-fold helpers)
       src/app/server/rama/util_fns.cljc     (adapter / compatibility layer)
       src/app/server/rama/kernel.clj         (KERNEL-SHAPE, as-built contract)
     Validated against IMPLICIT_SPEC.md (the spec) and PLAN.md (the code-blind
     re-derived plan), under the rama skill's production rules.
     Retro rule: verdicts are recorded; no fix loop is triggered by this artifact.
     Default verdict: major-fail. Every check below carries file:line citations
     and a runtime trace. Findings are split IN-SCOPE (fix-session-4 claims) vs
     DEFERRED (carried, out of this session's scope). -->

Runtime evidence base:
- `text_kernel_probe_test` — **41 assertions, 0 fail, 0 error** (re-run during this validation).
- `text_kernel_test` — **69 assertions, 0 fail, 0 error** (re-run during this validation).
- Three ad-hoc probes (conflict-decision readability, empty-string ingest, trailing-newline; `/tmp/probe_conflict.clj`) plus a dedup-key-alignment probe (`/tmp/probe_dedup_keys.clj`), traced inline below.

## What changed since the pre-fix validation (high level)

The pre-fix module (R4, **major-fail**) was a **stream** topology with **five `|hash` partitioner hops** per accepted ingest, no dedup/collision guards, `(now-ms)` in decisions, non-revision-scoped unit ids, and a poison-record class for garbage appends. Fix session 4 rewrote text_kernel.clj (518 lines changed) and extended core.clj (+164) to a **microbatch topology with zero partitioner hops**. The architectural majors that drove the pre-fix verdict (D1+D2: K-01/C4/C8) are now structurally addressed. Each claim is re-checked adversarially below.

## Plan vs implementation divergence (phase step 3)

The post-fix implementation now MATCHES the code-blind plan on its load-bearing axes (microbatch, zero partitioners, dedup guard, collision guard, revision-scoped unit ids, ingress surrogate keys, `:decided-at` from request time). Residual divergences from PLAN.md, none of which the **spec** pins:

| # | PLAN.md says | Implementation does | Where | Spec pins it? |
|---|---|---|---|---|
| D1 | `""` → 0 units; trailing newline drops one empty segment | `str/split #"\n" -1`: `""` → **1 empty unit**; `"a\nb\n"` → **["a" "b" ""]** (3 units) | text_kernel.clj:264 | **No** (A5 explicitly open) |
| D2 | Status domain `#{:rejected :canonical}`; non-default branch rejected `:branch-not-found` | Domain `#{:accepted :rejected :hidden :promoted :superseded}`; non-default branch accepted-and-written | text_kernel.clj:46-47,251-260 | **No** (A7/A11 open) |
| D3 | Typed defrecord leaf classes; `Object` schema banned | `(map-schema Keyword Object)` on 9 of 11 PStates | text_kernel.clj:413-426 | **No** (A2; deferred — see DEFERRED) |
| D4 | Unitization is its own request type `:artifact/unitize` | Internal derivation inside the ingest accept path; `unitize-lines!` is a read-await | text_kernel.clj:506,642-648 | **No** (A6 open) |
| D5 | `$$projection-views` materialized table | Projections computed client-side per read (`read-unit-projection`); `$$projection-cache` scaffold unwritten | text_kernel.clj:674-695 | **No** (A2; C19 allows derived-at-read) |
| D6 | Decisions keyed by request-id | Keyed by `request-id "/decision"`; read via `decision-id-for-request-id` (contract still met) | core.clj:345-347; text_kernel.clj:563-567 | No (contract met) |

The spec, not the plan, decides the verdict. D1/D2/D4/D5 each fall inside an explicitly-open spec ambiguity (A5/A7/A11/A6/A2), so none is a spec violation. D3 (Object schema) is a deferred-scope item (KNOWN DEFERRED: defrecord leaf schemas).

---

# Check matrix

## Redundant conditionals

**Check:** if every branch of an `<<if`/`<<cond`/`<<switch` does the same operation with only a variable differing, replace with a single operation using that variable directly.

**Evidence:** text_kernel.clj:454-480 — the `<<cond` now produces ONLY `*decision0` per branch (no duplicated 5-op write tail; the pre-fix tail at every branch is gone). The request+decision writes were hoisted: the request row is written once before the cond (line 445), and the decision write happens once after the cond + collision guard (line 491). Each `<<cond` branch is a distinct interpretation, not a copy. The accept-path `<<if` (493-516) dispatches per event type with genuinely different writes per branch (ingest vs status-set).

**Trace:** ingest branch (458-465) reads `$$text-revisions`; status branch (467-474) reads `$$units-by-artifact`; compat (476-477) and default (479-480) read nothing. No two branches do the same op with a swapped variable.

**Verdict: PASS.** The pre-fix duplication finding is resolved.

## Consecutive keypath

**Check:** `(keypath *a) (keypath *b)` → `(keypath *a *b)`.

**Evidence:** Every multi-key navigation is already merged into a single `keypath`:
- text_kernel.clj:463 — `(keypath *lookup-artifact-id *lookup-revision-id)` on `$$text-revisions`.
- text_kernel.clj:472 — `(keypath *lookup-artifact-id *lookup-unit-id)` on `$$units-by-artifact`.
- text_kernel.clj:509 — `(keypath *artifact-id *revision-id)` on `$$text-revisions`.
- text_kernel.clj:515 — `(keypath *artifact-id *branch-id *unit-id)` on `$$unit-statuses`.
- text_kernel.clj:590 — `(keypath artifact-id revision-id)` in `read-text-head`.

No `(keypath *a) (keypath *b)` adjacency remains (grep for two consecutive `(keypath` in one path vector: none). The pre-fix four-site finding is resolved.

**Verdict: PASS.**

## Select-compute-transform

**Check:** `local-select>` → compute → `local-transform> termval` of the same location → replace with `+compound`/aggregator.

**Evidence:** In-topology selects: dedup read (line 440, `$$decisions-by-id`), ingest existence probe (463, `$$text-revisions`), status unit probe (472, `$$units-by-artifact`), collision probe (487, `$$events-by-id`). None is read-modify-write of the same location: the dedup read feeds a gate (no write to that key on the proceed path), the existence probes feed accept/reject decisions, and all subsequent writes derive their value from the **event** (status-materialization, revision-materialization), not from the read value.

**Verdict: PASS.** `+compound` does not apply; no select-compute-transform-of-same-key pattern exists.

## Unnecessary nil->val

**Check:** navigators handle nil as empty collection — do not add `nil->val` unless the next navigator requires non-nil.

**Evidence:** `grep nil->val` over text_kernel.clj/core.clj: zero occurrences. Client-side `(or … {})` defaults (text_kernel.clj:596-598,606-608, read-units/read-unit-statuses) are JVM-side return shaping, not path navigation.

**Verdict: PASS.**

## :allow-yield?

**Check:** `local-select>`/`select>` iterating a subindexed structure beyond ~100 entries needs `{:allow-yield? true}`.

**Evidence:** The module declares NO subindexed structures (text_kernel.clj:413-426 — all `(map-schema Keyword Object)` / nested plain maps, no `{:subindex? true}`). Every in-topology read is a point read at a fully-specified key path (440, 463, 472, 487). No topology read iterates a collection.

**Trace:** no iteration ⇒ nothing to yield. (The defect that the growing inner maps *should* be subindexed is charged to the next check, and is a DEFERRED finding per the KNOWN DEFERRED list, not this one.)

**Verdict: PASS** (vacuously — no iterating reads exist).

## Non-subindexed collections without size limits

**Check:** for every write to a non-subindexed inner collection, verify the application enforces a max size; otherwise it must be subindexed.

**Evidence and traces:**

1. `$$text-revisions {String {String (map-schema Keyword Object)}}` (text_kernel.clj:420), written at 509 — one entry per revision, each carrying the full document content (`revision-materialization`, 385-392). Revisions are unbounded (spec: "Artifacts, revisions … grow without bound and are never deleted"). Non-subindexed inner map ⇒ all revisions of an artifact serialize as one RocksDB blob; each re-ingest rewrites every prior revision's content. `read-text-head` (584-592) reads one revision but the whole inner map deserializes. **Unbounded, uncapped.**
2. `$$units-by-artifact {String {String (map-schema Keyword Object)}}` (421), written at 510 with `(termval *units)` — one entry per line, whole-map write. Spec scales per-line ("very large documents (per-line unit fan-out)") with unbounded line count. No code caps line count (`line-ranges`, 262-279, loops over all lines). **Unbounded.**
3. `$$unit-statuses {String {String {String (map-schema Keyword Object)}}}` (425), written at 515 one entry per (artifact, branch, unit). The fix re-keyed this artifact→branch→unit (vs the pre-fix branch-led layout that funneled the whole world onto one task), which fixes the hotspot, but the innermost unit map is still non-subindexed and grows with judgments per artifact-branch (spec: "unbounded over time"). **Unbounded** (though now correctly colocated per artifact).

**Trace:** none of the three has a size cap; all three should be `{:subindex? true}` on the inner collection per SKILL.md goal 4.

**Verdict: FAIL** — but **DEFERRED scope.** The KNOWN DEFERRED list explicitly carries "K-06 subindexing/schemas ({String (map-schema Keyword Object)} rows, whole-map unit writes, unbounded revisions)". Recorded here as a deferred finding, not an in-scope fix-4 failure. (The fix-4 claims do not assert subindexing was done.)

## Microbatch / stream idempotency

**Check:** trace what happens if any record retries; every write and side effect must be duplicate-safe.

The topology is **microbatch** (text_kernel.clj:412, `microbatch-topology`), not stream — exactly-once at the processing level. The relevant retry surface is therefore (a) Rama microbatch replay of a not-yet-acked batch, and (b) **client** re-append of the same `:request/id` (a second depot record), which exactly-once does NOT dedupe. Both are guarded:

1. **Wall clock removed from decisions.** `accepted-decision`/`rejected-decision` stamp `:decided-at (:request/time-ms request)` (core.clj:362,374); `grep now-ms` inside the constructors (353-374): none. A replayed delivery re-derives a byte-identical `:decided-at`. The pre-fix non-idempotent-value finding is **resolved**. **PASS.**

2. **Dedup gate absorbs client re-append.** First step per record: `local-select>` the stored decision at `*audit-decision-id` (440) → `decision-dedup-gate` (441) → `filter> (= :proceed *gate-status)` (443). If a decision exists for this id, the gate returns `:replay` or `:conflict` and the filter **drops the record before any write** (verified: `/tmp/probe_conflict.clj` shows a conflicting re-append produces no new decision/event/head change). Dedup read-key == decision write-key on BOTH the normal path (`rN/decision` == `rN/decision`) and the garbage-surrogate path (`invalid/<hash>/decision` == same) — verified by `/tmp/probe_dedup_keys.clj`. So decision-exists ⟺ all effects applied (atomic microbatch group), making the gate a complete dedup. Implements C8 idempotency; resolves A4 as first-write-wins. **PASS.** `text_kernel_probe_test` "duplicate request-id, same payload" (replay, decided-at stable at 10) and "different payload" (no second event, head unchanged, original request preserved) both pass.

3. **Event-id collision guard.** Accept path: `accepted-event-id-of` (350-355) → probe `$$events-by-id` at the proposed id (487) → `guard-event-collision` (357-367) rejects `:event-id-conflict` if a committed event already names that id. Verified by the probe "event-id collision from a different request rejects :event-id-conflict" — the committed event's `:text/content` stays "victim". A committed `KernelEvent` is never `termval`-overwritten. The pre-fix event-mutability finding is **resolved.** **PASS.**

4. **IDs minted client-side.** `proposed-event-id` is minted in the helpers before append (text_kernel.clj:142,170; core.clj:440), never inside the topology; `ingest-request->event`/`status-request->event` fall back to a *deterministic* `(str request-id "/event")` (text_kernel.clj:187,206) only when absent — replay-stable. No randomness in the topology body. **PASS.**

5. **No internal depot appends.** Single microbatch, no `depot-partition-append!`. **PASS.**

**Verdict: PASS** (all idempotency sub-items). The pre-fix three sub-failures are resolved.

*Observation (not a failure):* the dedup gate fingerprints the **whole** raw request including `:request/time-ms` (core.clj:489, `with-request-fingerprint` on `*raw`; gate at 595 fingerprints `incoming-request`). A client retry that re-mints `:request/time-ms` would be classified **`:conflict`** (rejected as impostor) rather than `:replay`. Truth is still protected either way (both branches are total no-ops; no double-apply). `request-fingerprint`'s own docstring (core.clj:545-546) flags that volatile fields should be dissoc'd before fingerprinting, but the topology does not dissoc. This is a replay-vs-conflict *classification* robustness gap, not a C8 correctness failure — recorded as IN-SCOPE finding F1 below.

## Partial failure (microbatch)

**Check:** for each event writing multiple PStates across partitions, consider failure+retry after partial commit; can a write be left permanently unexecuted, and what do readers observe?

**Evidence:** ALL writes for one record happen on the ingress task `hash(:routing/key)` with **zero partitioner hops** in the topology body (verified: `sed -n '404,516p' | grep -E '\|hash|\|all|\|origin|\|global|\|direct|select>'` returns only a comment line). A microbatch is atomic per task: either the whole group commits or the batch retries from the source and re-runs the (dedup-guarded, termval-only) writes. There is no cross-partition re-convergence, so the pre-fix race classes are structurally eliminated:

1. **Per-key serialization holds.** Ingest I and status-set S for the same artifact share the depot partition (`hash-by :routing/key`, 405) AND the topology task (zero hops). The single-threaded task processes same-batch same-key records in depot order, and S's unit probe (472) sees I's unit write (510). Verified by the probe "status-set racing its own ingest" — appended I then S with **no await between**, S decides `:accepted` (the pre-fix `:target-unit-not-found` race is gone). **PASS** (C8 deterministic acceptance).
2. **No torn outcome.** A retry replays the whole record; the dedup gate makes a re-delivery of an already-committed record a no-op, and an in-flight (uncommitted) record's writes are all-or-nothing within the microbatch. No "event row committed, status row not" split is possible. **PASS.**
3. **Head monotonicity.** Re-ingest of an existing revision id now **rejects** `:revision-exists` (interpret-ingest-request, 243-247) instead of overwriting — verified by the probe "revisions are immutable" (head stays "original"). A re-ingest with a NEW revision id moves head via a single `termval` (508), atomic per key. **PASS.**
4. **Cross-PState visibility within a request is atomic.** Decision + event + materializations commit in one microbatch group on one task; a reader cannot observe an accepted decision while `read-event` of its id returns nil. **PASS** (C4 atomicity now holds).

**Verdict: PASS.** The pre-fix five partial-failure sub-failures are all resolved by the microbatch + zero-hop architecture.

## Single depot append per client operation

**Check:** each client write operation calls `foreign-append!` exactly once.

**Evidence:** The only `foreign-append!` site is `append-action-request!` (text_kernel.clj:546-549). `ingest-text!` (635-640), `set-unit-status!` (650-655) each append exactly one envelope. `unitize-lines!` (642-648) appends nothing (read-await). Every util_fns write helper appends exactly one compat envelope (util_fns.cljc:134-143 `append-compat-event!`, called once per `update-*`/`emit-*`/`save-*`). Atom `swap!`s alongside appends are charged to the caches-survive-restart check.

**Verdict: PASS.**

## Application-state caches survive restart

**Check:** for each TaskGlobal / in-process cache holding application state, name a durable source and cite the concrete rebuild path.

**Evidence and traces:**

1. **util_fns transitional mirror atoms** (util_fns.cljc:121-128: `!cli-sessions`, `!agent-runs`, `!sidebar-truth-atom`, `!settings-truth-atom`, `!agent-trail-atom`, `!workspace-truth-atom`, `!editor-doc-atom`, `!flow-session-atom`). These are still read as application state (`get-cli-session` 191, `get-agent-run` 238, `get-sidebar-state` 242, etc.) and still have **no rebuild path** — `defonce` literals, reset empty on restart; no code folds `:compat/recorded` events back. **Unchanged from pre-fix.**

   The fix-4 response is **declaration, not durability**: `transitional-mirror-quarantine` (util_fns.cljc:103-119) explicitly declares `{:kernel-contract? false :durable? false :reset-on-restart? true :rebuild-path :none}` and lists all eight atoms; the test `mirror-quarantine-covers-every-atom-test` asserts the declaration covers exactly the atoms present in the namespace (verified green). The contract claim (fix-4 item e) is that these are **quarantined OUT of the kernel contract**, with the S40 `foreign-proxy-async` crash documented as the reason they can't yet become PState reads.

   **Assessment:** As a *cache-survives-restart* check this is still a FAIL (no rebuild path exists). But the fix-4 scope did not claim to give them durability — it claimed to remove them from the kernel contract via explicit, test-enforced declaration. Against the **contract** (C12: the back-arrow rule applies to *kernel state*), the quarantine declaration is a legitimate move: these are now declared non-kernel session-local UI mirrors, not "Rama-vs-atom truth conflict inside the kernel." Recorded as IN-SCOPE finding F2 (the durability gap survives, now bounded and disclosed rather than silent).

2. **`!kernel-runtime`** (util_fns.cljc:12-17) still boots an InProcessCluster (`create-ipc`, text_kernel.clj:520). The deployed runtime IS the test harness; IPC state is process-lifetime only. **Unchanged.** This is the IPC-as-production-substrate deployment gap explicitly on the KNOWN DEFERRED list. Recorded as DEFERRED.

**Verdict: FAIL** — but split: the mirror-atom durability gap is IN-SCOPE finding F2 (bounded/declared, not silent); the IPC substrate is DEFERRED.

## No reimplementation of built-in operations

**Check:** scan for custom code duplicating Rama built-ins.

**Evidence:**
- `select-pstate-one` (the pre-fix reimplementation of `foreign-select-one`) is **removed** from text_kernel.clj. All nine read helpers (560-608) call `foreign-select-one` directly. (`select-pstate-one` still exists in the `dogfood/*` modules — out of this track's scope.) The pre-fix finding is **resolved.** **PASS.**
- `random-id` (core.clj:56-58), `now-ms`/`sha-256`/`canonical-str` (51-54,68-70,518-539) — no Rama built-in equivalents. `canonical-str` is a deterministic renderer for fingerprinting; not a built-in. Not charged.

**Verdict: PASS.**

---

# Spec validation (IMPLICIT_SPEC.md)

## Layer (a) operations

**Op A1 (ingest):** Accepted ingest produces one `:artifact/ingested` event with `:event/id` = `:proposed/event-id` (text_kernel.clj:186-187,68-90); carrier-independent envelope — target `{:target/kind :artifact …}`, payload `:artifact/type :text` (75-86); `[:ordering :key]` = `[:artifact id]` = `:routing/key` (88 vs 139). Verified by `kernel-event-envelope-test`. Artifact `:root-event-id` = ingest event id (383). Head round-trips byte-for-byte (`(str content)` 67,389; verified `text-head` = "keep\nreject\nalso keep" in `rama-v0-text-loop-test`, and `""` round-trips in `/tmp/probe_conflict.clj`). **Concurrency invariant now holds** ("decisions observe all previously accepted facts for that key" — microbatch + zero-hop, race probe passes). **Revision immutability now enforced**: a second ingest with the same (artifact-id, revision-id) rejects `:revision-exists` (243-247); verified by the "revisions are immutable" probe. **PASS** (the pre-fix Op A1 FAIL on concurrency + revision-immutability is resolved).

**Op A2 (unitization):** Internal derivation (A6 left open by spec — acceptable). One unit per line in document order, `:unit/preview` = line text (281-311); `:text/range` anchor (298-303); `[:provenance :root-event-id]` = ingest event id (308-310); ids `<artifact>/<revision>/line/<n>` now **revision-scoped** (292) — verified `"art_ri/rev_1/line/2"` in the re-attach probe. Idempotent re-derivation (pure fn + termval). Derives from the accepted event only (506, inside the `accepted-decision?` gate). **PASS.**

**Op A3 (status-set):** Decision derived from durable unit existence (472 → nil ⇒ `:target-unit-not-found`, 258); verified by `rejected-action-request-is-durable-test` and the re-attach probe's "late judgment on a dead revision's unit rejects." Branch-scoped storage `[artifact-id branch-id unit-id]` (515) and per-(artifact,branch) reads (606); `:reason` preserved (109-112,399). **Status event `[:ordering :key]` is now the routing key** `[:artifact artifact-id]` (116), NOT the pre-fix `[:unit unit-id]` — the pre-fix C4 ordering-key mismatch is **resolved** (comment at 114-115 documents the fix). "Later accepted wins" holds under microbatch depot order. **PASS** (the pre-fix Op A3 ordering-key + race FAIL is resolved).

**Op A4 (`:compat/record`):** Dispatched like any other type (476-477 → `core/interpret-compat-request`). Now **narrowed**: `compat-allowed-event-types` allow-list (core.clj:396-410) + per-type payload validators (412-431); an unknown event type rejects `:compat-type-not-allowed`, an allow-listed type with a missing required key rejects `:compat-payload-invalid` (486-503). Compat requests now carry a **header-proposed** event id minted client-side (`compat-record-request`, core.clj:440 `:proposed-event-id (random-id "evt")`), so identity is no longer invented inside interpretation. Verified by the probe "compat is allow-listed": `:evil/arbitrary-event` rejects, `:sidebar/file-select` accepts and the event carries the proposed id. **PASS** (the pre-fix C5 partial-fail on compat identity is resolved; prior finding 06/F3 addressed).

**Op A5 (raw append / generic ingress):** Verbatim durability: `storable-request` (core.clj:788-795) stores the record itself when it is a keyword-keyed map (verified `(= request (read-request …))` in `hidden-event-id-request-is-rejected-test`, `malformed-unknown-action…test`, and the garbage probe). Validation-before-dispatch: `request-validation-errors` computed at 446, tested as the FIRST `<<cond` case (455) before any type dispatch; malformed unknown type rejects `:request-invalid`, not `:unknown-action-type` (verified by `malformed-unknown-action-is-validated-before-dispatch-test`). **No-silent-drop for garbage now holds**: `audit-request-id` (core.clj:778-786) yields a deterministic `invalid/<hash>` surrogate for id-less/non-map/non-String-id records, `storable-request` wraps unstorable shapes in a bounded preview, and `pstate-key` (text_kernel.clj:343-348) coerces lookup keys — so a garbage append reaches a keyed write only via a safe key and still gets a durable rejected decision. Verified by three garbage probes (keyword-keyed-no-id → surrogate `:request-invalid` with `:request/id-invalid`; non-keyword-keyed → `:request-unstorable` bounded preview; non-map → topology not wedged, later fence still decides). The pre-fix poison-record FAIL is **resolved.** **PASS.**

**Op A6 (reads):** All read helpers route by `:pkey` = the routing key (the artifact key for truth reads, the caller-supplied routing key for audit reads) — partition-aligned with the zero-hop writes (560-608). Nonexistent ids ⇒ nil/{} (verified throughout). Projection rows carry `:target {:target/kind :unit …}`, `:preview`, `:provenance {:root-event-id …}` (657-672); canonical/discarded partition the units via complementary predicates over `discarded-statuses` (679-686); document order via `sort-by :unit/order` (677). `await-decision`/`await-materialized` poll without blind sleeps (610-627). **Monotonicity now holds** — the dedup gate prevents the pre-fix retry-flip (a committed accepted decision can no longer be overwritten as rejected). **PASS.**

**Op A7 (lifecycle):** `start-text-runtime!` returns the handle through which all appends/reads flow (518-537); `close-text-runtime!` idempotent/exception-swallowing (539-544); repeated start/close create independent IPCs (every test does this; both suites green). Fresh runtime ⇒ empty world (all reads nil/{}). Durability across restart: the IPC substrate is process-lifetime (DEFERRED — IPC-as-production deployment gap). **PASS** for the contract obligations in scope (the substrate choice is the deferred deployment gap, not a lifecycle-API defect).

## Entity matrix spot-traces

- **s0 × invalid append (payload `:event/id`)**: validator returns `{:type :request/payload-event-id}` plus all other applicable typed errors (`cond->` collects all, core.clj:230-296); first `<<cond` branch (455) writes verbatim request + `invalid-request-decision` (`:request-invalid`, errors, nil event-id, routing key copied). Smuggled id never becomes an event. Verified by `hidden-event-id-request-is-rejected-test` (read-event "evt_hidden" = nil). **Matches matrix.**
- **s1/s2/s3 × re-append same request-id (same payload)**: dedup gate → `:replay` → no writes; one decision, `:decided-at` stable. Verified (`probe-duplicate-id-same-payload!`, decided-at = 10). **Matches matrix.**
- **s1/s2/s3 × re-append same request-id (different payload)**: dedup gate → `:conflict` → filtered → no writes; original decision/request/head preserved, impostor's `evt_dup_b` never an event. Verified (`probe-duplicate-id-different-payload!`). The conflict decision row itself is computed but **not persisted** (gate filters `:proceed`; `/tmp/probe_conflict.clj` confirms `req_c/decision/conflict` is absent). This satisfies Entity 1 ("exactly one decision per request-id") and C8 (no double-apply); see finding F3 for the read-surface nuance. **Matches matrix.**
- **e1 × any write (event-id collision from a DIFFERENT request)**: `guard-event-collision` rejects `:event-id-conflict`; committed event immutable. Verified ("victim" survives). **Matches matrix** (the pre-fix Entity 2 FAIL is resolved).
- **a1/a2 × second accepted ingest (A3 either-way invariants)**: head is a single `termval` swap (508); one artifact row, `:root-event-id` traceable; `read-units` is whole-map `termval` (510) so no mixed-revision set; **prior judgments cannot silently re-attach** — unit ids are revision-scoped (292), so the new revision's line n has a NEW unit id and the old `:rejected` status (keyed by the old revision-scoped id) does not match. Verified by the re-attach probe: after re-ingest of "gamma\ndelta", discarded view is `[]`, canonical is `["gamma" "delta"]`, the old judgment remains durable audit state, and a late judgment on the dead unit rejects `:target-unit-not-found`. The pre-fix Entity 3 re-attach FAIL is **resolved.** **Matches matrix.**
- **a2 × accepted status-set**: status row under (artifact, branch, unit) with reason/event id (394-402,515); canonical loses / discarded gains the row; `read-units`, artifact, head unchanged. Verified by `rama-v0-text-loop-test`. **Matches.**
- **w0 (empty world)**: all reads nil/{} ; ingest decidable from empty state; status-set rejects `:target-unit-not-found`; `authorized-request?` passes `:system` actors and checks self-asserted caps otherwise (no persisted policy — consistent with the spec's V0 genesis rule). **Matches.**

## C1–C19 enforcement check

| Req | Where enforced (post-fix) | Verdict |
|---|---|---|
| C1 three envelopes | `valid-request?`/`valid-event?` mutually exclusive (a valid request must NOT carry top-level `:event/id`, core.clj:252-254; a valid event must, 304-306). Constructors/validators live in core; **text_kernel's duplicate copies removed** (`compat-record-request`, `interpret-compat-request`, `decide-event`, `unknown-action-decision`, `compat-request->event`, `artifact-id-from-unit-id` are now `:refer`'d from core, text_kernel.clj:5-10; grep confirms no `defn` of these in text_kernel). The pre-fix C1 duplication-drift FAIL is resolved. | **PASS** |
| C2 requests before decisions | Only client-appendable depot is `*text-requests-depot`; an appended event fails request validation; `$$events-by-id` written only in the topology accept gate (499). | **PASS** |
| C3 envelope validation | `request-validation-errors` (core.clj:225-296) collects all applicable typed errors pre-dispatch; now **strengthened** — `:request/id-invalid` (243-246), `:request/time-ms-invalid` (248-250), `:request/top-level-event-id` (252-254), `:request/payload-event-id` (256-258), action-type drift (268-271), routing-key (273-278), actor/target kind, and a tightened `:branch/missing-id` requiring a non-empty String (292-296). **Still missing:** no `:proposed/event-id` presence check and no `:action/capability` presence check (`required-request-keys`, 217-219, omits both). The spec lists capability as mandatory NOW (Entity 6) and proposed-event-id in the envelope (C3). The typed helpers always supply both, but a hand-built request omitting them validates clean. NOT a fix-4 claim; pre-existing minor drift. | **FAIL (minor)** — finding F4; carryover |
| C4 routing-key discipline | Ingress `(hash-by :routing/key)` (405); decision copies the key verbatim (core.clj:359,370); reads route via `:pkey`; **status event ordering key = routing key** (116, the pre-fix mismatch fixed); **zero partitioner hops ⇒ per-key single-event atomicity is structural** (verified). Both C4 requirements — same-key mutual ordering and per-request atomic visibility — now hold. | **PASS** (pre-fix major resolved) |
| C5 identity headers | Payload smuggling and top-level `:event/id` rejected; smuggled ids never become events; accepted event id = proposed id; **proposed id now always minted client-side** (text_kernel.clj:142,170; core.clj:440), deterministic fallback only; **event-id collision guard** protects e1 immutability (357-367). | **PASS** (pre-fix minor + collision-overwrite resolved) |
| C6 decision contract | Status ∈ {accepted, rejected}; same routing key; specific reasons (`:request-invalid`, `:actor-not-authorized`, `:target-unit-not-found`, `:unit-status-invalid`, `:revision-exists`, `:artifact-payload-invalid`, `:event-id-conflict`, `:compat-type-not-allowed`, `:compat-payload-invalid`, `:unknown-action-type`, `:derived-event-invalid`, `:request-id-conflict`); structured `:errors`; rejected path writes only request+decision (no event). **Exactly-one-durable-decision is now stable**: `:decided-at` from request time (no rewrite on replay); dedup gate prevents accepted→rejected flip; the pre-fix orphan-event interleaving is impossible under microbatch + zero hop. | **PASS** (pre-fix major resolved) |
| C7 mint before ingress | request-id/artifact-id/revision-id minted client-side; **proposed-event-id now minted client-side too** (142,170,440); no randomness in the topology. | **PASS** (pre-fix minor resolved) |
| C8 determinism laws | **Deterministic acceptance: PASS** — `:decided-at` from request time; interpret is a pure fn of request + colocated state read on the same task; no hop-races (zero partitioners). **Idempotency: PASS** — dedup gate (decision-existence + fingerprint) + termval-only + collision guard; replay/conflict are total no-ops. **Replay-invariance:** spec marks it stated-but-unverified (A10); the wall-clock stamps that made it structurally unreachable pre-fix are gone, so it is now *designed-for* — but no cold-replay oracle exists (A10 deferred). | **PASS** for the two hard laws; replay-invariance designed-for, oracle deferred (A10) |
| C9 interpret pattern | Type-dispatched `interpret-*` with `default>` ⇒ `:unknown-action-type` (454-480); unknown types reject, don't crash; envelope validation strictly precedes dispatch (446 computed, 455 first branch). | **PASS** |
| C10 materialization pattern | Pure per-event-type fns (artifact-/revision-/branch-/status-materialization, units-by-id-materialization, 313-402), invoked only under the `accepted-decision?` gate (493); derived rows carry provenance (`:root-event-id` 383,392; `:created-by-event/id` 319; status `:event/id` 402). | **PASS** |
| C11 depot-family taxonomy | Exactly one intent depot (405); no claim/obs/control — intent-only, structure earned (header 17-23). | **PASS** |
| C12 back-arrow | Inside the kernel: no UI side channel; reads are PStates. The util_fns mirror atoms are now **explicitly quarantined out of the kernel contract** (util_fns.cljc:79-119 `transitional-mirror-quarantine`, test-enforced): declared non-kernel, non-durable, reset-on-restart, no rebuild path, with the S40 `foreign-proxy-async` crash documented as why they can't yet be PState reads. This converts the pre-fix *silent* C12 violation (atoms-as-kernel-truth) into a *declared, bounded, test-asserted* non-kernel mirror. Against C12 (which governs **kernel** state and the back-arrow), the kernel itself no longer has a non-PState truth surface; the mirrors are explicitly outside it. | **PASS (in-scope)** — the contract-layer C12 violation is resolved by quarantine; the residual durability gap is finding F2 (bounded) + DEFERRED (IPC substrate) |
| C13 ack semantics | `:append-ack` hardcoded in the single append helper (548); outcomes readable by request id / domain id; `await-*` poll helpers (610-627); microbatch consumer cannot couple ack to processing. | **PASS** |
| C14 cross-kernel wires | N/A — no outbound wires from the text kernel. | **PASS (N/A)** |
| C15 executor option | N/A — no TaskGlobal/executor; work is in-topology materialization. | **PASS (N/A)** |
| C16 naming/vocabulary | `projection-*` convention ✓ (`$$projection-cache`). Primary tables are still `$$requests-by-id`/`$$decisions-by-id`/`$$events-by-id`/`$$units-by-artifact` — `-by-*`/`-by-id` is C16's reserved secondary-index suffix, used here on primaries. No keyword collisions; vocabulary (text-kernel) matches the lock. The `-by-id` naming is on the KNOWN DEFERRED list ("C16 -by-id primary naming"). | **FAIL (minor) — DEFERRED** |
| C17 scaffolding declared | `$$policies` (419) and `$$projection-cache` (426): zero writers (grep `local-transform>.*policies` / `…projection-cache`: none), declared scaffolds in KERNEL-SHAPE (kernel.clj:749-750,779,788). `$$branches` IS written now (500), correctly NOT claimed as scaffold. | **PASS** |
| C18 shape maintenance | KERNEL-SHAPE updated in the same change: ingress partitioner `:routing/key` + microbatch/zero-hop note (kernel.clj:543,554-562); intent depot `*text-requests-depot` (575); ack `:append-ack`; claim/obs/control absent; interpret dispatch types + `:decided-at` from request time + revision-scoped ids + allow-listed compat (690-696); the five materialize fns listed (713-717) all exist and match; PState count 11 = 9 written + 2 scaffold (760) — verified by `declare-pstate` count and the written-table grep; status layout artifact→branch→unit documented (755-758); projections scaffold-only `[$$projection-cache]` (788). One imprecision: `:interpret-fn :signature (fn [request existing-state] …)` (688) holds for ingest (existing-revision) and status (unit) interpreters but compat/default take request alone — a descriptive-text breadth note, not a defect. | **PASS** |
| C19 projections preserve identity | `projection-item` rows carry `:target {:target/kind :unit, :target/id, :target/address}`, `:unit/id`, `:preview`, `:provenance {:root-event-id, :created-by-event/id}` (657-672); derived at read time from truth tables, never read as truth by interpret; disposable. | **PASS** |

---

## Self-consistency check (phase step 6)

Re-read complete. Every check whose body acknowledges a defect is marked FAIL: non-subindexed collections (FAIL, DEFERRED), caches-survive-restart (FAIL — finding F2 + DEFERRED), C3 capability/proposed-event-id presence (FAIL minor — F4), C16 `-by-id` naming (FAIL minor — DEFERRED). No check is passed while its body acknowledges a behavior defect in scope. The PASS verdicts on C4/C6/C8/Op-A1/A3/A5, event immutability, and judgment re-attach are each backed by a green probe assertion, not just code reading. The two "PASS with note" items (C18 signature breadth, the dedup-fingerprint classification note under idempotency) are precision/robustness observations that do not describe a correctness defect; F1 and F3 capture them as findings without flipping the corresponding correctness PASS.

## Failed-checks summary

| Check | In-scope? | Severity |
|---|---|---|
| Non-subindexed collections (3 inner maps) | **DEFERRED** (K-06) | schema restructuring |
| Caches survive restart — mirror atoms no rebuild path | **IN-SCOPE (F2)** + DEFERRED (IPC substrate) | bounded/declared (in-scope); deployment gap (deferred) |
| C3 — no `:proposed/event-id` / `:action/capability` presence check | carryover (not a fix-4 claim) — **F4 minor** | line edits |
| C16 — `-by-id` on primaries | **DEFERRED** | naming |
| Dedup fingerprint includes `:request/time-ms` (replay→conflict misclassification) | **IN-SCOPE (F1)** | robustness, truth still safe |
| Conflict decision not durably readable at request-id read surface | **IN-SCOPE (F3)** | spec-consistent; observation |

Everything the pre-fix R4 verdict charged as a **major** (C4 atomicity/serialization, C6/C8 decision stability + determinism + idempotency, Entity 2 event immutability, Entity 3 judgment re-attach, Op A5 poison records, C12 atom-mirrors-as-kernel-truth) is **resolved** in fix session 4 and confirmed by green adversarial probes. The remaining in-scope findings (F1–F4) are all line-edit-fixable minors that do not compromise committed truth.

## Verdict

The fix-session-4 architectural rewrite achieves its claims: microbatch + zero partitioner hops gives per-key serialization and per-request atomic commit (C4/C6/C8); the dedup gate + event-id collision guard make replays and conflicting reuses total no-ops and committed events immutable (C8/Entity 2); revision-scoped unit ids stop judgment re-attach (Entity 3); ingress surrogate keys + storable-request + pstate-key eliminate poison records (Op A5); `:decided-at` from request time removes wall-clock non-determinism (C8); the compat allow-list closes the arbitrary-event hole (06/F3); duplicate contract fns and `select-pstate-one` are removed; the util_fns mirror atoms are quarantined out of the kernel contract by a test-enforced declaration (C12). All in-scope claims hold under both green test suites (110 assertions) and four independent adversarial probes.

Remaining failures are either DEFERRED by the session's own scope (subindexing/schemas K-06, `-by-id` naming, IPC-as-production substrate, defrecord leaf schemas, projection materialization, A10 replay oracle) or IN-SCOPE minors fixable by line edits (F1 fingerprint dissoc, F2 mirror-atom durability bound, F4 envelope presence checks) plus one spec-consistent observation (F3). None requires architectural restructuring.

**VERDICT: conditional-pass in-scope (4 line-edit-fixable minors F1–F4, none compromising committed truth); major-fail outstanding (DEFERRED scope only — subindexing, IPC substrate, `-by-id` naming, defrecord schemas, projection table, A10 replay oracle).**

PHASE_VALIDATION:minor-fail

---

## Same-session addendum (fix session 4, after this validation ran)

F1 and the safe half of F4 were fixed and probe-pinned in the same session:

- **F1 FIXED** — `text_kernel/dedup-request-view` drops `:request/time-ms` before
  both the gate read and the fingerprint stamp, so a client retry that re-mints
  time classifies as `:replay`, not `:conflict`. Pinned by
  `dedup-gate-classifies-reminted-time-as-replay-test` (pure: replay on
  re-minted time, conflict on changed content) plus a live probe re-appending
  the committed request with `:request/time-ms 99` (truth unchanged,
  `:decided-at` stays the first delivery's clock).
- **F4 PARTIALLY FIXED** — `:action/capability` presence is now validated
  (`:action/capability-missing`, Entity 6 "mandatory NOW"); every in-repo
  envelope constructor already supplies it (text helpers, compat, all four
  object-container adapters). The `:proposed/event-id` presence check is NOT
  added: the object-container kernel's ingress (`core/action-request` callers
  at object_container.clj:1657 and the three adapters) does not mint header
  event ids yet, so enforcing presence in the shared validator would reject
  another kernel's live traffic — out of this session's scope. Carryover
  flagged for the object-container track; kernel-side identity remains
  deterministic via the `(str request-id "/event")` fallback.
- **F2** stands as the session's chosen scope (quarantine, not durability).
- **F3** stands as a spec-consistent observation (first-write-wins, exactly one
  decision per request id; conflicts are total no-ops like the compute/llm
  template).

Post-addendum sweep: all suites green — batch 1 (text, text-probe, core-guards,
kernel-shape, probe-harness, object-container, transcript-ingest): 86 tests /
545 assertions; batch 2 (compute, compute-probe, space, space-probe, llm,
llm-probe, transcript): 48 tests / 569 assertions.
