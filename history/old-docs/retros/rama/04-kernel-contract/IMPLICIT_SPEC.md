# Implicit Spec — Kernel Contract Layer (Retro Phase 0)

<!-- Phase 0, RETROSPECTIVE mode. Subject: the kernel contract layer — the shared
     request/event contract (core.clj), the V0/V1 text-artifact module (text_kernel.clj),
     and shared utilities. This artifact captures REQUIREMENTS, not design. PState names,
     depot partitioning details, and topology types appearing in the inputs are treated as
     design; the motivating requirement is extracted instead. Exception: where KERNEL-SHAPE
     itself mandates a structural convention as the contract (e.g. "every kernel has an
     intent depot"), that mandate IS a requirement of the contract layer and is recorded
     as such. -->

**Sources for this spec (the "user-facing spec" for this track):**
- `src/app/server/rama/kernel.clj` — prose spec + `KERNEL-SHAPE` data form (primary spec source; the one src file read)
- `docs/current-mental-model/architecture/rama-world-kernel-v0-pr-trail.md` — V0 contract conventions
- `docs/current-mental-model/architecture/rama-world-kernel-text-instance.md` — pointer only (see Ambiguity A1)
- `test/app/server/rama/text_kernel_test.clj` — the public contract as exercised

**Scope is two layers:**
- **(a)** The text kernel as a module: V0/V1 text-artifact operations, lifecycle, reads, edge cases.
- **(b)** The cross-kernel contract this layer imposes on all five kernel instances (text, space, compute, transcript, llm) — requirements ON modules, stated so a later phase can validate instances against them.

**The center loop the contract serves** (from the V0 trail):

```
ActionRequest → intent ingress → interpret (accept/reject) → ActionDecision
  → (if accepted) KernelEvent(s) → materializations → projections → UI reads
```

Mental rule (verbatim from the trail, load-bearing):

```
ActionRequest asks.
KernelEvent happened.
ActionDecision records the answer.
```

---

## Operations

### Layer (a): Text-kernel operations

#### Op A1 — Ingest text artifact (request type `:artifact/ingest` → event type `:artifact/ingested`)

Creates a text artifact (or a new revision of one — see Ambiguity A3) from raw text content. The caller supplies or lets the helper mint: `request-id`, `artifact-id`, `revision-id`, `proposed-event-id`, `time-ms`.

- **Latency**: Human-interactive write (user or adapter ingests a document). Durable-append acknowledgment in tens of ms; decision + materialized state visible within hundreds of ms is acceptable. The contract is explicitly **asynchronous**: tests poll with `await-decision` / `await-materialized` rather than expecting synchronous read-after-write. No operation may require single-digit-ms decision visibility.
- **Throughput**: Driven by explicit user actions and adapter compatibility appends (the adapter layer "appends compatibility ActionRequests instead of prebuilt kernel events"). Low rate (interactive + agent-driven), but payloads can be whole documents — per-request payload size scales with document length, and each accepted ingest fans out into one unit per line.
- **Consistency/correctness invariants**:
  - The request must be durably recorded *before* any accept/reject decision exists ("the depot receives ActionRequests, not pre-decided KernelEvents" — the central V0 correction).
  - An accepted ingest produces exactly one `:artifact/ingested` KernelEvent whose `:event/id` equals the request's `:proposed/event-id`.
  - The accepted event must record the artifact's full provenance: the artifact's `:root-event-id` must equal the ingest event id, and every derived unit must trace back to it.
  - The event envelope is **carrier-independent**: target is expressed as `{:target/kind :artifact, :target/id <id>, :target/address nil}` and the payload carries `:artifact/type :text` — text is one carrier instance inside a general kernel, not a special case.
  - The event's ordering key must equal the request's routing key (`[:ordering :key]` = `:routing/key`).
  - After acceptance, reading the artifact's text head returns exactly the ingested content, byte-for-byte (`"keep\nreject\nalso keep"` round-trips).
  - Revisions are immutable once accepted; "update" means a new revision, never mutation of a stored revision (V0 includes "text revision materialization"; the request carries an explicit `revision-id`).
- **Data growth and scale**: Artifacts, revisions, and their derived units grow without bound and are never deleted (audit + provenance reads can arrive at any time). Dominant access patterns: point lookup of artifact/head by artifact id; enumeration of all units of one artifact (needs efficient per-artifact grouping, not N point reads); full-content read of the head revision.
- **Concurrency behavior**: Two concurrent ingests with the same routing key must be serialized — decisions observe all previously accepted facts for that key. Concurrent ingests for *different* artifacts have no mutual ordering guarantee. Two concurrent ingests of the *same* artifact id: both requests and both decisions must be durable; whether the second is accepted-as-new-revision or rejected is unspecified (Ambiguity A3) — but under no interleaving may the artifact head point at a revision whose content was never accepted.
- **Edge cases**: empty string content (zero lines? one empty line? — Ambiguity A5); content with trailing newline; very large documents (per-line unit fan-out); caller omits optional ids (helper must mint them **before** append — see contract C7); caller omits `time-ms` (must still produce a valid, decidable request — exercised by tests); duplicate `request-id` re-append (Ambiguity A4); `proposed-event-id` colliding with an existing event id (Ambiguity A4).

#### Op A2 — Derive line units from an accepted ingest ("unitization")

Splits an accepted text artifact event into per-line units, each independently addressable and judgeable.

- **Latency**: Follows ingest in the interactive flow; sub-second materialization acceptable. Tests await unit visibility explicitly.
- **Throughput**: One unit per line of the ingested content; an N-line document produces N units in one logical operation.
- **Consistency/correctness invariants**:
  - Exactly one unit per line, in document order; each unit's `:unit/preview` is the line's text.
  - **Provenance is mandatory**: every unit carries `[:provenance :root-event-id]` = the ingest event id. A projected unit must be traceable to the accepted ingest event/request (explicit V0 inclusion: "provenance from projected unit to accepted ingest event/request").
  - Every unit carries an anchor of type `:text/range` — units address a *range of the revision*, not a copy detached from it.
  - Unit ids are deterministic per artifact + line (test relies on stable ids like `<artifact>/line/<n>` to target a specific unit later; the contract requirement is *stable, re-derivable identity*, not the specific format).
  - Unitization derives from the **accepted event**, never from a raw request — only world facts can be unitized.
- **Data growth and scale**: Units per artifact bounded by line count; total units unbounded across artifacts. Dominant read: all units for one artifact (range/group access), plus point lookup of a single unit during status judgment.
- **Concurrency behavior**: Re-running unitization for the same accepted event must be idempotent in effect (same units, same ids — the kernel theory names idempotency as a law; see C12). Unitization racing a status-set on a not-yet-materialized unit resolves via the status-set's own validation (reject `:target-unit-not-found` if the unit isn't visible yet on that key's serialized timeline).
- **Edge cases**: zero-line content (no units — or rejected upstream, Ambiguity A5); duplicate identical lines (must still get distinct units/ids by position); unitizing an event id that doesn't exist (must fail without creating state); whether unitization flows through the intent ingress as its own request or is an internal derivation of ingest is not pinned by the readable inputs (Ambiguity A6).

#### Op A3 — Set unit status (request type `:unit/status-set` → event type `:unit/status-set`)

Records a judgment (e.g. `:rejected`) on one unit, scoped to a branch. This is the "user clicks reject on line 2" operation from the trail's mid-state example.

- **Latency**: Human-interactive; the click's effect (line moving from canonical to discarded view) should be visible in well under a second. Asynchronous decision is acceptable (tests await).
- **Throughput**: One request per judgment click / agent judgment; low rate, but accumulates one status row per (branch × unit) judged.
- **Consistency/correctness invariants**:
  - The decision must be derived from durable materialized state: the interpreter reads the unit's existence (and, per the trail, policy/capability state and status-transition validity) before accepting. A status-set targeting a missing unit is **rejected** with reason `:target-unit-not-found`, durably, with no event created.
  - Status is **branch-scoped**: the stored status is read per branch (`read-unit-statuses runtime core/default-branch-id`); the same unit may carry different statuses on different branches. V0 only requires the default branch to work (branch fork/merge is explicitly out of V0 scope).
  - An accepted status-set produces a `:unit/status-set` KernelEvent with `:event/id` = the request's `:proposed/event-id`, and the stored status map entry records the status (e.g. `{:status :rejected}`).
  - The status request carries the same routing key as the artifact's other operations (`[:artifact <artifact-id>]` in V1) so judgment is serialized with the artifact's own history.
  - A free-text `:reason` may accompany the judgment and must be preserved (auditable judgment, not just a flag).
- **Data growth and scale**: One entry per (branch × unit) with a recorded status; unbounded over time; dominant read is "all statuses on branch B" (map read) plus membership check for a specific unit.
- **Concurrency behavior**: Two status-sets racing on the same unit serialize per routing key; the later accepted one wins the stored status; **both** requests, both decisions, and both events remain durable (the audit trail never collapses). A status-set racing the ingest/unitization of its target resolves deterministically on the serialized per-key timeline.
- **Edge cases**: status value outside the known domain (`:rejected` is tested; the full status vocabulary and legal transitions are unspecified — Ambiguity A7); status-set on a unit of a different artifact than `:artifact/id` claims (payload/target consistency — should fail validation; unverified); re-judging an already-judged unit (allowed — last accepted wins — or transition-checked? trail says "check status transition", domain unspecified, Ambiguity A7); judgment on a non-default branch (out of V0 scope but must not corrupt default-branch state).

#### Op A4 — Compatibility record (request type `:compat/record`)

KERNEL-SHAPE lists `:compat/record` as a third dispatched request type in the text kernel, and the trail records that the adapter layer (`util_fns.cljc`) "now appends compatibility ActionRequests instead of prebuilt kernel events."

- **Latency / Throughput**: Same envelope and asynchronous-decision expectations as all other requests; volume driven by legacy/adapter call sites.
- **Consistency/correctness invariants**: Must obey the full request envelope contract (Layer b) — the entire point is that legacy paths enter through the front door (request → decision → event) instead of injecting pre-decided events. No adapter may bypass interpretation.
- **Edge cases / status**: The operation's payload contract and what it materializes are not described in the readable inputs (Ambiguity A8). What IS a requirement: it exists, it is dispatched like any other request type, and it must not smuggle event identity (C5 applies).

#### Op A5 — Append a raw ActionRequest (generic ingress)

The module exposes a raw append path (`append-action-request!`) taking a fully-formed envelope of *any* request type — including unknown ones. This is the ingress every typed helper ultimately uses.

- **Latency**: Acknowledgment confirms **durable append only**, not processing (KERNEL-SHAPE: `:ack-level :append-ack`). Callers needing the outcome must poll/await the decision.
- **Throughput**: Sum of all typed operations plus adapter traffic.
- **Consistency/correctness invariants**:
  - **Verbatim durability**: the stored request must equal the appended request value exactly — even when invalid (tests assert `(= request (read-request ...))` for rejected/malformed requests). The kernel stores what was asked, not a normalized version.
  - Every appended request eventually receives exactly one durable decision, readable by request id — including malformed and unknown-type requests. There is no "silently dropped" outcome.
  - Common envelope validation runs **before** action-type dispatch: a malformed request of an unknown type is rejected with `:decision/reason :request-invalid` (carrying the specific validation error types), NOT `:unknown-action-type`. Well-formed-but-unknown types get their own rejection reason (`:unknown-action-type` is the implied reason; only its non-use for malformed requests is directly tested).
- **Concurrency behavior**: Appends are the serialization point; per-routing-key ordering is established at ingress.
- **Edge cases**: unknown request type (well-formed → rejected, decidable); malformed unknown type (rejected `:request-invalid` first); nil/missing routing key (validation error `:routing/key-invalid`, see C4); payload smuggling `:event/id` (rejected, see C5).

#### Op A6 — Read operations

Three families, all callable **at any time and in any entity state** (users and tests query in all states):

1. **Audit reads** — request by request-id, decision by request-id, event by event-id.
   - Must return the verbatim stored request; the decision with status/reason/errors/routing-key; the event or `nil` if no such event exists. `read-event` of a never-created id returns `nil` (not an error).
2. **Truth reads** — artifact by id (carries `:root-event-id`), text head by artifact id (carries `:text/content` = current full content), units by artifact id, unit statuses by branch id (map of unit-id → status row).
3. **Projection reads** — canonical view (branch, artifact) → ordered rows of non-rejected units; discarded view (branch, artifact) → ordered rows of rejected/hidden units.
   - Canonical/discarded must **partition** the artifact's units per branch: a unit appears in exactly one of the two views, document order preserved.
   - Projection rows are not bare strings: each row carries `:preview`, a `:target` (`:target/kind :unit`), and `:provenance` with `:root-event-id` — projections must preserve identity and provenance, not just display text (this is the "lawful compression" requirement: identity, provenance, relations survive projection).

- **Latency**: These serve the UI (Rama → e/watch → UI per the kernel diagram); sub-100ms desirable, sub-second acceptable. Reads must never block on in-flight writes.
- **Throughput**: Read-dominated relative to writes; one UI subscription re-read per relevant state change.
- **Consistency**: Eventually consistent with appends; monotonic per key (once a decision/materialization is visible, it does not un-happen). The module must provide an awaiting/polling affordance (`await-decision`, `await-materialized`) so callers can bridge append-ack to processed-state without sleeps.
- **Edge cases**: every read with a nonexistent id returns nil/empty (never throws); reads between "request stored" and "decision made" must show the request with no decision yet; reads between "decision made" and "materialization complete" must never show a half-materialized artifact (per-key event-boundary atomicity, C6).

#### Op A7 — Runtime lifecycle (start / close)

- **Invariants**: `start-text-runtime!` yields a runtime handle through which ALL appends and reads flow; `close-text-runtime!` must release all resources (tests run start/close repeatedly in one JVM — repeated cycles must not leak or collide). State durability across restart is the substrate's job; the contract requirement is that closing is safe at any point after pending awaits complete, and that a fresh runtime in tests starts from an empty world.
- **Edge cases**: close with in-flight requests (must not corrupt durable state); double close; reads after close (error or nil, but not corruption).

---

### Layer (b): Cross-kernel contract requirements

These are requirements the contract layer imposes on **every** kernel instance (text, space, compute, transcript, llm). A later phase validates instances against them. "MUST" items are the contract; items marked *(structural mandate)* are where KERNEL-SHAPE itself fixes a structural convention as the contract.

**C1 — Three-envelope taxonomy.** Every kernel speaks exactly three envelopes: `ActionRequest` (the raw proposal entering the system), `ActionDecision` (the durable record of accept/reject), `KernelEvent` (an accepted world fact). They are disjoint shapes: a valid request is not a valid event and vice versa (`valid-request?` / `valid-event?` are mutually exclusive on the same map). The shared core owns the constructors and validators (`action-request`, `valid-request?`, `valid-event?`, `request-validation-errors`, `default-branch-id`) so instances cannot drift on envelope shape.

**C2 — Requests before decisions.** The system ingests `ActionRequest`s, never pre-decided `KernelEvent`s. Acceptance/rejection is decided *inside* the kernel, after durable ingress, from the request plus durable state. No client, helper, or adapter may construct world facts directly. (This is the V0 correction the whole layer is built on.)

**C3 — Required request envelope.** An ActionRequest carries: request identity (`:request/id`), `:request/type`, routing key (`:routing/key`), proposed event identity (`:proposed/event-id`), actor, branch, context, target (`:target/kind`, `:target/id`, `:target/address`), action (`:action/type`, `:action/capability`, `:action/params`), payload, causality, provenance. Validation enforced by the shared core, with typed errors:
  - `:request/action-type-drift` — `:request/type` must equal `[:action :action/type]`; the envelope's claim and the action's claim may not diverge.
  - `:request/payload-event-id` — the payload must NOT contain `:event/id` (see C5).
  - `:routing/key-invalid` — the routing key must be present and well-formed (nil is invalid).
  - Common validation runs before per-type dispatch (see Op A5); rejection reason for envelope failures is `:request-invalid` with the full error list preserved on the decision.

**C4 — Routing-key discipline.** Every request carries an explicit routing key; the decision for that request carries the **same** routing key; the resulting event's ordering key equals it. All state changes caused by one request must be visible atomically with respect to that key, and all of one key's history must be mutually ordered. Requirements behind the design: (i) the routing key is the unit of serialization and of event-boundary atomicity; (ii) cross-key effects within one kernel require an explicit re-partitioning step and do NOT get single-event atomicity *(structural mandate — KERNEL-SHAPE `:ingress-partitioner`, required in all 5)*. The text/space kernels' `:routing/key` field is declared transitional (`routing-key-contract` exposes `:transitional? true`) — the *discipline* is the contract; the field name/shape may migrate (Ambiguity A9).

**C5 — Identity-header obligations.** Event identity is proposed by the requester in the header (`:proposed/event-id`), never inside the payload, and never as a top-level `:event/id` on the request. A request smuggling `:event/id` through the payload MUST be rejected (`:request/payload-event-id`), and the smuggled id must never become a readable event. On acceptance, the kernel mints the event with `:event/id` = the request's `:proposed/event-id` — so the event id is auditable back to the request that proposed it. On rejection, the decision's `:event/id` is nil and the proposed id is never used.

**C6 — Decision contract.** Every ingested request produces exactly one durable, readable decision:
  - `:decision/status` ∈ {`:accepted`, `:rejected`}.
  - Same `:routing/key` as the request (C4).
  - Accepted: the decision carries the accepted event (its `:event/id` readable from the decision); the event is independently readable by id; materializations follow.
  - Rejected: `:decision/reason` (a specific, machine-readable cause — `:request-invalid`, `:target-unit-not-found`, `:unknown-action-type`, ...), structured `:errors` (a list of `{:type ...}` maps) for validation failures, nil `:event/id`.
  - **Rejected requests never create world state**: no KernelEvent, no materialization, no projection change — but the request AND the decision remain durable forever (the audit trail is itself world state). "Rejected request stays durable and does not produce KernelEvent" is a per-kernel test obligation.

**C7 — Identity is minted before ingress.** All ids in a request (`request-id`, domain ids, `proposed-event-id`) are minted by the caller/helper **before** the durable append. Kernels must not generate fresh identity during interpretation — otherwise replay/retry of the same ingested request could produce different ids, violating C8. Helpers may mint defaults for omitted ids, but minting always happens on the construction side of the boundary.

**C8 — Determinism laws.** The kernel theory names three laws every instance must satisfy: **deterministic acceptance** (the decision is a pure function of the request plus durable state at its position in the key's serialized history — no wall clock, no randomness, no out-of-band reads inside interpretation), **idempotency** (re-processing an already-processed input must not double-apply effects), and **replay-invariance** (re-folding the ingested history rebuilds the same state). Status caveat the spec itself declares: replay-invariance is the *intended* property; cold-replay/property-test proof is planned, not currently verified (Ambiguity A10). Phase-validation should treat deterministic acceptance and idempotency as hard requirements and replay-invariance as a stated-but-unverified requirement.

**C9 — Interpret pattern** *(structural mandate)*. Every kernel has an interpret step with the shape `(fn [request existing-state] => decision-or-events)` — the accept/reject machinery dispatching on request type. Requirements: it consumes only the request and durable state (C8); it produces a decision (with events if accepted, reason+errors if rejected); unknown types produce rejections, not crashes; envelope validation precedes type dispatch (C3).

**C10 — Materialization pattern** *(structural mandate)*. Every kernel has per-event-type **pure** materialization steps that fold accepted events into durable state. Only accepted events materialize. Materializations must preserve provenance links (event id → derived rows) so any projected row can be traced to the accepting event and its request.

**C11 — Depot-family taxonomy** *(structural mandate — this is the shape itself)*. Every kernel has exactly one **intent** ingress ("do this" requests) — 5 of 5, required. The other families are optional roles with fixed semantics when present:
  - **claim** (executor lock: "I'm taking this work") — optional; MUST co-occur with **observation**. The pair is the back-arrow rule made structural.
  - **observation** (the back-arrow: workers stream what HAPPENED back into the kernel) — optional; co-occurs with claim. Requirement behind the mandated source option: a retried fold over observations must resume mid-stream without re-executing the external side effect that produced them (a retry must never re-spawn a process / re-run an LLM call).
  - **control** (out-of-band cancel / steer / approve / compact: what someone WANTS to happen) — optional. Controls are NOT observations: different fold semantics, different ack semantics, different retry expectations; they get their own type-dispatching reducer.
  - Intent-only kernels are legitimate (text and space are intent-only); instances must not grow claim/obs/control roles they don't need (free-construction discipline: structure must be earned).

**C12 — Back-arrow rule.** Workers and agents report results by streaming observations back INTO the kernel; the kernel is truth; the UI reads kernel state. Workers/agents never stream directly to the UI as a source of truth, and external execution state never bypasses the decision/event pipeline.

**C13 — Ack semantics.** Ingress acknowledgment = durable append only (`:append-ack`), not processing completion. Every kernel must therefore make outcomes *readable* (decision by request id, state by domain id) and callers must await/poll rather than assume synchronous effects. Cross-kernel writes likewise use explicit, acknowledged appends into the target kernel's ingress.

**C14 — Cross-kernel wires.** When one kernel drives another (only space→llm today), the requirements are: (i) the writing kernel appends into the target's *intent/control ingress* — it never writes the target's state directly; (ii) the append is partition-aligned to the target's routing key and explicitly acknowledged; (iii) reads of the other kernel's state go through that kernel's declared read surface. The "natural transformation" framing is intuition; the operational contract (partitioned, acked append + declared read) is what gets validated.

**C15 — Executor option.** A kernel whose work executes inside the kernel process may own a reactive executor that polls pending work and spawns it with a spawn-if-absent registry (only compute today). Requirements: execution is triggered by durable *committed* intent state (never directly inside request processing), and duplicate spawn under retry is structurally prevented. Kernels whose execution lives outside the process (llm) instead route results back via observation ingress (C11/C12). The executor is an option, not a default.

**C16 — Naming and vocabulary conventions** *(structural mandate)*. Plural names for primary state tables; `-by-*` for secondary indexes; `projection-*` for view tables. Locked vocabulary: *kernel* = the shape (no bare "kernel" module exists); *text-kernel* = V0/V1 text-artifact + units model; *space* / *turn* / *llm-run* / *transcript* as defined in kernel.clj. Keyword collisions across semantic contexts are permitted ONLY when disambiguation is structural by field (`{:request/type :turn/cancel}` vs `{:control/type :turn/cancel}`), documented at both dispatch sites, and tested on both sides.

**C17 — Scaffolding must be declared.** State declared but not yet written (future contracts) is acceptable only if documented as scaffold; silent unused declarations are a contract violation. (Text kernel's two scaffold tables are the canonical, documented example.)

**C18 — Shape maintenance obligation.** KERNEL-SHAPE is descriptive and must stay in sync: building kernel #N or changing an existing kernel requires updating KERNEL-SHAPE's `:examples`/fields in the same change. New kernels are built by copying the closest existing instance, not by a generator (the generator is deliberately deferred until ≥3 new kernels stabilize the shape).

**C19 — Projections preserve identity.** Projections (pre-rolled views for UI subscription) are optional per kernel, but when present every projected row must preserve target identity and provenance back to the accepting event (see Op A6.3). Projections are derived, disposable state — never the source of truth.

---

## Entity State × Write Matrix

Read operations referenced below (all callable in every state):
`read-request` (by request-id), `read-decision` (by request-id), `read-event` (by event-id), `read-artifact` (by artifact-id), `read-text-head` (by artifact-id), `read-units` (by artifact-id), `read-unit-statuses` (by branch-id), `read-canonical-view` (branch, artifact), `read-discarded-view` (branch, artifact).

### Entity 1: ActionRequest / decision lifecycle (keyed by request-id)

States: **(s0) never appended**, **(s1) appended, undecided**, **(s2) decided-accepted**, **(s3) decided-rejected**.

**s0 × append valid request (any type)** → s1 then s2/s3
- read-request: the verbatim appended map, once durable — exact value equality with what was appended, no normalization.
- read-decision: nil while undecided (s1); then exactly one decision with matching `:routing/key`.
- read-event (proposed id): nil until and unless accepted; equals the accepted event after s2.
- all truth/projection reads: unchanged until the decision accepts and materialization completes; must never show partial effects of an undecided request.

**s0 × append invalid request (e.g. payload `:event/id`, nil routing key, action-type drift)** → s1 then s3
- read-request: verbatim stored, including the invalid fields — the audit trail records exactly what was asked.
- read-decision: `:rejected`, `:decision/reason :request-invalid`, `:errors` containing every applicable typed error (not just the first), `:event/id` nil, routing key = request's routing key (when the request *had* one; behavior of decision routing for a nil routing key must still produce a readable decision — the test asserts decision routing-key equality even for the smuggling case).
- read-event (smuggled or proposed id): nil — forever. The smuggled id never becomes an event.
- read-artifact / read-text-head / read-units / read-unit-statuses / read-canonical-view / read-discarded-view: completely unchanged. Rejected requests never create state.

**s0 × append well-formed unknown-type request** → s1 then s3
- read-request: verbatim stored.
- read-decision: `:rejected` with the unknown-type reason (`:unknown-action-type` implied), nil `:event/id`.
- read-event: nil. All truth/projection reads: unchanged.

**s0 × append malformed unknown-type request** → s1 then s3
- read-decision: `:rejected`, reason `:request-invalid` (envelope validation wins over unknown-type dispatch — order is observable and contractual), errors include the envelope error types.
- read-request: verbatim. read-event: nil. Truth/projection reads: unchanged.

**s1/s2/s3 × re-append same request-id** (duplicate)
- read-request: must remain a single coherent record — either the original verbatim (first-write-wins) or last-write-wins, but never a merge; **which** is unspecified (Ambiguity A4).
- read-decision: must remain exactly one decision per request-id; a duplicate must not double-apply effects (idempotency law C8) — re-acceptance creating a second event with the same proposed id would violate event-id uniqueness.
- read-event / truth / projection reads: no second materialization.

**s2/s3 × any later unrelated write**
- read-request / read-decision / read-event for this id: immutable forever (monotonic audit — once visible, never un-happens, never edited).

### Entity 2: KernelEvent (keyed by event-id)

States: **(e0) does not exist**, **(e1) exists (accepted fact)**.

**e0 × request accepted with this proposed id** → e1
- read-event: the event; `:event/type` matches the operation; target/ordering/payload per C1/C4/C5; envelope passes `valid-event?` and fails `valid-request?`.
- read-decision (of the accepting request): carries/points to this event id.
- truth reads: reflect this event's materialization (artifact row carries `:root-event-id` = this id for ingest; status map updated for status-set).
- projection reads: reflect the new fact (new canonical rows after ingest+unitize; row migrates canonical→discarded after a rejected judgment).

**e0 × request rejected naming this proposed id**
- read-event: nil, permanently (unless some *later, different, accepted* request legitimately proposes the same id — see Ambiguity A4 on collision).
- All other reads: unchanged.

**e1 × any write** — events are immutable; no write may modify or delete an existing event.
- read-event: identical forever. Replay/audit depends on this (C8).

### Entity 3: Text artifact + head revision (keyed by artifact-id)

States: **(a0) does not exist**, **(a1) exists with head revision R, units not yet derived**, **(a2) exists with units derived**, **(a3) exists with units derived and ≥1 unit judged**.

**a0 × accepted ingest** → a1
- read-artifact: row exists; `:root-event-id` = ingest event id.
- read-text-head: `:text/content` = exact ingested text.
- read-units: empty (units not yet derived) — readers must tolerate an artifact whose units are not yet visible.
- read-unit-statuses: unchanged (no entries for this artifact's units).
- read-canonical-view / read-discarded-view: empty or unit-free for this artifact — projections derive from units, so pre-unitization views must be empty, not erroring.
- read-request/read-decision/read-event: full audit chain readable (request → accepted decision → event).

**a0 × rejected ingest** — stays a0
- read-artifact: nil. read-text-head: nil. read-units: empty. canonical/discarded: empty. Audit reads: request + rejected decision readable; event nil.

**a1 × unitize lines** → a2
- read-units: exactly one unit per line, document order, previews = line text, each with `:text/range` anchor and `:provenance :root-event-id` = ingest event id.
- read-artifact / read-text-head: unchanged (unitization adds units; it does not touch content).
- read-unit-statuses: still no entries (units exist with *no* status until judged).
- read-canonical-view: all units (unjudged units are canonical by default — "canonical = non-rejected").
- read-discarded-view: empty.

**a1 or a2 × second accepted ingest, same artifact-id** (revision semantics — Ambiguity A3; requirements that hold regardless of resolution)
- read-text-head: must be exactly one of {old content, new content} — never a blend; if accepted as a new revision, head moves to the new revision atomically per routing key.
- read-artifact: still one artifact row; `:root-event-id` lineage must remain traceable.
- read-units: must not present units of two different revisions as one undifferentiated set; prior judgments must not silently re-attach to different text (anchors are revision-addressed — `:text/range` against a specific revision).
- audit reads: both requests, both decisions, distinct events all readable.

**a2 × accepted unit-status-set (unit u ← :rejected)** → a3
- read-unit-statuses(default branch): now contains u → `{:status :rejected}` (plus preserved reason).
- read-canonical-view: u's row removed; all other rows present, order preserved.
- read-discarded-view: u's row present, with target kind `:unit` and provenance intact.
- read-units: unchanged — the unit still *exists*; only its branch-status changed (existence and judgment are separate).
- read-artifact / read-text-head: unchanged — judging lines never edits text.
- audit reads: status request, accepted decision, `:unit/status-set` event readable.

**a2/a3 × rejected unit-status-set (missing unit id)** — state unchanged
- read-decision: `:rejected`, `:decision/reason :target-unit-not-found`, nil event id.
- read-unit-statuses: no entry added for the missing id.
- read-canonical-view / read-discarded-view / read-units / read-artifact / read-text-head: byte-identical to before.
- read-event (proposed id): nil.

**a3 × accepted unit-status-set on an already-judged unit** (re-judgment — transition rules Ambiguity A7)
- read-unit-statuses: exactly one current status per (branch × unit) — the resolved status; no duplicate entries.
- read-canonical-view / read-discarded-view: the unit appears in exactly one view, matching the current status.
- audit reads: every judgment request/decision/event in the unit's history remains individually readable (current-state reads collapse; audit reads never collapse).

### Entity 4: Unit (keyed by unit-id, within artifact; status scoped by branch)

States: **(u0) does not exist**, **(u1) exists, unjudged on branch B**, **(u2) exists, judged on branch B**.

**u0 × unit-status-set** — rejected (`:target-unit-not-found`); u stays u0; all reads unchanged (covered above).

**u0 × unitize (of parent artifact's accepted ingest)** → u1
- read-units(artifact): includes u with stable id, preview, anchor, provenance.
- read-unit-statuses(B): no entry for u.
- read-canonical-view(B, artifact): includes u (unjudged ⇒ canonical).
- read-discarded-view(B, artifact): excludes u.

**u1 × accepted status-set (:rejected) on branch B** → u2
- (per a2 × status-set row above; additionally:) read-unit-statuses on a *different* branch B′: unchanged — judgments are branch-local; canonical/discarded on B′ still show u as canonical.

**u2 × any write to a different unit** — u's status, view membership, provenance unaffected (no cross-unit interference).

### Entity 5: Branch (keyed by branch-id)

States: **(b0) default branch only (V0)**, **(b1) forked branches — explicitly out of V0 scope**.

- All branch-scoped writes/reads in V0 target `core/default-branch-id`; the contract requires branch identity to be explicit in requests and status reads *now* so that V1+ forking does not change envelope shape (the compatibility promise: splitting/extending changes routing/topology, "not the meaning of ActionRequest or KernelEvent").
- b0 × status-set naming a nonexistent branch: unspecified (accept-and-create vs reject) — Ambiguity A11; whichever way, default-branch state must be unaffected.

### Entity 6 (genesis): Empty world

State: **(w0) cold start — no artifacts, no branches materialized, no user policies; only a trusted bootstrap/system actor exists in code/config.**

- **w0 × ordinary user request**: must be decidable from empty durable state — for the text kernel, an ingest is acceptable from empty state (tested); operations that *require* existing state (status-set) reject cleanly (`:target-unit-not-found`).
- **w0 × bootstrap request**: the trail defines a genesis rule — only the trusted bootstrap/system actor may create first world facts where policy would otherwise gate them. There is exactly one unavoidable trusted root (genesis); after genesis, ordinary actions are decided from durable state only. V0 ships without persisted policy tables, so capability checks beyond the envelope are not yet enforced (the `:action/capability` field is mandatory in the envelope NOW so policy can be enforced later without envelope change).
- read operations at w0: all return nil/empty without error.

---

## Ambiguities and Open Questions (flagged for later phases)

- **A1 — Text-instance doc is a pointer.** `architecture/rama-world-kernel-text-instance.md` only redirects to `10-anchors/rama-world-kernel-text-instance.md`, which is excluded from this track's readable inputs. This spec therefore derives the text-instance contract from kernel.clj + the V0 trail + the tests only. If the anchors doc contains additional contract content, a follow-up pass should reconcile.
- **A2 — Read surface vs. depot/PState design.** All reads above are stated as *capabilities with consistency requirements*; their storage layout (which tables, which partitions) is design, out of Phase-0 scope.
- **A3 — Re-ingest / update-versioning semantics.** Requests carry explicit `revision-id` and V0 includes "text revision materialization," implying ingest-as-new-revision; but no input exercises a second ingest of an existing artifact. Accept-as-revision vs reject is unresolved; invariants that hold either way are pinned in the matrix (Entity 3).
- **A4 — Duplicate / colliding identity.** Duplicate `request-id` re-append, and `proposed-event-id` collision with an existing event, are not exercised. The idempotency law (C8) demands no double-apply; first-write-wins vs last-write-wins for the audit record, and reject-vs-ignore for event-id collision, are open.
- **A5 — Empty-content ingest.** Zero-line / empty-string artifacts: accepted with zero units, or rejected by validation? Untested.
- **A6 — Unitization's ingress path.** Whether line-unit derivation flows through the intent ingress as its own request type or is an internal derivation step of accepted ingest is not determinable from the readable inputs; the tests drive it via a separate runtime call (`unitize-lines!`).
- **A7 — Status vocabulary and transitions.** Only `:rejected` is exercised. The full status domain (accepted? hidden? reset-to-unjudged?) and legal transition rules ("check status transition" in the trail) are unspecified.
- **A8 — `:compat/record` payload contract.** Named in KERNEL-SHAPE as a dispatched type; its payload and materialization are otherwise undocumented in the readable inputs.
- **A9 — Transitional routing key.** `routing-key-contract` is explicitly `:transitional? true`; KERNEL-SHAPE poses the open question of migrating text/space to domain-specific keys. The routing *discipline* (C4) is the contract; the key's field shape may change.
- **A10 — Replay-invariance is aspirational.** kernel.clj itself states cold-replay / property tests are planned, not written. Phase validation must treat it as a stated requirement without an existing oracle (property tests for replay-determinism, retry, and partitioner consistency are the named missing oracles).
- **A11 — Non-default branch behavior in V0.** Branch-scoped requests for branches that don't exist: accept-and-create vs reject is unspecified; only default-branch behavior is exercised.
- **A12 — Accepted-decision metadata.** Whether accepted decisions carry `:decision/reason` / policy echo (the KernelEvent envelope lists a `policy` field) is unasserted; only the rejected-side reason/errors contract is pinned by tests.
