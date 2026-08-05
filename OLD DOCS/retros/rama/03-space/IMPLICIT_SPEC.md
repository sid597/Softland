# Implicit Spec — Space Kernel

<!-- Phase 0 (RETROSPECTIVE). Produced after the fact for a module built before this skill existed.
     Requirements analysis only. Storage design (state-store names/schemas, inbox partitioning,
     topology types, cross-module wiring mechanics) is deliberately excluded — that is Phase 1+.
     Evidence base: the three-depot current-system note, the world-kernel V0 request/decision/event
     contract trail, the module identity header, and the full public test contract
     (test/app/server/rama/dogfood_space_test.clj). Where the named PState `$$space-graph` appears
     in the identity header, it is translated to its requirement: spaces form a graph related by
     fork lineage. -->

## System summary (domain framing)

The Space Kernel is the local-world kernel. It manages a graph of inhabitable **spaces** — currently chat-thread work areas — each holding its own **turns**, **objects**, **slices**, **overlays**, **derivatives**, **patch proposals**, and **projections**. Conversation and intent become inhabitable local worlds.

It follows the world-kernel request/decision/event contract:

```
ActionRequest (asks) → durable request → kernel decides
  → rejected: durable ActionDecision, no facts
  → accepted: durable ActionDecision + KernelEvents (happened) → canonical state → projections
```

**Boundary rule:** Space manages chat as place; the LLM Kernel executes model runs inside that place. Space owns the turns and freezes the context bundles; it **never runs the model itself**. When a space request needs external model work (compose-and-send, fork, controls), Space derives a dispatch/control record that crosses into the LLM Kernel. Many space requests are purely local and must produce **no** LLM-side effect at all.

**Actors:** humans (e.g. `{:actor/id "sid" :actor/type :human}`), system, and agents. Every request carries actor, time, and a caller-supplied request id. Callers also supply entity ids (turn ids, bundle ids, run ids); the kernel derives sub-identities (event ids, dispatch ids, control ids) deterministically from the request id.

---

## Operations

### Cross-cutting guarantees (apply to every request operation)

- **One request → exactly one durable decision.** Accepted or rejected; rejection carries a machine-readable reason (e.g. `:space/not-found`). Both outcomes are durable and queryable by request id. A rejected request produces zero facts.
- **Accepted decisions enumerate their facts.** The decision lists every event id minted by the request, in a deterministic order (e.g. compose-and-send: space, turn, context-bundle, llm-turn-run). Each event is individually readable with its type (e.g. `:space/created`). The decision also identifies the space it applies to.
- **Deterministic derived identity.** Event ids, LLM dispatch ids, and LLM control ids are pure functions of the request id (`<request-id>/event/<entity>`, `<request-id>/llm-request`, `<request-id>/llm-control`). Consequence: a retried/redelivered request collides with itself instead of minting duplicates. Any implementation must preserve this collision-instead-of-duplication property.
- **Pure builders / writer asymmetry.** Constructing a request, observation, or control record mutates nothing. Only the durable append changes state. Verified by the property test: building requests and observations without appending leaves every read surface (canonical, LLM-side, control, catalog, projection) byte-identical.
- **Determinism / rebuildability.** Replaying the same appended inputs from empty state must reproduce **identical** canonical state, catalog, controls, cost rollups, and projections (full-snapshot equality across two independent runtimes). No wall-clock reads, randomness, or iteration-order nondeterminism may leak into materialized state. All timestamps come from the request/observation payloads.
- **Decisions are awaitable.** Callers append and then await the decision by request id; the UI blocks on this. Decision latency is interactive-path latency.

### 1. Create space (`space/create`)

Creates a new inhabitable space (chat work area) with optional title and actor.

- **Latency:** Interactive. A human clicks "new chat" and waits for the decision plus the readable space. Target: decision and canonical readability well under a second; tens of ms desirable. Not single-digit-ms critical.
- **Throughput:** Low. Human- and agent-initiated; scales with users × sessions plus fork activity. Tens to hundreds per day per user, not thousands per second.
- **Consistency/correctness invariants:**
  - Accepted create mints exactly one space-created event; the decision references it.
  - New space materializes as: given title, status `:active`, turn-count 0, empty turn order.
  - The space becomes a catalog object (type `:space`) carrying the title.
  - Title is optional (creates without title are accepted).
- **Data growth and scale:** Number of spaces is unbounded (every conversation and every fork). Access is point-lookup by space id; the space graph (fork lineage) needs per-parent enumeration of children.
- **Concurrency:** Two creates racing on the same space id must not produce conflicting or duplicated space facts. (Spec is silent on whether the second is rejected, replayed, or last-write — flagged in Ambiguities. Deterministic event ids make same-request retries safe; *different*-request collisions are the open case.)
- **Edge cases:** missing title (accepted); duplicate space id (flagged); create racing a compose-and-send that would implicitly create the same space (flagged).

### 2. Compose-and-send (turn + frozen context bundle + LLM dispatch)

The user (or agent) sends a prompt into a space. This is the core operation: it creates the space if absent, appends a turn, **freezes a context bundle**, and derives the LLM run dispatch.

- **Latency:** Interactive send path. The turn must appear in the chat canvas promptly (sub-second; ideally ~100ms), and the derived run must become visible to the executor in the same window — the user is waiting for the model to start. Bundle freezing happens synchronously within acceptance, never after the run starts.
- **Throughput:** Human message cadence per space (peak a few per minute) × active spaces, plus agent-initiated sends. Hundreds–thousands per day total; bursty, not streaming-volume.
- **Consistency/correctness invariants:**
  - **Implicit space creation:** sending into a nonexistent space creates it as part of the same accepted decision. All sibling facts — space (if new), turn, bundle, run dispatch — are all-or-nothing: no observable state where the turn exists but the bundle or dispatch does not.
  - **Exactly one turn, one bundle, one run per send.** Verified per send; also the turn→bundle and turn→run mappings are single-valued.
  - **Context bundle freezing semantics (the heart of the kernel):**
    - Frozen at acceptance time, before any execution; **immutable forever after**.
    - Carries a collision-resistant content hash (`sha256:`-prefixed); the accepted decision exposes the **same** hash, so callers can verify what was frozen without refetching.
    - The rendered model input must include the prompt text and a rendered reference for every ref in the request, by kind: `object:<id>`, `slice:<id>`, and user-authored derivatives rendered as `user-authored-derivative:<id>` (authorship visibly preserved in the model input).
    - **Bundle-owned execution options only:** agent kind, model, approval policy, sandbox, cwd are frozen into the bundle. Non-bundle-owned options (e.g. executor pool selection) must be **excluded** — physical placement is not part of the frozen epistemic context.
    - Refs are rendered by id without requiring the referenced material to exist yet (observed: refs to never-created objects/slices still render). Dangling refs are the requester's responsibility — flagged.
  - **Bundle-before-run ordering:** the run record points at the bundle id, and the bundle must be durably materialized so that any consumer that can see the run can resolve its bundle. The run never starts against an unfrozen or missing bundle.
  - **Cross-kernel dispatch obligations:** the derived LLM run request carries the deterministic id `<request-id>/llm-request`, the turn id, and the bundle id; the LLM side records its own acceptance decision; the run materializes as `:pending` and enters the executor's pending queue. Space guarantees the dispatch is durable and traceable back to the originating space request.
  - **Decision exposes sibling facts** so projections and callers need no second lookup: space id, turn id, bundle id, bundle hash, llm thread id, run id, derived llm request id, and the full ordered event-id list.
  - **Turn ordering:** turns within a space form a single total order (request acceptance order); turn-count increments by exactly 1 per turn.
- **Idempotency (caller-supplied key):**
  - Replaying a send with the same idempotency key is **accepted** (not rejected) but flagged `:idempotency/replayed? true`.
  - The replay returns the **original** send's facts: same event ids, original turn/bundle/run ids — even if the replayed request supplied different fresh ids. None of the replay's fresh ids may be partially created (no turn, no bundle, no run, no dispatch, no LLM-side row).
  - An idempotency index maps key → the original send's facts.
  - Under concurrent replays of the same key, at most one set of facts is ever minted.
- **Data growth and scale:** Turns per space are unbounded (long-lived chats: hundreds–thousands). Chat-canvas reads need ordered range access over a space's turns; everything else is point lookup. Bundles can be large (rendered context: KBs–MBs) and are write-once/read-few — stored full fidelity, never trimmed.
- **Concurrency:** Concurrent sends into the same space are both accepted and must serialize into one well-defined turn order. Concurrent sends with the same idempotency key: exactly-one-wins. An LLM run request arriving with a control-type request type must fail validation (`:request/type-invalid`) — type confusion between sends and controls is rejected at the boundary.
- **Edge cases:** send creating the space (covered, accepted); empty prompt text (untested — flagged); duplicate caller-supplied turn/bundle/run ids across *different* sends (untested — flagged); refs to nonexistent material (rendered anyway — flagged as deliberate-or-risk); replay with same key but different payload (returns original facts; divergent payload silently ignored — flagged).

### 3. Space-only material turns: comment, slice, derivative

Turns that add material to the space without any model work: `:turn/comment-create` (overlay), `:turn/slice-create`, `:turn/derivative-create`.

- **Latency:** Interactive (pin a note, excerpt a span, rewrite a model answer). Sub-second canvas visibility.
- **Throughput:** Human-paced; low. A few per minute peak per space.
- **Consistency/correctness invariants:**
  - **Space must already exist.** Space-only turns cannot invent their containing space: rejected with `:space/not-found`; the turn must not exist afterwards. (Asymmetric with compose-and-send, which *does* implicitly create — this asymmetry is contract, not accident.)
  - **Strictly no LLM side effect.** No context bundle frozen (`bundle-by-turn` is empty), no run derived (`run-by-turn` empty), no executor pending work created. The decision's event list contains only the turn event (e.g. exactly `["<req>/event/turn"]`).
  - Each is an ordered turn: appended to turn order, turn-count incremented, turn kind preserved (`:turn/comment-create` etc.), prompt text preserved.
  - **Slice semantics (immutable snapshot + dual hash):**
    - A slice records `snapshot/text` (defaulting to the source's content text when not explicitly supplied), a `snapshot/hash` computed from the snapshot text, and a `source/content-hash` preserved from the request — both hashes kept, and they differ whenever the slice is a sub-span of larger source material.
    - Slice creation must **not** mutate the raw source item.
    - Once created, the slice is immutable: later (including mutated/duplicate) re-observations of the source item must not rewrite the slice — its snapshot text and source hash survive unchanged.
    - Raw LLM items themselves are immutable on the read surface: a second observation reusing the same item id with different text leaves the first recorded content in place (by-id and by-run reads both still return the original text).
    - Slice creation does not require the source item to exist in the LLM store — the requester supplies the snapshot; the source hash is provenance, not a foreign-key check.
    - Referencing a raw LLM item **lazily promotes** it into the catalog: before the slice, the raw item has no catalog object; after, it exists with type `:llm-item` and a relation raw-item → slice.
  - **Comment/overlay semantics:** stored and readable by overlay id; may carry refs to older turns; purely additive annotation.
  - **Derivative semantics (user-authored rewrite of model material):**
    - Authorship recorded as `:user`; rendering directive `:user-authored` on the derivative and `:user` authorship on its catalog object.
    - When a later bundle references the derivative, the rendered model input must mark it `user-authored-derivative:<id>` — the model must be able to distinguish the human's rewrite from model output. Authorship provenance survives every projection of the derivative.
- **Data growth and scale:** Slices/overlays/derivatives are unbounded per space; point lookup by id dominates; relation edges (raw→slice, etc.) need per-object adjacency enumeration.
- **Concurrency:** Slice creation racing new observations of its source: the slice keeps the snapshot supplied at request time (observed). Duplicate slice/overlay/derivative ids across requests: untested — flagged; deterministic event ids protect only same-request retries.
- **Edge cases:** slice of a never-observed item (accepted — covered); snapshot text omitted (defaults from source content text — covered); comment refs to turns that don't exist (accepted, rendered as refs — flagged); derivative without source (untested — flagged).

### 4. Patch proposal ingestion (LLM observation → pending space proposal)

Patch-like observations streamed from an LLM run (e.g. `:codex/patch-proposal`) become **pending patch proposals** in the space that owns the run's turn.

- **Latency:** Streaming-observation cadence; the proposal must be visible quickly (sub-second) because a human is watching the run and will act on it.
- **Throughput:** Observations as a class are high-volume (token streams); the patch-like subset is low (a few per run). The ingestion path must tolerate sitting inside a high-volume stream without losing the low-volume extracts.
- **Consistency/correctness invariants:**
  - The proposal binds: proposal id (from the observation), originating run id, owning space id, source turn id, summary text, and the proposed file list (paths + hunk counts) — all preserved verbatim.
  - Initial status is `:pending`.
  - At-least-once delivery safety: redelivering the same observation must not duplicate the proposal; redelivery after resolution must not reset a resolved proposal back to `:pending` (required by retry-safety even though untested — flagged as a must-hold).
- **Data growth and scale:** Proposals unbounded over time; point lookup by proposal id; plausibly per-space/per-run listing later (flagged — only by-id reads are exercised).
- **Concurrency:** A resolution turn racing the proposal's materialization (resolve-before-visible) is untested — flagged. Two proposals from the same run materialize independently (covered: accept one, reject the other).
- **Edge cases:** proposal naming an unknown space or run (untested — flagged); observation missing the proposal id (untested — flagged).

### 5. Patch resolution turns (`:turn/patch-accept`, `:turn/patch-reject`)

Human accepts or rejects a proposed patch **as a space turn** — an epistemic act recorded in the conversation, not a tool approval.

- **Latency:** Interactive.
- **Throughput:** Human-paced; low.
- **Consistency/correctness invariants:**
  - **Not a tool approval:** the decision must carry **no** LLM-control type, and no LLM control record may be derived or linked to the resolution turn. Patch resolution and tool approval are disjoint flows even though both look like "user says yes/no".
  - Accept: proposal status → `:accepted`, recording the resolving turn id (`resolution/turn-id`).
  - Reject: proposal status → `:rejected`, recording the resolving turn id and the supplied reason (e.g. `:not-right-shape`).
  - The resolution is itself an ordered turn (kind preserved, appears in turn order).
  - Acceptance is recorded intent only — this kernel does not apply the patch to any world objects; downstream application is out of scope here.
- **Data growth and scale:** Same as turns; proposals transition in place.
- **Concurrency:** Accept and reject racing on the same proposal (untested — flagged: needs a defined winner, plausibly first-resolution-wins); resolution racing late redelivery of the proposal observation (must not resurrect `:pending` — see op 4).
- **Edge cases:** patch-accept **without** a proposal id is accepted as a plain turn and mutates no proposal (observed — flagged: silent no-op vs validation error is an open call); resolving a nonexistent or already-resolved proposal id (untested — flagged); reject without reason (untested — flagged).

### 6. Control turns: tool-approval-resolve, cancel, compact, steer

Run-steering acts that enter Space first (as turns) and derive an LLM control record on acceptance. Space records the act; the LLM kernel/executor consumes the control.

- **Latency:** Approval resolution and cancel sit on a **blocked-run path** — the executor is waiting. Human reaction time dominates arrival, but propagation from accepted turn to visible run-status change should be ~100ms-class, not seconds. Compact/steer are background-shaping; sub-second is fine.
- **Throughput:** Low; bounded by approvals/cancellations per run (a handful) × active runs.
- **Consistency/correctness invariants:**
  - **Space-first:** each control enters as a space turn; on acceptance, exactly one LLM control record is derived with deterministic id `<request-id>/llm-control`; the decision carries the control type (e.g. `:approval/resolve`, `:turn/cancel`, `:compact/request`); turn → control linkage is readable both ways.
  - **Type discrimination:** overlapping keywords (`:turn/cancel`, `:turn/steer` exist as both turn-request types and control types) are discriminated by the **request type**, never by a control-type field on the request. A run request whose request type is a control type fails validation with `:request/type-invalid`.
  - **Approval resolve (approved):** the pending approval becomes `:approved`; the run transitions to `:running`; the pending-approval index entry clears. The control carries the **native correlation id** (e.g. JSON-RPC request id) through unchanged, so the executor can answer the right native protocol request.
  - **Approval resolve (expired/timeout):** durably recorded as a resolution turn (audit trail of the timeout); approval status and decision both `:expired`; pending-approval entry clears; the run fails with a structured error identifying the cause (`:approval/declined`, decision `:expired`).
  - **Cancel:** run status → `:cancelled`; the run leaves the executor pending queue; control recorded with the cancel reason.
  - **Compact:** control recorded; run status **unchanged** (still `:pending`/`:running`); the run accumulates an ordered compaction history — each entry preserving control id, time, acting actor, and full payload (space id, turn id, run id, strategy).
  - **Steer:** exists as a request type with the same discrimination rules; its run-side semantics are not exercised by the contract evidence — flagged.
- **Data growth and scale:** Controls unbounded but low-volume; point lookup by control id and by turn; compaction history grows per run (small).
- **Concurrency:** Double-resolve of one approval (untested — flagged: second must not corrupt; plausibly no-op/reject); cancel racing run completion (untested — flagged: a terminal run must stay terminal, cancel must not resurrect); concurrent cancel + approval-resolve on the same run (untested — flagged).
- **Edge cases:** resolving a nonexistent approval id (untested — flagged); cancel of an already-cancelled run (untested — flagged: should be idempotent, not corrupting); compact against a missing run (untested — flagged); decision values other than `:approved`/`:expired` (e.g. explicit `:declined` — unexercised, flagged).

### 7. Fork-from-span (`space/fork-from-span`)

A span of material in one space becomes the anchor of a **new child space**, with native LLM-thread forking so the child's model run continues from the parent's native conversation.

- **Latency:** Interactive (a user forks a span to explore it). Child space, anchor slice, bundle, and dispatch visible sub-second. The child's *run start* is additionally gated on fork-binding durability (below) — that gate may add latency and must fail safe, not fast-start.
- **Throughput:** Low; human exploration acts plus occasional agent-driven forking.
- **Consistency/correctness invariants:**
  - One accepted fork mints an atomic family of facts: child space (status `:active`, turn-count 1), anchor slice (snapshot text + source content hash, same immutability rules as op 3), turn, frozen bundle whose rendered input includes the anchor `slice:<id>` ref, and the derived LLM run dispatch.
  - **Lineage is bidirectional:** the child records its parent space id; the parent's fork graph contains the child. Spaces form a graph related by fork lineage; per-parent child enumeration must be supported.
  - **Native thread fork plumbing:** the dispatch carries the parent's native thread id (`fork/from-native-thread-id`) and, when pre-bound, the child's native thread id; the materialized run records both. This is Space's hand-off obligation: the LLM side must receive everything needed to fork the native conversation.
  - **Fork-binding gate (hard safety rule):** the executor must **not** claim or start the forked child's run until the native binding is durable. A premature claim attempt returns not-spawned with the explicit reason (`:fork-binding-not-durable`), invokes **no** adapter/model call, and leaves the run `:pending` and unclaimed. Rationale: starting a child model run against a non-durable fork binding could attach the run to the wrong (or no) native conversation; the gate must be re-checkable and side-effect-free.
- **Data growth and scale:** Fork lineage graph grows with exploration; expected shallow-but-wide (many children per heavily-explored space). Child enumeration per parent; parent pointer per child.
- **Concurrency:** Fork racing parent deletion is moot (no deletion exists); two forks of the same span are independent children (each request mints its own ids); fork with a child space id that already exists (untested — flagged).
- **Edge cases:** fork from a nonexistent parent (untested — flagged: plausibly rejected like space-only turns, but unverified); binding never becomes durable (run pends forever — no timeout policy in evidence, flagged); fork without a pre-bound child native thread id (covered — binding gate handles late binding).

### 8. Catalog and relation derivation (derived obligation, not a user-facing call)

Accepted space facts automatically become a typed object catalog plus a relation graph — the substrate for "everything in the space is an inspectable object".

- **Latency:** Catalog/relations are read by UI detail panes shortly after the originating decision; they may materialize asynchronously after the decision but should converge promptly (sub-second; tests poll-await them separately from decisions).
- **Throughput:** One catalog object + a constant handful of edges per accepted fact; scales with total accepted facts.
- **Consistency/correctness invariants:**
  - **Eager cataloging** of accepted space facts: space, turn, context bundle, run become typed objects (`:space`, `:turn`, `:context-bundle`, `:llm-turn-run`) carrying their salient fields (title on space; bundle id on turn; turn id on bundle; llm thread id on run).
  - **Relation edges** for a compose-and-send: space→turn, turn→bundle, turn→run, bundle→run; **reverse edges queryable** (e.g. run's in-edges = {turn, bundle}).
  - **Lazy promotion** of raw LLM items: a raw item observed by the LLM side gets **no** catalog object until something in the space references it (e.g. a slice); upon reference it appears with type `:llm-item` and edge raw-item→slice. Slices and derivatives are cataloged with authorship.
  - Catalog object ids are a pure function of (type, entity id) — stable, derivable by any caller without lookup.
- **Data growth and scale:** O(all accepted facts) objects; adjacency must support per-object in/out enumeration (relations pane). Unbounded.
- **Concurrency:** Derived purely from accepted facts — no independent writers; determinism rules of the rebuildability guarantee apply.
- **Edge cases:** reading a catalog object before materialization converges returns absent (callers must treat catalog reads as eventually consistent with decisions); relations of an object with no edges (empty, not error).

### 9. Read operations and projections

Canonical reads: space by id (title, status, turn-count, parent lineage), ordered turn ids per space, turn by id (kind, prompt, bundle pointer), bundle by id and by turn, run-dispatch by run id, run id by turn, control id by turn, slice/overlay/derivative by id, patch proposal by id, event by id, decision by request id, send-by-idempotency-key, space fork graph, catalog object by id, relations in/out by object id.

Projections (each self-describing with a projection type tag): **chat canvas** per space (type `:chat-canvas`, declared source = canonical space state, title, full turn order, latest turn), **object detail** per catalog object, **object relations** per object, plus the LLM-side surfaces Space coordinates with (**run detail** per run, **thread cost rollup** per llm thread with token totals accumulated from usage observations).

- **Latency:** UI subscription/point reads — fast point lookups (single-digit to low-tens of ms server-side). Chat canvas is the hot read (every open space renders it).
- **Throughput:** Read-dominated system; every UI surface polls/subscribes. Reads vastly outnumber writes.
- **Consistency/correctness invariants:**
  - All reads are **eventually consistent with decisions**: a caller that has seen an accepted decision must be able to await convergence of every fact the decision enumerates (tests await materialization after every decision; absence before convergence is `nil`, never an error).
  - Reads must be safe in **every** entity state, including "never existed" (return absent/empty, not error) and "request rejected" (read what rejection implies: nothing created).
  - **Projection rebuildability:** every projection must be a pure function of canonical accepted state — declared as such (the chat canvas names its canonical source), and verified by full-snapshot equality across independent replays. A projection may be dropped and rebuilt at any time without changing any answer.
- **Data growth and scale:** turn-order reads need ordered range access per space; everything else point lookup. Cost rollup is an accumulating aggregate per llm thread (monotonically growing token totals).
- **Concurrency:** Reads concurrent with writes see either before or after a decision's fact-family, converging to after; no torn intermediate where e.g. a turn is in turn order but unreadable by id (flagged as required — tests await per-fact, so torn visibility windows are tolerated only as transient absence, never as inconsistency between a decision and final state).
- **Edge cases:** read of missing id → absent; empty space → empty turn order (not absent); relations of unconnected object → empty in/out; cost rollup before any usage → absent/zero (flagged: which one is unspecified).

### 10. Request validation (pure, pre-append)

- **Latency:** In-process, pure function; microseconds.
- **Throughput:** Once per request construction.
- **Invariants:** Validation never mutates state. Discrimination errors are structured (`:request/type-invalid`). A request that passes validation can still be rejected by the kernel decision (e.g. `:space/not-found`) — validation is shape-level, decisions are state-level. Control-type/request-type confusion is caught at validation (see op 6).
- **Edge cases:** stray discriminator-like fields on a valid request (e.g. a bogus control-type field on a turn request) must not change its classification — the request type alone discriminates.

---

## Entity State × Write Matrix

Read-key (used below):
- `read-space` (record), `read-turns-by-space` (ordered ids), `read-turn`, `read-context-bundle`, `bundle-by-turn`, `run-by-turn`, `control-by-turn`, `read-slice`, `read-overlay`, `read-derivative`, `read-patch-proposal`, `read-llm-run-request` (dispatch), `llm-run` (status record), `llm-pending` (executor queue), `llm-control`, `approvals-by-run`, `pending-approval`, `read-event`, `decision`, `send-by-idempotency`, `space-graph`, `read-object` (catalog), `relations` (in/out + projection), `chat-canvas`, `object-detail`, `run-detail`, `cost-rollup`.

ALWAYS-readable rule: every read below can be called in every state; "absent" means nil/empty, never an error.

### Entity: Space
States: **does-not-exist**, **active-empty** (0 turns), **active-with-turns**, **forked-child** (active + parent lineage). No archived/closed/deleted state exists in the contract (flagged).

**does-not-exist × space/create (accepted)**
- decision: `:accepted`, one space event id, routed to this space
- read-event: `:space/created`
- read-space: title, `:active`, turn-count 0
- read-turns-by-space: `[]` (empty, not absent)
- chat-canvas: converges to canvas with title, empty turn order
- read-object/object-detail (space object): type `:space`, title
- relations: empty in/out
- space-graph: no children
- send-by-idempotency: unchanged (creates carry no send key)

**does-not-exist × compose-and-send (accepted — implicit creation)**
- decision: `:accepted`, four event ids (space, turn, bundle, run), sibling fact ids + bundle hash
- read-space: exists, turn-count 1
- read-turns-by-space: `[turn-id]`
- read-turn: kind `:compose-and-send`, bundle pointer
- read-context-bundle / bundle-by-turn: frozen bundle / its id
- run-by-turn, read-llm-run-request, llm-run, llm-pending: dispatch + pending run present
- chat-canvas: title (if given), turn order `[turn-id]`, latest-turn = this turn
- read-object ×4 (space/turn/bundle/run), relations: space→turn, turn→{bundle,run}, bundle→run; reverse edges
- send-by-idempotency (if key given): key → this send's facts

**does-not-exist × space-only turn / control turn (rejected)**
- decision: `:rejected`, reason `:space/not-found`
- read-space, read-turn: absent — the turn was NOT created
- read-turns-by-space: `[]`/absent
- read-slice/read-overlay/read-derivative (for the would-be material): absent
- bundle-by-turn, run-by-turn, control-by-turn: absent
- llm-control, llm-pending: untouched (no control derived, no queue entry)
- chat-canvas, catalog reads: nothing new
- read-event: no event minted for this request

**does-not-exist × fork-from-span (as child, accepted)**
- decision: `:accepted`
- read-space (child): `:active`, turn-count 1, `:parent-space/id` = parent
- space-graph (parent): contains child
- read-slice (anchor): snapshot text + source hash
- read-context-bundle: rendered input includes `slice:<anchor-id>`
- read-llm-run-request: executor fork fields (parent native thread id; child native id if bound)
- llm-run: `:pending`, fork fields recorded; llm-pending: contains run
- read-turns-by-space, chat-canvas, catalog/relations: as for compose-and-send

**active-empty × compose-and-send (accepted)**
- decision: event ids include the turn/bundle/run family (space already exists — whether a space event re-mints is unspecified; observed contract only pins the four-event list for implicit creation — flagged)
- read-space: turn-count 0→1; title unchanged unless first set here
- read-turns-by-space: `[turn-id]`; chat-canvas latest-turn updates
- all bundle/run/dispatch/pending/catalog/relations reads: as above

**active-with-turns × compose-and-send (accepted)**
- read-turns-by-space: previous order + new turn appended (total order preserved)
- read-space: turn-count +1
- chat-canvas: latest-turn = newest; full order intact
- prior turns/bundles/runs: unchanged by this write (immutability of history)
- all other reads: as above

**active (any) × compose-and-send replay (same idempotency key, accepted+replayed)**
- decision: `:accepted` + `:idempotency/replayed? true`, event ids = ORIGINAL send's event ids, fact ids = original turn/bundle/run
- read-turns-by-space: unchanged (no new turn)
- read-turn (replay's fresh turn id): absent
- read-context-bundle (replay's fresh bundle id): absent; original bundle: present
- read-llm-run-request / llm-run (replay's fresh run id): absent; original run: present
- send-by-idempotency: still → original facts
- read-space: turn-count unchanged
- chat-canvas, catalog, relations: unchanged

**active (any) × space-only turn (accepted)**
- decision: `:accepted`, exactly one event id (`.../event/turn`)
- read-turns-by-space: turn appended; read-space: turn-count +1
- read-turn: kind + prompt text
- bundle-by-turn, run-by-turn: absent (the no-LLM-side-effect guarantee)
- llm-pending: unchanged
- material read (slice/overlay/derivative by id): present per kind
- chat-canvas: latest-turn = this turn
- catalog: material object present (slice/derivative; overlay catalog presence unexercised — flagged); raw source item promoted if referenced (slice)

**active (any) × control turn (accepted)** — see LLM-run entity for run-side transitions
- read-turns-by-space / read-space: turn appended / count +1
- control-by-turn: control id; llm-control: control record
- bundle-by-turn / run-by-turn for the control turn: absent (controls freeze nothing)
- chat-canvas: latest-turn = control turn

**forked-child × any turn write**: behaves as active-with-turns; additionally
- read-space: parent lineage persists across all later writes
- space-graph (parent): membership persists

### Entity: Turn
States: **does-not-exist**, **exists(kind)** — turns are immutable once created; no edit/delete exists.

**does-not-exist × compose-and-send (accepted)**
- read-turn: kind `:compose-and-send`, bundle id
- read-turns-by-space: contains id at correct position
- bundle-by-turn: bundle id; run-by-turn: run id; control-by-turn: absent
- read-object (turn object): type `:turn`, bundle id field
- relations: in {space}, out {bundle, run}
- chat-canvas latest-turn: this turn
- decision: turn event id present; primary event/id = turn event

**does-not-exist × space-only turn (accepted)**
- read-turn: kind preserved (`:turn/comment-create` / `:turn/slice-create` / `:turn/derivative-create` / `:turn/patch-accept` / `:turn/patch-reject`), prompt text
- bundle-by-turn: absent; run-by-turn: absent; control-by-turn: absent
- relations: in {space}; out edges to its material (slice case observed via raw→slice; turn→material edge shape otherwise unexercised — flagged)

**does-not-exist × control turn (accepted)**
- read-turn: kind (e.g. `:turn/tool-approval-resolve`, `:turn/cancel`, `:turn/compact-request`)
- control-by-turn: `<request-id>/llm-control`
- bundle-by-turn / run-by-turn: absent

**does-not-exist × rejected request (any kind)**
- read-turn: absent; read-turns-by-space: does not contain it; all by-turn indexes: absent

**exists × any later write (other turns, observations, controls)**
- read-turn: unchanged — kind, prompt, bundle pointer immutable
- read-turns-by-space: position stable
- bundle-by-turn / run-by-turn / control-by-turn: stable

### Entity: Context bundle
States: **does-not-exist**, **frozen**. No mutable state exists by contract.

**does-not-exist × compose-and-send / fork (accepted)**
- read-context-bundle: execution options (bundle-owned only — non-bundle-owned keys ABSENT), `sha256:` hash, rendered model input containing prompt + every ref (`object:`, `slice:`, `user-authored-derivative:` forms)
- bundle-by-turn: bundle id
- decision: same hash value as the bundle (hash equality is contract)
- llm-run: points at this bundle id
- read-object (bundle object): type `:context-bundle`, turn id
- relations: in {turn}, out {run}

**frozen × any later write (turns, observations, controls, replays)**
- read-context-bundle: byte-identical — hash, options, rendered input never change
- all pointers (bundle-by-turn, run's bundle id, decision hash): stable

**does-not-exist × idempotent replay carrying a fresh bundle id**
- read-context-bundle (fresh id): absent forever — replays must not mint bundles

### Entity: Slice
States: **does-not-exist**, **exists (immutable snapshot)**.

**does-not-exist × turn/slice-create (accepted)**
- read-slice: snapshot text (explicit or defaulted from source content), snapshot hash = hash(snapshot text), source content hash preserved; the two differ for sub-span slices
- raw source item (item-by-id / items-by-run): UNCHANGED by slice creation
- read-object (slice object): type `:slice`; read-object (raw item): newly promoted, type `:llm-item` (was absent before)
- relations: raw-item → slice (out of raw, in of slice)
- read-turns-by-space: slice turn appended
- bundle-by-turn / run-by-turn (slice turn): absent

**exists × later source re-observation (same item id, new content)**
- read-slice: unchanged (snapshot text + source hash survive)
- item-by-id / items-by-run: still the ORIGINAL content (raw items immutable on read surface)
- llm-run last-seq: advances (sequence tracking continues)
- read-object (raw): persists as `:llm-item`

**exists × fork-from-span using the slice as anchor**
- read-slice: unchanged; bundle of fork renders `slice:<id>` ref
- relations: slice unchanged plus any new ref edges (unexercised shape — flagged)

**does-not-exist × rejected slice turn (missing space)**
- read-slice: absent; no catalog object; no raw promotion

### Entity: Overlay (comment)
States: **does-not-exist**, **exists**.

**does-not-exist × turn/comment-create (accepted)**
- read-overlay: present
- read-turn: kind `:turn/comment-create`, prompt text, refs preserved
- bundle-by-turn / run-by-turn: absent; llm-pending: unchanged
- chat-canvas: comment turn in order
- catalog object for overlay: unexercised — flagged

**exists × any later write**: read-overlay unchanged (no mutation path exists)

### Entity: Derivative
States: **does-not-exist**, **exists (user-authored)**.

**does-not-exist × turn/derivative-create (accepted)**
- read-derivative: authorship `:user`, render-as `:user-authored`, content text
- read-object (derivative): authorship `:user`, type per catalog
- bundle-by-turn / run-by-turn (derivative turn): absent
- read-turns-by-space: appended

**exists × later compose-and-send referencing it**
- read-derivative: unchanged
- read-context-bundle (new bundle): rendered input contains `user-authored-derivative:<id>` — authorship visible to the model
- relations: unexercised additional edges — flagged

### Entity: Patch proposal
States: **does-not-exist**, **pending**, **accepted**, **rejected**.

**does-not-exist × patch observation ingest**
- read-patch-proposal: `:pending`, run id, space id, summary text, file list verbatim
- decision/read-event: none (observations are not requests — no space decision minted)
- read-turns-by-space: unchanged (ingest creates no turn)
- llm-control: none

**does-not-exist × turn/patch-accept with no/unknown proposal id (accepted turn)**
- read-patch-proposal: absent/unchanged — nothing mutated
- read-turn: `:turn/patch-accept` exists; turn order appended
- control-by-turn / llm-control: absent (never an approval)
- (flagged: silent no-op vs validation rejection is an open product call)

**pending × turn/patch-accept (accepted)**
- read-patch-proposal: `:accepted`, resolution turn id recorded
- decision: `:accepted`, NO llm-control type field
- control-by-turn (resolution turn): absent
- read-turns-by-space: resolution turn appended
- llm-run of the originating run: untouched by patch resolution

**pending × turn/patch-reject (accepted)**
- read-patch-proposal: `:rejected`, resolution turn id, reason preserved
- all other reads: as accept row (no control, turn appended, run untouched)

**pending × duplicate ingest redelivery**
- read-patch-proposal: still single `:pending` proposal, not duplicated (required; untested — flagged)

**accepted/rejected × duplicate ingest redelivery**
- read-patch-proposal: resolution must survive — status NOT reset to pending (required by retry-safety; untested — flagged)

**accepted/rejected × second resolution turn**
- read-patch-proposal: defined winner needed (first-resolution-wins presumed; untested — flagged)
- read-turn (second resolution turn): would exist as a turn regardless (turns are accepted independently — flagged)

### Entity: LLM turn-run (as governed by Space — execution state owned across the boundary)
States: **not-dispatched**, **pending** (dispatched, unclaimed), **pending-gated** (fork binding not durable), **running**, **cancelled**, **failed**. (A completed/succeeded terminal state must exist in the full system but is not exercised by Space's contract — flagged.)

**not-dispatched × compose-and-send / fork (accepted)**
- read-llm-run-request: dispatch with `<req>/llm-request` id, turn id, bundle id (+ fork fields for forks)
- llm-run: `:pending`, bundle pointer (+ fork native ids for forks)
- llm-pending: contains run id under the executor task
- run-by-turn: run id; run-detail projection: `:pending`
- read-object (run): type `:llm-turn-run`, llm thread id; relations: in {turn, bundle}

**pending-gated (fork) × executor claim attempt**
- claim result: not spawned, reason `:fork-binding-not-durable`, adapter NEVER invoked
- llm-run: still `:pending`, `:claimed-by` nil
- llm-pending: run still queued
- all space-side reads: unchanged

**pending × turn/tool-approval-resolve (approved)**
- llm-run: `:running`
- approvals-by-run: approval `:approved`; pending-approval: cleared (nil)
- llm-control: `:approval/resolve` record with native correlation id
- control-by-turn: control id; decision: llm-control type `:approval/resolve`
- llm-pending: (claim semantics during running unexercised — flagged)
- run-detail projection: converges to `:running`

**pending × turn/tool-approval-resolve (expired)**
- llm-run: `:failed`, error reason `:approval/declined`, decision `:expired`
- approvals-by-run: `:expired` status and decision; pending-approval: cleared
- llm-control: recorded; decision: `:approval/resolve`
- run-detail: converges to `:failed`

**pending × turn/cancel**
- llm-run: `:cancelled`; llm-pending: run removed
- llm-control: `:turn/cancel` record; decision: llm-control type `:turn/cancel`
- read-turn (cancel turn): exists; run-by-turn (cancel turn): absent
- run-detail: `:cancelled`

**pending × turn/compact-request**
- llm-run: status unchanged `:pending`; compactions list grows by one ordered entry (control id, time, actor, payload)
- llm-control: `:compact/request` record
- llm-pending: unchanged (run still queued)
- cost-rollup: unchanged by compaction itself

**running/cancelled/failed × further controls (cancel-after-terminal, double-resolve, compact-after-cancel)**
- required: terminal states must not be resurrected and pending queues must not regain terminal runs; exact decision outcomes unexercised — flagged
- llm-run / llm-pending / run-detail: must stay consistent with the terminal state

### Entity: Approval
States: **does-not-exist**, **pending**, **approved**, **expired**. (Explicit `:declined` unexercised — flagged.)

**does-not-exist × approval observation**
- pending-approval: present; approvals-by-run: present with native correlation id
- llm-run: status unchanged by the request itself (executor-side blocking is outside Space's contract)

**pending × resolve approved** — see run matrix: approval `:approved`, pending cleared, run `:running`
**pending × resolve expired** — approval `:expired` (status and decision), pending cleared, run `:failed` with structured error
**approved/expired × second resolve or redelivered observation**
- approvals-by-run: resolution must survive; pending-approval must remain cleared (required; untested — flagged)

### Entity: Catalog object + relation edges
States: **not-materialized**, **materialized-eager** (space/turn/bundle/run/slice/derivative), **lazily-promoted** (raw LLM items).

**not-materialized × accepted compose-and-send/fork**
- read-object: typed object per fact; object-detail projection: type-tagged
- relations/artifact-graph out+in: full edge family (space→turn→{bundle,run}, bundle→run) with reverse traversal

**not-materialized (raw item) × LLM item observation alone**
- read-object (raw item): STILL absent — observation alone does not catalog
- item-by-id: present on the LLM read surface

**not-materialized (raw item) × turn/slice-create referencing it**
- read-object (raw item): promoted, type `:llm-item`
- relations: raw→slice both directions queryable

**materialized × any later write**
- read-object: stable identity (catalog ids are pure functions of type+id); fields immutable in evidence
- relations: grow monotonically; existing edges never removed (no removal path exists)

### Entity: Idempotency send record
States: **absent**, **recorded**.

**absent × first send with key**
- send-by-idempotency: key → original facts (run id observed; full fact set implied)
- decision: accepted, not replayed

**recorded × replayed send (same key)**
- send-by-idempotency: unchanged (still original)
- decision: accepted + replayed flag + original event ids
- every fresh-id read from the replay: absent (see Space/Bundle/Run matrices)

**recorded × replay with same key but different payload/space**
- required: original facts win wholesale; divergent payload ignored (cross-space same-key behavior untested — flagged)

### Entity: Request / Decision / Event audit records
States: **unseen**, **decided-accepted**, **decided-rejected**.

**unseen × any append**
- decision: awaitable by request id; exactly one per request
- read-event: each accepted fact's event readable with type; rejected requests mint zero events
**decided × redelivery/retry of the same request**
- decision: stable (deterministic derived ids make retries collide, not duplicate — required by the kernel conventions)

---

## Ambiguities and Unstated Requirements (flagged for Phase 1)

1. **Duplicate entity ids across different requests** (space id re-create, turn/bundle/run/slice/overlay/derivative id reuse with different request ids): no contract evidence. Deterministic derived ids protect same-request retries only. Needs a defined rule (reject vs first-wins).
2. **Patch-accept without/with unknown proposal id is a silent no-op turn** (observed): decide whether that stays contract or becomes a validation/decision rejection.
3. **Re-resolution races**: double approval-resolve, accept-vs-reject race on one proposal, cancel of terminal runs, compact of missing runs — all must be non-corrupting; exact outcomes undefined.
4. **Observation redelivery** (at-least-once): proposal/approval resolution state must survive redelivery; required by retry-safety, untested.
5. **Dangling refs render without existence checks** (objects, slices, turns): deliberate or risk — confirm.
6. **Fork edge cases**: fork from nonexistent parent; fork-binding never becoming durable (no timeout policy); child space id collision.
7. **`:turn/steer` semantics**: type exists and validates; run-side behavior unspecified.
8. **Run success path**: no completed/succeeded state exercised in Space's contract; the full lifecycle needs it.
9. **Event re-mint on sends into existing spaces**: whether a space event appears in the event list when the space already existed is unpinned (only the implicit-creation list is pinned).
10. **Space lifecycle beyond `:active`**: no archive/close/delete states exist; deletion is out of scope (kernel convention: never delete data the read contract depends on).
11. **Catalog coverage of overlays** and turn→material relation edges beyond raw→slice: unexercised shapes.
12. **Cost rollup zero-state** (absent vs zeroed totals before first usage observation) is unspecified.
