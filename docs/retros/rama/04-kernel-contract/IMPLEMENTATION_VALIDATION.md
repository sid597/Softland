# Implementation Validation

<!-- Phase 4, RETROSPECTIVE mode. Subject: the kernel contract layer built BEFORE
     the rama skill existed —
       src/app/server/rama/text_kernel.clj  (text-kernel-module, V0/V1)
       src/app/server/rama/core.clj         (shared envelope contracts)
       src/app/server/rama/util_fns.cljc    (adapter / compatibility layer)
     Validated against IMPLICIT_SPEC.md (the spec) and PLAN.md (the code-blind
     re-derived plan), under the skill's production rules. kernel.clj is treated
     as the as-built contract artifact whose KERNEL-SHAPE claims are checked
     against the text instance.
     Retro rule: fail verdicts are recorded as the retro result; no fix loop.
     Default verdict: major-fail. Every check below carries file:line citations
     and a runtime trace. -->

Review all topology, query topology, and foreign client code. For each check, state pass or fail with evidence. Then emit one of three verdicts at the end of this artifact, per the rubric below.

## Rama semantics relied on (citations)

- Foreign PState queries "automatically route to the correct partition based on the path's first key" — `references/core-concepts.md:49`. This is what makes the module's read helpers line up with its `|hash`-keyed writes.
- Stream topologies are at-least-once; a record can be retried "even after all PState writes have completed and committed"; every write must be idempotent or deduplicated — `references/stream.md:3,9`.
- "An event that hops across tasks via partitioners creates work in different streaming batches on different tasks. Those streaming batches commit independently. … On retry, the entire event replays from the source" — `references/stream.md:96,115-122,168`.
- Ordering is pairwise FIFO only: "for any two tasks A, B, if A sends events e₁, e₂, e₃ to B, then B processes them in order" — `references/stream.md:31`. No ordering guarantee across different multi-hop chains that re-converge.
- Default stream retry mode is `:individual` — retry the failed record only; subsequent records on the same partition are NOT held back — `references/stream.md:86-88,158`.
- "Narrow schemas (e.g. `{String String}`) reject mismatched writes"; maximal schema validation defaults on — `references/pstate-schema.md:152-163`, `references/syntax.md:140`.
- Partition alignment: `local-select>`/`local-transform>` operate on the current task; misalignment is silent — `references/core-concepts.md:77`, SKILL.md goal 5.

## Plan vs implementation divergence (phase step 3)

The implementation predates the plan (retro mode: PLAN.md was re-derived code-blind from the spec). The divergences are structural, not cosmetic:

| # | PLAN.md says | Implementation does | Where |
|---|---|---|---|
| D1 | Microbatch topology, exactly-once | **Stream topology**, at-least-once | text_kernel.clj:397 |
| D2 | **Zero partitioners** in topology body; all writes colocated on `hash(routing-key)` ⇒ per-key single-event atomicity | **Five `|hash` hops per accepted ingest** (`*request-id`, `*decision-id`, `*event-id`, `*branch-id`, `*artifact-id`); writes scattered across tasks, committing independently | text_kernel.clj:420,422,442,444,446 |
| D3 | Dedup guard (decision-exists check) as first step per record | No dedup guard anywhere | text_kernel.clj:410-499 |
| D4 | Event-id collision guard on accept path (`:event-id-conflict`) | No collision guard; `termval` overwrites an existing event | text_kernel.clj:443,471,489 |
| D5 | Typed defrecord leaf classes; `Object` schema banned | `(map-schema Keyword Object)` on 9 of 11 PStates; plain maps for all envelopes | text_kernel.clj:398-408 |
| D6 | Unitization is its own request type `:artifact/unitize` through the intent ingress | Unitization is an internal derivation inside the ingest accept path; `unitize-lines!` is a read-await, appends nothing | text_kernel.clj:441,450,603-609 |
| D7 | Unit ids revision-scoped (`<artifact>/<revision>/line/<n>`) so judgments never re-attach across revisions | Unit ids are `<artifact>/line/<n>` — **not** revision-scoped | text_kernel.clj:310 |
| D8 | Surrogate audit key for id-less garbage so every append gets a decision | Request id used raw; nil/non-String ids hit the schema | text_kernel.clj:412,421 |
| D9 | `decided-at`/time copied from request, never wall clock | `(now-ms)` stamped inside both decision constructors | core.clj:318,330 |
| D10 | Status domain `#{:rejected :canonical}`; non-default branch rejected `:branch-not-found` | Domain `#{:accepted :rejected :hidden :promoted :superseded}`; any non-nil branch accepted | text_kernel.clj:37-38,267-268; core.clj:255 |
| D11 | `""` → 0 units; trailing newline drops one empty segment | `str/split #"\n" -1`: `""` → 1 empty unit; trailing newline yields a trailing empty unit | text_kernel.clj:285 |
| D12 | Decisions keyed by request-id | Keyed by `request-id + "/decision"`; readable via `decision-id-for-request-id` (contract still met) | core.clj:305-307; text_kernel.clj:542-545 |
| D13 | `$$projection-views` materialized table | Projections computed client-side per read (`read-unit-projection`); `$$projection-cache` scaffold unwritten | text_kernel.clj:634-655 |

Divergence does not itself decide the verdict — the spec does. But D1+D2 remove the mechanism the plan used to satisfy C4/C6/C8, and the implementation supplies no replacement (traced below).

## Redundant conditionals

**Check:** if every branch of an `<<if`, `<<cond`, or `<<switch` does the same operation with only a variable differing, replace with a single operation using that variable directly.

**Evidence:** All five `<<cond` branches (text_kernel.clj:417-499) end with the identical 5-op tail, differing only in how `*decision` is produced:

```
(decision-id *decision :> *decision-id)
(|hash *request-id)
(local-transform> [(keypath *request-id) (termval *request)] $$requests-by-id)
(|hash *decision-id)
(local-transform> [(keypath *decision-id) (termval *decision)] $$decisions-by-id)
```

appears verbatim at 419-423, 427-431, 458-462, 478-482, 495-499.

**Trace:** every record, regardless of branch, executes the same request+decision writes. The branches need only compute `*decision` (and emit `*event` materializations); the shared tail could be hoisted after the `<<cond>` emits `*decision`, removing 4 duplicated copies and 4 duplicated partitioner declarations.

**Verdict: FAIL.** Mechanical restructuring; identical operations duplicated across all branches.

## Consecutive keypath

**Check:** `(keypath *a) (keypath *b)` → `(keypath *a *b)`.

**Evidence:**
- text_kernel.clj:449 — `[(keypath *artifact-id) (keypath *revision-id) (termval *revision)]` on `$$text-revisions`.
- text_kernel.clj:456 — `[(keypath *artifact-id) (keypath *unit-id)]` on `$$units-by-artifact`.
- text_kernel.clj:474 — `[(keypath *branch-id) (keypath *unit-id) (termval *status)]` on `$$unit-status-by-branch`.
- text_kernel.clj:562 — `[(keypath artifact-id) (keypath revision-id)]` in `read-text-head`.

**Verdict: FAIL.** Four sites; each is a one-line merge into a single `keypath`.

## Select-compute-transform

**Check:** `local-select>` followed by computation followed by `local-transform>` with `termval` — replace with `+compound` and an aggregator when possible.

**Evidence:** The only in-topology select is text_kernel.clj:456 (`$$units-by-artifact` unit probe). Its result feeds `interpret-status-request` (457) for accept/reject validation; the subsequent status write (474) derives its value from the event, not from the read value. No read-modify-write of the same location exists, so `+compound` does not apply.

**Verdict: PASS.**

## Unnecessary nil->val

**Check:** navigators handle nil as empty collection — do not add `nil->val` unless the next navigator requires a non-nil value.

**Evidence:** `grep nil->val` over text_kernel.clj/core.clj: zero occurrences. Client-side `(or … {})` defaults (text_kernel.clj:566,570) are JVM-side, not path navigation.

**Verdict: PASS.**

## :allow-yield?

**Check:** `local-select>`/`select>` iterating a subindexed structure beyond ~100 entries needs `{:allow-yield? true}`.

**Evidence:** The module declares **no** subindexed structures (text_kernel.clj:398-408). The single in-topology read (456) is a two-key point read. No topology read iterates a collection.

**Trace:** no iteration ⇒ nothing to yield. (The real defect — that these collections *should* be subindexed — is charged to the next check, not this one.)

**Verdict: PASS** (vacuously).

## Non-subindexed collections without size limits

**Check:** for every write to a non-subindexed inner collection, verify the application explicitly enforces a maximum size; otherwise it must be subindexed.

**Evidence and traces:**

1. `$$units-by-artifact {String {String (map-schema Keyword Object)}}` (text_kernel.clj:406), written at 450 with `(termval *units)` — the whole units map, one entry per line. Spec Op A1/A2 explicitly scales per-line: "very large documents (per-line unit fan-out)", "units per artifact bounded by line count" with unbounded line count. No code caps line count (`line-ranges`, 283-300, loops over all lines). A 100k-line document serializes a 100k-entry map as one RocksDB value and rewrites it whole on every re-ingest. **FAIL.**
2. `$$unit-status-by-branch {String {String (map-schema Keyword Object)}}` (407), written at 474 one entry per accepted judgment under the **branch** top-level key. Spec: "One entry per (branch × unit) … unbounded over time." V0 has exactly one branch (`default-branch-id`, core.clj:12), so every judgment of every artifact accumulates in ONE inner map on ONE task — unbounded, uncapped, and a storage/throughput hotspot (SKILL.md goals 1-2). **FAIL.**
3. `$$text-revisions {String {String (map-schema Keyword Object)}}` (405), written at 449 one entry per revision, each carrying the **full document content** (`revision-materialization`, 376-383). Revisions are unbounded (spec: "Artifacts, revisions … grow without bound and are never deleted"). Non-subindexed ⇒ all revisions of an artifact are one serialized blob; each ingest rewrites every prior revision's content, and `read-text-head` (559-562) deserializes the entire revision history to return one revision. Unbounded write amplification. **FAIL.**

Bounded row-maps (`$$artifacts`, `$$branches`, `$$decisions-by-id` values etc.) are fixed-field rows, not growing collections — fine.

**Verdict: FAIL.** Three unbounded inner collections require schema changes (`{:subindex? true}`) and corresponding read/write restructuring.

## Stream topology idempotency

**Check:** trace what happens if any event retries; every write and side effect must be duplicate-safe.

**Traces (retry of one depot record, per stream.md:9,96):**

1. **Wall clock in decisions — non-idempotent value.** `accepted-decision` and `rejected-decision` stamp `:decided-at (now-ms)` (core.clj:318,330). On retry, interpretation re-runs and the decision row is rewritten with a different `:decided-at`. The "exactly one durable decision" row exists, but its content is not stable under retry — violating the spec's s2/s3 immutability ("read-decision … immutable forever") and C8's "no wall clock … inside interpretation". **FAIL.**
2. **Re-interpretation against later state can flip a committed decision and orphan an event.** Scenario: status-set S for unit u is processed; the streaming batches that write `$$requests-by-id` (460), `$$decisions-by-id` (462, accepted), and `$$events-by-id` (471) commit; the final batch (`$$unit-status-by-branch`, 474) fails. Retry replays S from source (stream.md:115). Because retry mode is `:individual` (no `:retry-mode` option at text_kernel.clj:411 ⇒ default, stream.md:158), other records — e.g. a re-ingest that replaces the unit map via `(termval *units)` at 450, shrinking the document so u's line no longer exists — can commit between attempts. The retried `local-select>` (456) now returns nil ⇒ `interpret-status-request` returns `:target-unit-not-found` (text_kernel.clj:267) ⇒ the decision row is **overwritten as `:rejected` with `:event/id nil`** while the attempt-1 event remains durable in `$$events-by-id` forever. Result: a rejected decision coexisting with a readable KernelEvent under the proposed id — violating C6 ("rejected requests never create world state"), the entity matrix ("read-event: nil — forever" for rejected), and C8 deterministic acceptance (decision depends on retry timing, not on the request's position in the key's history). **FAIL.**
3. **Duplicate client append (same request-id) is not deduplicated.** Plan's dedup guard is absent. Trace: client retries `foreign-append!` after an ack timeout ⇒ second depot record, full reprocessing. With identical content the writes are `termval`-idempotent except `:decided-at` (changes) and the re-interpretation hazard in (2). With *different* content under the same `request-id` (buggy client), the request row, decision row, **and the event row under the same derived event id** (`(str request-id "/event")`, text_kernel.clj:188,207,225) are silently overwritten — mutating audit history and an existing KernelEvent, violating Entity 2 e1 ("events are immutable; no write may modify an existing event") and Entity 1 ("never edited"). The same overwrite occurs when a *different* request smuggles an existing `:proposed/event-id` through the header (no collision guard at 443/471/489). **FAIL.**
4. **IDs generated inside the topology?** No randomness: event-id fallback is the deterministic `(str request-id "/event")` (188,207,225); `kernel-event`'s `(random-id "evt")` fallback (core.clj:180) is unreachable from the three `request->event` paths since they always pass a non-nil event-id. Replay produces the same ids. PASS for this sub-item.
5. **`depot-partition-append!` to internal depots?** None exist. PASS for this sub-item.

**Verdict: FAIL** (items 1-3).

## Partial failure in stream topologies

**Check:** for each stream event writing multiple PStates across multiple partitions, consider failure+retry after partial commit; can any write be left permanently unexecuted, and what do readers observe?

**Evidence:** One accepted ingest writes across **five** independently-committing streaming batches: hash(request-id) → `$$requests-by-id` (421/429); hash(decision-id) → `$$decisions-by-id` (431); hash(event-id) → `$$events-by-id` (443); hash(branch-id) → `$$branches` (445); hash(artifact-id) → `$$artifacts` + `$$artifact-heads` + `$$text-revisions` + `$$units-by-artifact` (447-450, atomic together). The status path spans four batches (460,462,471,473-474).

**Traces:**

1. **Permanently unexecuted writes:** under `:individual` retry the record replays from source until the whole tree succeeds, and all writes are `termval` keyed by request-derived values, so no write is *permanently* lost in the success case. But see trace 2 of the idempotency check: the replayed interpretation can take the **reject** branch, in which case the materialization writes of attempt 1 that did *not* commit (e.g. the status row) are permanently unexecuted while the ones that did (the event row) permanently remain — a torn, internally inconsistent outcome that no subsequent processing repairs. **FAIL.**
2. **Per-key serialization is broken by multi-hop re-convergence.** Spec Op A1: "Two concurrent ingests with the same routing key must be serialized — decisions observe all previously accepted facts for that key"; C8: the decision is a function of state "at its position in the key's serialized history". The depot serializes same-key records at ingress (hash-by `:routing/key`, 396), but each record's effects travel a different hop chain (request-ids/decision-ids/event-ids hash to different tasks). Pairwise FIFO (stream.md:31) does not order chains that diverge and re-converge. Trace: ingest I and status-set S for the same artifact appended in order I, S. I's unit write reaches hash(artifact-id) via T0→…→T_units (5 hops); S's unit *read* reaches the same task via T0→T_units (1 hop). S's read can arrive first ⇒ S rejects `:target-unit-not-found` even though I precedes it on the same routing key's timeline. The decision is a race outcome, not a function of the key's serialized history. **FAIL** (C8 deterministic acceptance).
3. **Last-accepted-wins is not guaranteed.** Spec Op A3: two status-sets racing on one unit serialize per routing key; "the later accepted one wins the stored status." S1 and S2 (appended in that order) take chains T0→T_units→hash(req₁)→hash(dec₁)→hash(evt₁)→T_branch vs …req₂/dec₂/evt₂…→T_branch. The intermediate tasks differ per record, so arrival order at T_branch is unordered: S1's `termval` (474) can land after S2's, leaving the **earlier** judgment as the stored status forever. **FAIL.**
4. **Record-order inversion via `:individual` retry.** Ingest I1 fails downstream and awaits retry; re-ingest I2 (same artifact) fully commits in the interim; I1's retry then overwrites `$$artifact-heads`/`$$text-revisions`/`$$units-by-artifact` (448-450) with revision-1 state. The head permanently regresses to old content while `$$events-by-id`/`$$decisions-by-id` record I2 as the later accepted fact. Violates Entity 3 ("head moves to the new revision atomically per routing key") and replay-style monotonicity. The plan prevented this class structurally (zero partitioners + exactly-once); the implementation has no guard. **FAIL.**
5. **Transient cross-PState visibility.** A reader can observe an accepted decision while `read-event` of its `:event/id` still returns nil (decision batch committed, event batch not), i.e. state changes of one request are not "visible atomically with respect to that key" (C4). The artifact-level tables themselves (artifact/head/revision/units) are written in ONE batch (447-450), so no half-materialized *artifact* is observable — that narrow matrix clause holds — but C4's atomicity claim across the request's full effect set does not. **FAIL** (C4).

**Verdict: FAIL.**

## Single depot append per client operation

**Check:** each client write operation must call `foreign-append!` exactly once.

**Evidence:** The only `foreign-append!` site is `append-action-request!` (text_kernel.clj:529-532). `ingest-text!` (597-601), `set-unit-status!` (611-615), and every util_fns write helper (`append-compat-event!` util_fns.cljc:94-103; `update-event-id` 105; `register-user` 115; `update-user-setting` 139; `update-cli-session` 155; `submit-agent-run` 172; `emit-sidebar-event!` 237; `emit-settings-event!` 252; `save-agent-trail!` 272; `emit-workspace-truth-event!` 290; `save-editor-doc!` 305; `emit-flow-session-event!` 324) each append exactly one envelope per operation. `unitize-lines!` (603-609) appends nothing. Atom `swap!`s alongside appends are not depot writes (they are charged to the back-arrow check below).

**Verdict: PASS.**

## Application-state caches survive restart

**Check:** for each TaskGlobal or in-process cache holding application state: name a durable source sufficient to rebuild it, and cite the concrete rebuild path.

**Evidence and traces:**

1. **util_fns transitional mirror atoms** (util_fns.cljc:81-88): `!cli-sessions`, `!agent-runs`, `!sidebar-truth-atom`, `!settings-truth-atom`, `!agent-trail-atom`, `!workspace-truth-atom`, `!editor-doc-atom`, `!flow-session-atom`. These are **read** as application state (`get-cli-session` 151, `get-agent-run` 198, `get-sidebar-state` 202, `get-settings-state` 248, `get-agent-trail` 263, `get-workspace-truth` 286, `get-editor-doc` 301, `get-flow-session-state` 320). Durable source: each write also appends a `:compat/record` request whose accepted event lands in `$$events-by-id` — so the data exists durably *in principle*. Rebuild path: **none exists**. No code reads `:compat/recorded`-era events back into the atoms; on JVM restart every atom re-initializes empty (`defonce` + literal initial values, 81-88) and stays empty until new writes arrive. Editor docs, agent runs, CLI session mappings, sidebar/settings/workspace state are all lost to readers. The check requires a *concrete, cited* rebuild path; there is none to cite. **FAIL.**
2. **`!kernel-runtime` (util_fns.cljc:12-17) wraps `start-text-runtime!`, which boots an InProcessCluster** (`create-ipc`, text_kernel.clj:503; import at text_kernel.clj:11). The production runtime IS the test harness. IPC state does not survive process restart, so the "durable" depot and PStates themselves are process-lifetime only in the deployed configuration — there is no durable source at all behind the whole kernel as wired. SKILL.md: "InProcessCluster is a test harness — production has node failures, process crashes, and retries." Op A7's "state durability across restart is the substrate's job" is unmet because the chosen substrate instantiation is non-durable. **FAIL.**

**Verdict: FAIL.**

## No reimplementation of built-in operations

**Check:** scan for custom code duplicating Rama built-ins.

**Evidence:**
- `select-pstate-one` (text_kernel.clj:534-536) — `(first (foreign-select path pstate))` — reimplements `foreign-select-one` (named as the built-in point-read in core-concepts.md:49). Every read helper funnels through it. **FAIL** (one-line fix: use `foreign-select-one`).
- `random-id` (core.clj:56-58) wraps `java.util.UUID/randomUUID` with a prefix; prefixed-id construction is not a Rama built-in (the plan's `ops/random-uuid7` is an alternative, not a duplicated built-in semantics) — not charged.
- `now-ms`, `sha-256` (core.clj:51-54,60-70) — no Rama equivalents. Not charged.

**Verdict: FAIL** (minor — `select-pstate-one`).

---

# Spec validation (IMPLICIT_SPEC.md)

The phase doc forbids validating against the plan alone. Below: Layer (a) operations and the entity matrix, then the C1–C19 enforcement map checked against where (or whether) each requirement is actually enforced in code.

## Layer (a) operations

**Op A1 (ingest):** Accepted ingest produces one `:artifact/ingested` event with `:event/id` = `:proposed/event-id` when supplied (text_kernel.clj:187-188); target `{:target/kind :artifact, :target/id …, :target/address nil}` and payload `:artifact/type :text` (58-69) — carrier-independent envelope **holds**. `[:ordering :key]` = `[:artifact artifact-id]` = `:routing/key` (71 vs 126) **holds**. Artifact `:root-event-id` = ingest event id (374) **holds** for first ingest. Head round-trip: content stored verbatim via `(str content)` (50,381) and returned by `read-text-head` (559-562) — byte-for-byte for any String input **holds**. Durable-before-decision (request write precedes nothing observable about acceptance, both written in topology after durable append) **holds** in spirit but request/decision land in different batches — see C4. Concurrency invariant ("decisions observe all previously accepted facts for that key") **FAILS** — multi-hop re-convergence trace above. Revision immutability: a second ingest with the same artifact-id+revision-id silently overwrites the stored revision row (449, `termval`) — "Revisions are immutable once accepted" is **not enforced** (no existence check in `interpret-ingest-request`, 252-258, which reads no durable state at all). **FAIL.**

**Op A2 (unitization):** Internal derivation of the accepted event (A6 resolved differently from the plan; spec leaves it open — acceptable). One unit per line in document order with `:unit/preview` = line text (302-329) **holds**; `:text/range` anchor (317-321) **holds**; `[:provenance :root-event-id]` = ingest event id (326-328,331-339) **holds**; deterministic ids `<artifact>/line/<n>` (310) — stable and re-derivable **holds**, but not revision-scoped — see Entity 3 FAIL below. Idempotent re-derivation (pure fn of event + `termval`) **holds**. Derives from the accepted event only (441: `units-by-id-materialization *event` inside the `accepted-decision?` gate) **holds**.

**Op A3 (status-set):** Decision derived from durable unit existence (local-select 456 → nil ⇒ `:target-unit-not-found`, 267) **holds** on the happy path, subject to the race/retry FAILs above. Branch-scoped storage (`[branch-id unit-id]`, 474) and per-branch reads (568-570) **hold**; non-default branches are accepted-and-written without touching default-branch state (A11 left open by spec; default-branch isolation **holds**). `:reason` preserved (98-101,390) **holds**. Accepted event `:event/id` = proposed id (207) **holds**. **But the event's ordering key is `[:unit unit-id]` (103) while the request's routing key is `[:artifact artifact-id]` (153)** — C4/A1's "`[:ordering :key]` = `:routing/key`" is violated for every status event. "Later accepted wins" **FAILS** under re-convergence (trace above). **FAIL** (ordering-key mismatch + races).

**Op A4 (`:compat/record`):** Dispatched like any other type (476-491); full envelope validation applies (271-277). Payload contract intentionally thin (A8). However compat requests can never carry `:proposed/event-id` (`compat-record-request`, 163-178, has no slot) and the event id is **always** kernel-derived `(str request-id "/event")` (225) — identity is created during interpretation, not proposed in the header. Deterministic and audit-linkable, but contrary to C5's "event identity is proposed by the requester in the header." **Partial FAIL** (charged to C5/C7).

**Op A5 (raw append):** Verbatim durability: `termval` of the untouched request map (421 etc.) round-trips with value equality for keyword-keyed maps — `(= request (read-request …))` **holds** for every envelope the helpers can produce, including invalid ones. Validation-before-dispatch: validation is computed once per record (414) and the first `<<cond` case (417) tests it before any type `case>`, so a malformed unknown-type request rejects `:request-invalid`, not `:unknown-action-type` — **holds**. Well-formed unknown type ⇒ `default>` ⇒ `:unknown-action-type` (493-499) **holds**. **But "every appended request eventually receives exactly one durable decision … no silently dropped outcome" FAILS for garbage:** a request lacking `:request/id` (or with a non-String one, or a non-map, or a map with non-keyword keys) reaches `(local-transform> [(keypath *request-id) …] $$requests-by-id)` (421) with a nil/non-String key or a value violating `(map-schema Keyword Object)`; narrow schemas reject mismatched writes (pstate-schema.md "Narrow schemas … reject mismatched writes", syntax.md:140), the event throws, and under `:individual` retry the record retries forever — a poison record with no decision, ever. The plan's surrogate-key step existed precisely for this; the implementation has nothing. **FAIL.**

**Op A6 (reads):** All read helpers route by the path's first key, which matches the task each table was written on (`|hash` of the same key) — request/decision/event/artifact/head/units/statuses/branch reads are all partition-aligned (538-570 vs 420-474). Nonexistent ids ⇒ nil/{} without error **holds**. Projection rows carry `:target {:target/kind :unit, :target/id …}`, `:preview`, `:provenance {:root-event-id …}` (617-632) **holds**; canonical/discarded partition the units (complementary predicates over `discarded-statuses`, 639-641; unjudged ⇒ canonical) **holds**; document order via `sort-by :unit/order` (637) **holds**. `await-decision`/`await-materialized` poll without blind sleeps in callers (572-589) **holds**. Monotonic-per-key visibility **FAILS** in the retry-flip scenario (a visible accepted decision is later overwritten as rejected — "once visible, does not un-happen" violated). **FAIL** (monotonicity under retry; otherwise the read surface is contract-complete).

**Op A7 (lifecycle):** `start-text-runtime!` returns the handle through which all appends/reads flow (501-520) **holds**. `close-text-runtime!` idempotent, exception-swallowing (522-527); repeated start/close cycles create/destroy independent IPCs — no shared static state in this layer **holds**. Reads after close throw (client closed) — "error, not corruption" **holds**. Fresh runtime ⇒ empty world **holds**. Durability across restart: see Application-state-caches FAIL (IPC substrate). **Partial FAIL** (durability).

## Entity matrix spot-traces

- **s0 × invalid append (payload `:event/id`)**: validator returns `{:type :request/payload-event-id}` (core.clj:220-222) plus any other applicable errors (`cond->` collects all, not just the first — 204-256); branch 1 writes verbatim request + rejected decision with `:decision/reason :request-invalid`, `:errors` vector, `:event/id nil`, routing key copied (core.clj:320-330). Smuggled id never becomes an event (no event write on the reject path). Decision readable even with nil routing key (decision row routed by decision-id, not routing key). **Matches matrix.**
- **s1/s2/s3 × re-append same request-id**: last-write-wins overwrite, never a merge — allowed by the matrix — but the same-id event overwrite (idempotency trace 3) violates "no second materialization"/event immutability. **FAIL** (as charged above).
- **e1 × any write**: no guard prevents overwriting `$$events-by-id` at an existing key (443/471/489). **FAIL** (Entity 2).
- **a1/a2 × second accepted ingest (A3 either-way invariants)**: head is a single `termval` swap (448) — never a blend, **holds**. One artifact row, `:root-event-id` overwritten to the latest ingest's event id (374,447) — lineage remains traceable only via `$$text-revisions` rows' per-revision `:root-event-id` (383) — **weakly holds**. `read-units` presents only the new revision's units (whole-map `termval`, 450) — no mixed-revision set, **holds**. **"Prior judgments must not silently re-attach to different text" FAILS:** unit ids are `<artifact>/line/<n>` (310) and status rows are keyed by unit id alone (474); after re-ingest, the new revision's line n inherits the old revision's judgment — `read-unit-projection` joins by `:unit/id` (644), so a `:rejected` set on old text silently discards different new text. The unit's *anchor* is revision-scoped (318) but nothing consults it at join time. **FAIL** (Entity 3, pinned "regardless of A3 resolution").
- **a2 × accepted status-set**: status row appears under (branch, unit) with reason/event id (385-393,474); canonical loses the row, discarded gains it, others keep order (639-647); `read-units` unchanged; artifact/head unchanged. **Matches matrix.**
- **u1 × status on branch B′ ≠ B**: writes under B′ only (474); B unaffected. **Matches.**
- **w0 (empty world)**: all reads nil/{} (538-570, keypath on absent keys); ingest decidable from empty state (interpret-ingest reads no state); status-set rejects `:target-unit-not-found`. Genesis/capability: `authorized-request?` (core.clj:296-303) passes `:system` actors unconditionally and otherwise checks the request's own self-asserted `:actor/capabilities` — a static envelope check, no persisted policy (consistent with the spec's "V0 ships without persisted policy tables"). **Matches.**

## C1–C19 enforcement check

| Req | Where enforced (or not) | Verdict |
|---|---|---|
| C1 three envelopes | `valid-request?`/`valid-event?` mutually exclusive by construction: a valid request must NOT contain `:event/id` (core.clj:216-218) and a valid event must contain it (`required-event-keys`, core.clj:200-202, checked at 264-266), so no map satisfies both. Constructors/validators live in core. **But** text_kernel.clj duplicates `compat-record-request`, `compat-request->event`, `decide-event`, `interpret-compat-request`, `unknown-action-decision`, `artifact-id-from-unit-id` (text_kernel.clj:75-80,163-178,220-250,271-281 vs core.clj:107-127,344-403) — the topology uses its local copies while util_fns uses core's. Identical today; this is exactly the instance-level drift vector C1 assigns to the shared core to prevent. | **FAIL (minor)** — duplication; mutual exclusivity itself holds |
| C2 requests before decisions | Only client-appendable ingress is `*text-requests-depot`; an appended KernelEvent fails request validation (missing `:request/id` etc.) and is rejected; `$$events-by-id` written only inside the topology's accept gates (443,471,489). | **PASS** |
| C3 envelope validation | `request-validation-errors` (core.clj:204-256) collects all applicable typed errors pre-dispatch (topology branch order, text_kernel.clj:417-423): drift (233-236), payload event-id (220-222), top-level event-id (216-218), routing key nil/non-vector/empty/nil-element (238-243), actor/target/branch checks. **Missing:** no `:proposed/event-id` presence check (`required-request-keys`, core.clj:196-198, omits it) and no `:action/capability` presence check — both named by the spec as envelope content (C3, Entity 6 "mandatory NOW"); a request without either validates clean. | **FAIL (minor)** |
| C4 routing-key discipline | Ingress partitioner `(hash-by :routing/key)` (396) ✓; decision copies the key verbatim (core.clj:315,326) ✓; reads route consistently ✓. **But**: status events' `[:ordering :key]` = `[:unit unit-id]` ≠ routing key (103 vs 153); and per-key event-boundary atomicity does not exist — five partitioner hops per request, independently committing batches, re-convergence races (traces above). The discipline's two requirements — same-key mutual ordering and per-request atomic visibility — are both violated. | **FAIL (major)** |
| C5 identity headers | Payload smuggling rejected (`:request/payload-event-id`) and top-level `:event/id` rejected; smuggled ids never become events; accepted event id = proposed id when present (188,207); rejected ⇒ `:event/id nil` (core.clj:327). **But** proposed-event-id is optional in practice: when absent the kernel derives `(str request-id "/event")` during interpretation (188,207) and the compat path can never carry one (225) — identity assignment happens inside the kernel, deterministic but not header-proposed. No event-id collision guard protects e1 immutability. | **FAIL (minor for derivation; the collision overwrite is charged major under C6/Entity 2)** |
| C6 decision contract | Status ∈ {accepted, rejected} ✓ (core.clj:312,323); same routing key ✓; specific machine-readable reasons ✓ (`:request-invalid`, `:actor-not-authorized`, `:target-unit-not-found`, `:unit-status-invalid`, `:unknown-action-type`, `:derived-event-invalid`); structured `:errors` ✓; rejected path writes only request+decision (no event/materialization inside the reject branches) ✓ structurally. **But** exactly-one-durable-decision is not stable: retry rewrites `:decided-at`; retry can flip accepted→rejected after the event row committed, leaving a rejected decision with a readable event (orphan) — "rejected requests never create world state" violated in that interleaving. | **FAIL (major)** |
| C7 mint before ingress | request-id/artifact-id/revision-id minted client-side in helpers (text_kernel.clj:110-111,48-49; core.clj:134) ✓; no randomness in the topology ✓. **But** helpers do not mint omitted `proposed-event-id` (`action-request` only assocs if supplied, core.clj:150,165), so the kernel derives event identity at interpret time — deterministic (replay-stable, so C8's rationale survives) but minting is not "always on the construction side." | **FAIL (minor)** |
| C8 determinism laws | **Deterministic acceptance: FAIL** — `(now-ms)` inside both decision constructors (core.clj:318,330) and decision outcomes dependent on hop-races and retry timing (traces above). **Idempotency: FAIL** — no dedup guard; duplicate/colliding appends overwrite audit rows and events; retried records can double-decide differently. **Replay-invariance:** spec itself marks it stated-but-unverified (A10); additionally the wall-clock stamps guarantee a cold replay cannot reproduce decision rows byte-identically, so the stated requirement is structurally unreachable as written. | **FAIL (major)** |
| C9 interpret pattern | Type-dispatched interpret fns with `:default` ⇒ `:unknown-action-type` (416-499); unknown types reject, don't crash; envelope validation strictly precedes dispatch (computed at 414, tested at 417 before any type `case>`). Signature caveat: only `interpret-status-request` consumes durable state; ingest/compat interpreters are request-only (252-258,271-277) — within the pattern (state consumption is bounded above, not below). Purity is violated only via `now-ms` (charged to C8). | **PASS** (pattern present and ordered correctly) |
| C10 materialization pattern | Pure per-event-type fns (`artifact-`, `revision-`, `branch-`, `status-materialization`, `units-by-id-materialization`, 360-393,331-339), invoked only under `accepted-decision?` gates (432,463,483); derived rows carry provenance (`:root-event-id` 374,383; `:created-by-event/id` 337; status `:event/id` 393). | **PASS** |
| C11 depot-family taxonomy | Exactly one intent depot (396); no claim/observation/control depots — intent-only, structure earned (file header text_kernel.clj:17-22 documents intent-only rationale). | **PASS** |
| C12 back-arrow | Inside the module: no UI side channel; reads are PStates. **But util_fns (a named source of this layer) violates it:** UI reads are served from process-local atoms (`get-sidebar-state` 202, `get-agent-run` 198, `get-editor-doc` 301, etc.) while the kernel receives fire-and-forget compat appends whose decisions are never awaited or read back; if the kernel rejects, the atom still updates (e.g. `emit-sidebar-event!` 237-246 swaps after append unconditionally) — the atom, not Rama, is the source of truth for those reads, and nothing reconciles them. The file declares them "transitional local mirrors" (79), which documents the violation; it does not remove it. | **FAIL (major)** |
| C13 ack semantics | `:append-ack` hardcoded in the single append helper (531); outcomes readable by request id (read-decision) and domain id; `await-*` poll helpers provided (572-589). No read-after-write assumes processing-on-ack. | **PASS** |
| C14 cross-kernel wires | N/A — no outbound wires from the text kernel; no mirror depots declared. | **PASS (N/A)** |
| C15 executor option | N/A — no TaskGlobal, no executor; work is in-topology materialization. | **PASS (N/A)** |
| C16 naming/vocabulary | `projection-*` convention ✓ (`$$projection-cache`). **But** primary tables are named `$$requests-by-id`/`$$decisions-by-id`/`$$events-by-id`/`$$units-by-artifact`/`$$unit-status-by-branch` — the `-by-*` suffix that C16 reserves for *secondary indexes* is used on primaries, and primaries are not plural-noun tables per the locked convention (kernel.clj:471-475). No keyword collisions in this kernel. Vocabulary (text-kernel) matches the lock. | **FAIL (minor)** |
| C17 scaffolding declared | `$$policies` (404) and `$$projection-cache` (408): zero writers (no transform targets them in 410-499), zero readers in module code; both are named as the canonical documented scaffolds in kernel.clj (`:pstate-spec` note + `:projections :scaffold-only`, kernel.clj:733-744,759,768). Documented, not silent. | **PASS** |
| C18 shape maintenance | KERNEL-SHAPE's text-kernel entries verified against source: ingress partitioner `:routing/key` ✓ (396, kernel.clj:543); intent depot `*text-requests-depot` ✓ (kernel.clj:565); ack `:append-ack` ✓; claim/obs/control absent ✓ (kernel.clj:581-620 list text in none); no executor ✓; no cross-module wires ✓; interpret dispatch types `:artifact/ingest`, `:unit/status-set`, `:compat/record` ✓ (425,452,476); the five listed materialize fns all exist ✓ (kernel.clj:698-702); PState count 11 = 9 written + 2 scaffold ✓ (398-408); projections scaffold-only `[$$projection-cache]` ✓. One imprecision: the `:interpret-fn :signature` `(fn [request existing-state] …)` holds only for the status interpreter; ingest/compat take request alone. Descriptive doc, in sync on every checkable claim. | **PASS** (with the signature imprecision noted) |
| C19 projections preserve identity | Every projection row carries `:target/kind :unit`, `:target/id`, `:unit/id`, `:preview`, and `:provenance` with `:root-event-id` + `:created-by-event/id` (617-632); views derived at read time from truth tables, never read as truth by interpret; trivially "disposable" since they are never stored. | **PASS** |

---

## Self-consistency check (phase step 6)

Re-read complete. Every place this artifact records a gap, race, or unenforced invariant is marked FAIL on the corresponding check — no check above is passed while acknowledging a defect in its own body. The two "PASS (with note)" entries (C9 signature breadth, C18 signature imprecision) are documentation-precision observations about kernel.clj's descriptive text, not functional gaps in the module; neither describes a behavior defect, and C18's only obligation (claims in sync with code) was verified claim-by-claim.

## Failed checks summary

| Check | Severity driver |
|---|---|
| Redundant conditionals | identical 5-op tail duplicated in all 5 branches |
| Consecutive keypath | 4 sites |
| Non-subindexed collections without size limits | 3 unbounded inner maps ($$units-by-artifact, $$unit-status-by-branch, $$text-revisions) — schema restructuring |
| Stream topology idempotency | wall-clock decisions; retry can flip decisions and orphan events; no dedup/collision guards — events mutable in practice |
| Partial failure in stream topologies | 5 independently-committing batches per request; re-convergence races break per-key serialization, last-write-wins, and head monotonicity — architectural |
| Application-state caches survive restart | util_fns atoms have no rebuild path; entire runtime is an in-memory IPC |
| No reimplementation of built-ins | `select-pstate-one` vs `foreign-select-one` |
| Spec Op A1/A3/A5/A6, Entity 2/3, C3, C4, C5, C6, C7, C8, C12, C16, C1(dup) | as traced above; the load-bearing majors are C4 (atomicity/serialization), C6/C8 (decision stability, determinism, idempotency), Entity 2/3 (event immutability, judgment re-attach), Op A5 (poison records), C12 (atom mirrors as UI truth) |

## Verdict

**major-fail** — multiple failures require restructuring rather than line edits: per-key atomicity and serialization demand removing/redesigning the five-partitioner-hop architecture (or moving to microbatch with colocated writes, as the plan specified); event immutability and idempotency require dedup/collision guards and removing wall-clock stamps from interpretation; the judgment re-attach defect requires revision-scoped unit identity (id-scheme + status-keying change); three PState schemas need subindexing; and the no-silent-drop contract needs a surrogate-key path for malformed appends. Several minor failures (ordering-key mismatch, naming, duplication, `foreign-select-one`, consecutive keypaths) ride along but do not drive the verdict.

Retro note: recorded as the retrospective result per track rules — no fix loop is triggered by this artifact.
