# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

> **RETROSPECTIVE MODE.** The LLM Kernel was built before this skill existed. This artifact
> retroactively captures the requirements analysis the module never had. Sources: the four
> llm-track docs (`llm-track-canonical.md`, `llm-track-v2.md`, `llm-track-derived-contract.md`,
> `llm-track-slice-roadmap.md`), the module identity header, and the public contract as
> exercised by `test/app/server/rama/dogfood_llm_test.clj`. The source files under `src/` were
> NOT read. Where the spec docs name storage-level design (depot/PState names, partitioning,
> topology shapes), this artifact extracts the **requirement that motivated the design** and
> records that instead. Where a behavior is inferred rather than test-evidenced, it is marked
> **[INFERRED]**; genuinely unresolved points are collected under "Ambiguities" at the end.

Before designing any Rama-specific implementation, write out the implicit assumptions and expectations that a senior engineer would bring to this system based on the requirements and the domain.

---

## System Summary (requirements altitude)

The LLM Kernel runs provider-side turn-runs for chats that live in Space. Its protocol:

```
requested model work  →  observable agent-run state

  1. A TURN-RUN REQUEST arrives (intent): "execute this frozen context bundle as one
     model turn, as run R on LLM-side thread T, for space S / turn WT."
  2. An EXECUTOR (which lives OUTSIDE this module) discovers the pending run, CLAIMS it,
     waits for a durable grant, loads the bundle content by id, spawns the provider
     process, and STREAMS every provider event back as a sequenced OBSERVATION.
  3. CONTROLS (cancel / steer / approve / compact) intervene in a run mid-stream.
  4. All state is a deterministic fold of the appended history; every fact is queryable
     by run, thread, turn, item, approval, and space identities.
```

**Entities** (protocol-level):

| Entity | Identity | What it is |
|---|---|---|
| LLMTurnRun | `llm-turn-run/id` | one model execution of one frozen context bundle |
| LLMThread | `llm-thread/id` | LLM-side conversation; owns an ordered list of runs; bound to one space; carries the native provider thread/session id once known |
| Space binding | `space/id → llm-thread/id` | which LLM thread currently serves a space |
| Raw LLM item | `llm-item/id` | immutable provider output (message / reasoning / tool call / tool result) |
| Tool call | `tool-call/id` | exec / patch / MCP / web tool invocation trace |
| Approval | `approval/id` | a blocked provider-side permission request, with the native JSON-RPC request id that must answer it |
| Token usage | per run | input-total, cached-input, output, reasoning-output, context window, subscription messages used, billing mode |
| Cost rollup | per thread | aggregate of per-run usage, without double counting |
| Decision trace | `request/id` / run id | durable accept/reject answer for each request |
| Control record | `control/id` | durable record of cancel / steer / approval-resolve / compact / stale-marking |
| Pending-work inbox entry | per executor lane | run ids awaiting an executor claim |

**Run lifecycle states** (evidenced by tests + spec):

```
(does not exist)
   → :pending                  request accepted; awaiting executor claim
   → :claimed                  exactly one executor holds the lock
   → (streaming)               observations folding; still :claimed unless blocked
   → :blocked-awaiting-approval  provider asked permission; turn is blocked
   → :succeeded                provider finished the turn          (terminal)
   → :failed                   error / stale approval / policy     (terminal)
   → :cancel-requested → :cancelled   user-initiated interrupt      (terminal)
```

**Cross-cutting requirements** (apply to every operation below):

1. **Deterministic fold.** The same appended history must rebuild identical state — two
   independent kernel instances fed identical records produce identical full snapshots
   (writer-asymmetry property test). Consequences: no wall-clock reads during folding
   (decision timestamps derive from the request's own `time-ms`), no randomness, no
   ambient I/O in the fold path.
2. **Pure record construction.** Building a request/claim/observation/control record must
   not mutate any state. Only the append does.
3. **External writers append intent; only the kernel materializes state.** The executor
   reads state and appends records; it never mutates queryable state directly
   (back-arrow rule).
4. **Per-run total order.** Every record about a run carries the run identity as its
   ordering scope (the request envelope's routing key is observable contract:
   `[:llm-run <run-id>]`). All state transitions for one run must be serialized; no
   cross-run coordination is required for correctness.
5. **Never drop observations.** Out-of-order observations are buffered and drained, never
   discarded. Correctness beats losing deltas.
6. **Never delete the trail.** Items, tool calls, usage, approvals, decisions, and control
   records remain queryable after the run reaches a terminal state — replay/time-travel
   and post-hoc citation are product requirements. Compaction of the provider-side
   context must NOT erase the kernel-side trail.
7. **Secret hygiene.** No credential material is ever durably stored: provider stream
   lines are redacted before storage (e.g. an `api_key` field in a stream event must not
   appear in any stored event or raw item); spawn environments strip auth-bearing
   variables; API keys enter only via secret references resolved at spawn time.
8. **Provider-agnostic surface.** Multiple backends (Codex default; Claude
   scalar-discriminated; future agents) must fold into the SAME generic run surface —
   same items, tool calls, usage, views — with the backend and its native session/thread
   id recorded on the run/thread.
9. **Honest cost accounting.** Cached input is tracked separately from total input;
   subscription quota separately from token counts. Never store "inherited context is
   free" anywhere.

---

## Operations

### 1. Submit turn-run request (intent: "run this bundle as one model turn")

Carries: space id, world-turn id, context-bundle id (reference only), run id, thread id,
request id, request time, executor inbox key, backend (`:codex` default when absent,
`:claude` explicit), auth mode (`:subscription`, `:api-key`; `:passive-observe` invalid
for active runs), optional run-restart policy (e.g. `:fail-on-stale-approval`), optional
fork-from-native-thread reference.

- **Latency**: Human-initiated send (or an automated trigger). Acceptance + visibility of
  the `:pending` run within a few hundred ms is acceptable; the UI awaits the decision to
  confirm the send. Not single-digit-ms critical.
- **Throughput**: Driven by user sends, follow-ups, forks, and reconciliations. Human-paced:
  per-user per-minute scale. Fork fan-out can burst ~10 sibling requests at once. Low
  volume relative to observations.
- **Consistency/correctness invariants**:
  - Exactly one frozen context bundle per run; the request carries ONLY the bundle
    reference plus scheduling hints. Execution options that affect what the provider sees
    or may do (model, approval policy, sandbox, cwd) are bundle-owned — a request carrying
    them in its payload is a validation error (`:payload/execution-options-not-bundle-owned`).
  - The bundle exists (was frozen) before the kernel receives the run request; the kernel
    never composes or mutates bundles. (World-first: the user's send already became true
    upstream; this kernel's decision answers "can this executor track run this bundle
    now?", not "is the turn true?".)
  - A request without a backend normalizes to `:codex` (legacy compatibility), including
    the executor agent-kind; `:claude` requests carry their auth mode and default to the
    Claude executor pool. `:passive-observe` auth mode is rejected for active runs.
  - The accepted decision is durable and enumerates produced event ids; its routing key
    equals the request's; `decided-at` derives from the request's `time-ms` (replay
    determinism).
  - Acceptance atomically makes ALL of the following visible together: run row `:pending`,
    pending-inbox entry, turn→run mapping, run appended to the thread's ordered run list,
    space→thread binding (created on first run for that space).
  - Follow-up semantics: a new run on an already-bound thread is a NEW run id on the SAME
    thread — never a second turn appended to an old run. If the thread already carries a
    native provider thread id, the new run inherits it at acceptance so the executor needs
    no extra lookup.
  - Idempotency: replaying the same request id / idempotency key returns the same decision
    shape and must not mint a duplicate run, inbox entry, or thread entry.
- **Data growth and scale**: Runs accumulate forever (trail retention); a thread's run list
  grows unbounded over time — ordered access by thread dominates (render a conversation),
  point lookup by run id and by world-turn id must be cheap. Threads per space: one
  active thread per space per agent kind (working default; multi-lane model comparison is
  a named future).
- **Concurrency behavior**: Two requests for the same space arriving close together must
  both land on the same bound thread in arrival order. Duplicate request replay must be
  idempotent (no double-mint). A request racing the observation that records the native
  thread id: the second run must still end up correctly bound **[INFERRED — the test
  awaits the binding before the follow-up send; at minimum the kernel must never bind a
  run to a stale/absent native id silently when one is required]**.
- **Edge cases**: missing backend (→ codex); invalid auth mode for active work
  (→ rejected); execution options smuggled in payload (→ validation error); request for a
  space whose thread is bound but native id not yet known (run is `:pending` with no
  native id; executor handles first-turn provider-thread creation); duplicate request id
  (→ same decision, no duplicates); rejected requests remain queryable as decisions and
  produce no run.

### 2. Claim a pending run (executor lock)

Executor appends a claim: run id, thread id, executor id, claim token, claimed-at,
executor inbox key.

- **Latency**: Hundreds of ms acceptable; this sits on the time-to-first-token path, so
  the grant should be visible sub-second after the run is pending. The executor must be
  able to await the durable grant ("3-state await": pending / granted-to-us /
  granted-to-other) before doing anything irreversible.
- **Throughput**: Exactly one successful claim per run; contention only when multiple
  executors poll the same inbox. Low volume.
- **Consistency/correctness invariants**:
  - **Grant-before-execute.** A claim is granted only if the run is still `:pending`.
    Exactly one executor wins; the grant is durable; the executor must verify
    `granted-to-us` (its own token) before spawning the provider process
    (`:spawned-after-grant?` is asserted contract).
  - Granting removes the run from the pending inbox atomically with the status change to
    `:claimed`.
  - A claim must never regress a run that is already claimed, blocked, terminal, or
    nonexistent, and must never invent a run.
- **Data growth and scale**: One claim per run; negligible.
- **Concurrency behavior**: Two executors claim concurrently → first processed wins;
  the loser observes `granted-to-other` and must not spawn. Claim racing a cancel: if
  cancel lands first the claim should not be granted (run no longer plain-pending)
  **[INFERRED — see Ambiguity #3]**.
- **Edge cases**: claim for unknown run id (no-op, no state invented); duplicate claim
  replay by the winner (idempotent — still granted-to-us, no double-removal effects);
  claim after run already terminal (not granted).

### 3. Append streamed observation (provider output, token-attributed)

Executor appends every provider event as an observation: run id, thread id, observation
type, per-run sequence number, observation id, received-at, type-specific payload, raw
provider JSON. Observed types: item-completed, token-usage, tool-call, approval-request,
run-finished; the native provider thread/session id may ride on any observation
(fork-completed / first-turn discovery).

- **Latency**: This is the streaming path — the product is watching reasoning arrive live.
  Fold-to-queryable should be tens of ms per event; end-to-end provider-token →
  queryable-view under ~200ms to feel like streaming. Highest-frequency, latency-sensitive
  write in the module.
- **Throughput**: DOMINANT volume. Provider deltas arrive at 10–100 events/sec per active
  run; concurrent runs (fork fan-out ×10) multiply this. Hundreds to thousands of
  observations per run. The fold must keep up with sustained streaming without starving
  reads.
- **Consistency/correctness invariants**:
  - **Sequence ordering.** Observations fold strictly in per-run sequence order. An
    observation arriving ahead of a gap is BUFFERED (visible as buffered; the
    folded-progress marker `last-seq` unchanged; no items materialized from it) and
    drained in order once the gap fills; after draining, the buffer is empty and folded
    progress reflects the highest contiguous sequence. Never dropped.
  - **Item materialization.** item-completed produces an immutable raw item, queryable by
    run (ordered), by thread (full history index), and by item id (with back-pointer to
    its run); the raw provider payload is retained for replay; the item records its
    source backend.
  - **Token attribution.** token-usage observations set per-run usage with input-total,
    cached-input, output, reasoning-output, context window, subscription messages used,
    and billing mode — all as separate fields. Usage re-reports for the same run apply as
    a delta/replace: thread-level cost rollups must not double-count a run whose usage is
    reported more than once.
  - **Tool calls** are indexed per run with id, type, name, status.
  - **Approval requests** create a durable pending-approval row holding the native
    JSON-RPC request id (the blocked state must be durable and visible to the UI), append
    to the run's approval trail, and move the run to `:blocked-awaiting-approval`.
  - **run-finished** closes the run `:succeeded`; the live view reflects it.
  - **Native thread discovery.** The first observation carrying the provider's native
    thread/session id records it on the thread (Claude's session id additionally on the
    run); follow-up runs depend on this binding. For forks: the executor must not start
    the first turn on a new native thread until the fork-completed observation is durably
    recorded and the binding is visible — otherwise an executor crash between fork and
    first turn loses the native-thread mapping.
  - **Redaction before storage.** Credential-bearing fields in provider stream lines are
    stripped before the observation is built; stored events and raw items must never
    contain them.
  - Observations on a run that is `:pending` (not yet claimed) still fold (evidenced:
    an approval observation on an unclaimed run materializes the pending approval).
- **Data growth and scale**: Unbounded append per run; per-thread item history unbounded.
  Dominant access patterns: ordered range scan of a run's items (render the trail),
  ordered scan of a thread's items (full history), point lookup by item id. The live
  per-run view is required to stay BOUNDED (spec: "bounded UI stream view") even though
  the underlying trail is unbounded — see Ambiguity #6.
- **Concurrency behavior**: One executor streams one run, so per-run writes are
  single-writer in practice — but retries/redelivery mean the fold must tolerate the same
  observation arriving twice (the same sequence number must not double-fold: no duplicate
  items, no double-counted usage) **[INFERRED — required by replay determinism plus
  at-least-once delivery; not directly test-evidenced]**. Observations for different runs
  are independent and must not contend.
- **Edge cases**: a gap that never fills (run stalls with buffered observations and never
  closes — needs surfacing, see Ambiguity #2); run-finished with unresolved pending
  approvals (Ambiguity #5); observation after terminal state (Ambiguity #2); usage event
  with missing fields (fold what is present); empty turn (run-finished with zero items →
  run `:succeeded` with an empty item list — views/rollups must handle empties);
  non-UTF-8/base64 payload chunks (stored as-is in the raw payload).

### 4. Submit control (cancel / steer / approval-resolve / compact)

Controls arrive as durable records targeting a run (or its thread), each queryable by
control id afterward. User-initiated controls enter through the world side first; this
kernel receives the derived control record — the UI never writes controls directly into
this kernel's intake.

- **Latency**: Human intervention on a live run. The control record must be durable and
  the executor must observe it within ~1s (cancel responsiveness while tokens stream;
  approval unblocks a frozen provider turn). The pending-approval row must appear promptly
  when the provider blocks — a silent blocked turn looks like a hang.
- **Throughput**: Rare; human-paced. A handful per run at most.
- **Consistency/correctness invariants**:
  - **Cancel** is two-phase: a durable `:cancel-requested` marker first; the executor
    reads it and interrupts the provider; the provider's abort confirmation arrives as an
    observation that closes the run `:cancelled`. Truth transitions only on confirmation.
  - **Approval-resolve** carries the durable approval id AND the native JSON-RPC request
    id (copied from the pending row so the executor never infers the mapping). Resolution
    clears the pending row, records the outcome on the run's approval trail, and unblocks
    the run. Approving means "the agent may attempt the tool action" — it does NOT mean
    the resulting artifact is accepted as truth (that acceptance lives outside this
    kernel).
  - **Approval staleness**: a pending approval's native request id is only answerable
    while the original provider process is alive. If the executor/provider dies before
    resolution, the approval must be marked stale/expired — a new process must never
    pretend to answer the old request id.
  - **Approval timeout**: unresolved approvals are durably declined/expired after a
    wall-clock timeout policy (the provider waits forever; the kernel side must not).
  - **Steer** injects mid-turn input into an in-flight run; meaningful only while the run
    is executing **[INFERRED — spec'd control type, not test-evidenced; effect at each
    state is Ambiguity #7]**.
  - **Compact** requests provider-side context compaction on the thread; a compaction
    marker is recorded; the kernel-side trail retains everything pre-compaction (future
    bundles may cite pre-compaction items even though the provider only sees the summary).
  - Terminal states are sticky: a control landing on a `:succeeded`/`:failed`/`:cancelled`
    run is recorded but must not regress the status **[INFERRED]**.
- **Data growth and scale**: Negligible volume; control records retained forever as trail.
- **Concurrency behavior**: Cancel racing run-finished → whichever folds first wins the
  terminal state; the loser is recorded without regressing it **[INFERRED]**. Two
  resolutions of the same approval → the first wins; the second is a no-op on the pending
  row.
- **Edge cases**: cancel on a `:pending` (never-claimed) run — no executor exists to
  confirm the interrupt (Ambiguity #3); approval-resolve for an already-expired approval
  (no resurrection; recorded); approval-resolve for an unknown approval id (no-op, no
  state invented); compact on a thread with no native binding yet.

### 5. Mark stale approvals (executor-death protocol)

When the executor detects its provider child died (or it is restarting) with unresolved
approvals on a run, it invokes the stale-marking protocol with run id, executor id, time.

- **Latency**: Seconds acceptable — crash cleanup, not a hot path. What matters is that it
  happens reliably, or blocked runs leak forever.
- **Throughput**: Rare (crash/restart events only).
- **Consistency/correctness invariants**:
  - Every unresolved approval on the run is marked `:expired`, its pending row is cleared,
    and a durable control record explains why (`:executor-stale`), queryable by a derived
    control id tied to the run + approval.
  - The run then follows its RECORDED restart policy (captured at request time, echoed on
    the run row): the MVP policy `:fail-on-stale-approval` closes the run `:failed` with a
    structured error (reason: approval declined; decision: expired). The action taken,
    the policy, and the stale approval ids are reported back to the caller.
  - The approval's full history (now `:expired`) remains on the run's approval trail.
- **Data growth and scale**: Negligible.
- **Concurrency behavior**: Stale-marking racing a genuine resolution of the same approval
  → exactly one outcome wins per approval **[INFERRED]**; stale-marking is idempotent
  (re-invocation finds nothing unresolved, returns empty, does not double-fail the run).
- **Edge cases**: run with zero unresolved approvals (no-op, empty stale list); run
  already terminal (approvals still cleaned up, status not regressed) **[INFERRED]**.

### 6. Executor pull protocol (obligations of the out-of-module executor)

The executor lives OUTSIDE this module as helper/runtime fns — distinct from the compute
kernel, which owns its executor. The kernel must expose enough state for an external
executor to be correct, and the executor must honor this protocol:

1. Discover pending runs via its inbox key (read the pending inbox).
2. Claim; await the durable grant; verify `granted-to-us` (its own token).
3. Load the context bundle CONTENT by id through an external loader — the kernel hands
   out only the reference; bundle-owned execution options come from the bundle.
4. Spawn the provider only after the grant (`:spawned-after-grant?`), with the
   backend-appropriate invocation:
   - **Claude, subscription mode**: streaming JSON in/out with partial messages, hook
     events, and user-message replay enabled; NO bare mode; child env stripped of ALL
     auth-bearing variables (OAuth token, API keys) — subscription auth comes from the
     CLI's own login state; non-auth env passes through.
   - **Claude, api-key mode**: bare mode; inject exactly the API key resolved from a
     secret reference; all other auth-bearing variables stripped.
   - **Codex**: long-lived JSON-RPC child (initialize → thread start/resume/fork →
     turn start); auth-flipping env stripped (per canonical doc).
5. Stream EVERY provider event back as a sequenced observation (redacting credentials);
   record native thread/session ids; for forks, append fork-completed durably and see the
   binding before starting the first turn on the new native thread.
6. Report per-run results: claim state, whether spawned, observations appended.
7. On crash/death with unresolved approvals: run the stale-approval protocol.

- **Latency**: pending-discovery cadence bounds idle-to-execute lag; sub-second polling
  acceptable.
- **Invariants**: the executor never mutates kernel state directly; everything it learns
  enters as appended records (back-arrow rule).
- **Edge cases**: executor restart mid-run (claim already granted-to-us → resume or fail
  per recorded policy); two executor instances sharing an inbox key (claims arbitrate).

### 7. Reads (the queryable surface)

All reads are interactive UI/executor queries; point lookups should answer in low tens of
ms; ALL must be safe to call in EVERY entity state (absent ids return nil/empty, never
errors).

| Read | By | Returns |
|---|---|---|
| decision | run/request id | accept/reject, event ids, routing key, decided-at |
| run | run id | status, backend, auth mode, native ids, restart policy, folded progress (`last-seq`), buffered sequences, structured error |
| live view | run id | bounded stream view: status, ordered items, token usage, folded progress |
| run-detail projection | run id | self-describing derived view (declares its type and the canonical material it derives from) |
| thread | thread id | ordered run ids, native provider thread id |
| thread binding | space id | bound thread id |
| runs-by-thread | thread id | the thread's runs |
| run-for-turn | world-turn id | the run minted for that turn |
| pending inbox | executor lane | claimable run ids |
| items by run / by thread / by id | resp. ids | immutable raw items |
| raw response items | run id | literal provider payloads (replay primitive) |
| tool calls | run id | tool-call trace |
| approvals | run id / approval id | approval trail / pending approval (with native request id) |
| token usage | run id | the separate-field usage record |
| cost rollup | thread id | totals + per-run breakdown + run count, self-describing |
| control | control id | durable control / stale records |
| claim state | run row + claim | pending / granted-to-us / granted-to-other (pure function) |

Derived views must be rebuildable from canonical state and must declare what they derive
from. The thread cost rollup is repairable by rebuilding from canonical per-run usage —
it is derived, never source truth.

---

## Entity State × Write Matrix

Reads abbreviated: **run** (run row), **view** (live view), **detail** (run-detail
projection), **dec** (decision), **inbox** (pending inbox), **r4t** (run-for-turn),
**rbt** (runs-by-thread), **items** (items by run/thread/id), **raw** (raw response
items), **tools** (tool-calls by run), **appr** (approvals by run), **pend-appr**
(pending approval by id), **usage** (token usage), **cost** (thread cost rollup),
**thr** (thread row), **bind** (space→thread binding), **ctl** (control by id),
**claim?** (claim-state fn).

### Entity A: LLMTurnRun

States: does-not-exist · pending · claimed (incl. streaming) · blocked-awaiting-approval ·
succeeded · failed · cancel-requested · cancelled.

**Write: turn-run request**
- × does-not-exist (valid request):
  - dec: accepted; event ids derived from the request id; routing key echoed; decided-at = request time-ms.
  - run: `:pending`; backend / auth mode / restart policy recorded; native thread id present iff the thread is already bound (follow-up).
  - view: exists for the run; no items, no usage; folded progress at the initial sentinel.
  - detail: derivable, empty-trail equivalent.
  - inbox: contains the run id (executor can discover it).
  - r4t: turn id → this run id. rbt/thr: run id appended in order. bind: space → thread (created if first run for the space).
  - items/raw/tools/appr: empty — no provider work yet. pend-appr: nil. usage: nil. cost: unchanged (no usage yet). ctl: unchanged. claim?: pending (no claim yet).
- × does-not-exist (invalid: execution options in payload / passive-observe / bad shape):
  - dec: rejected, queryable, no event ids **[spec invariant I16; the validation surface is test-evidenced as a pure pre-check, the kernel-side rejection row is inferred — Ambiguity #1]**.
  - run/view/detail: absent (nil). inbox: no entry. r4t/rbt/thr/bind: unchanged. items/raw/tools/appr/pend-appr/usage/cost/ctl: unchanged.
- × pending / claimed / blocked / terminal (duplicate or replayed request id):
  - dec: the SAME decision returned (idempotency). run: status unchanged. inbox: no duplicate entry. rbt/thr: no duplicate run id. r4t: unchanged. view/detail/items/raw/tools/appr/pend-appr/usage/cost/ctl: unchanged.

**Write: claim**
- × pending:
  - run: `:claimed`; claim recorded (token, executor id, claimed-at). view/detail: status reflects claimed.
  - inbox: run id REMOVED, atomically with the grant. claim?: granted-to-us for the winning token.
  - dec/r4t/rbt/thr/bind: unchanged. items/raw/tools/appr/pend-appr/usage/cost/ctl: unchanged.
- × claimed (second executor):
  - run: unchanged — the first claim stands. claim?: granted-to-other for the loser → loser must not spawn.
  - inbox: already empty for this run. dec/view/detail/r4t/rbt/thr/bind/items/raw/tools/appr/pend-appr/usage/cost/ctl: unchanged.
- × claimed (same executor, replay): idempotent — claim?: still granted-to-us; every other read unchanged.
- × blocked / succeeded / failed / cancel-requested / cancelled:
  - run: unchanged — no grant, no regression. inbox: unchanged. claim?: not granted-to-us. All other reads: unchanged.
- × does-not-exist:
  - run: still nil — a claim never invents a run. inbox and all other reads: unchanged.

**Write: observation — item-completed (in-sequence)**
- × pending or claimed:
  - items: the item appears, ordered, with content, source backend, and run back-pointer (by run, by thread, and by item id). raw: provider payload retained.
  - run: folded progress (`last-seq`) advances; no buffer entry for this seq. view/detail: item appears in the ordered list.
  - thr: native thread id recorded if the observation carries it (and the Claude session id on the run row).
  - dec/inbox/r4t/rbt/bind: unchanged. tools/appr/pend-appr/usage/cost/ctl: unchanged.
- × blocked-awaiting-approval: same as above — the trail keeps folding around the blocked marker **[INFERRED for items arriving while blocked]**.
- × succeeded / failed / cancelled: terminal status sticky in every read (run/view/detail); the observation is durably retained; whether items still materialize is Ambiguity #2.
- × does-not-exist: no run state invented; the observation must not be silently lost **[INFERRED — Ambiguity #2]**.

**Write: observation — item-completed (out-of-order, gap before it)**
- × claimed:
  - run: the observation is visible in the BUFFER keyed by its sequence; folded progress UNCHANGED (initial sentinel if nothing folded yet). items: NOT yet materialized (empty if nothing folded). view/detail: items unchanged.
  - dec/inbox/r4t/rbt/thr/bind/raw/tools/appr/pend-appr/usage/cost/ctl: unchanged.
  - …then when the gap-filling observation arrives: the buffer drains in order; items: all drained items appear in sequence order; run: buffer empty, folded progress = highest contiguous sequence; view: complete ordered items.

**Write: observation — token-usage**
- × pending / claimed / blocked:
  - usage: per-run record set with separate fields (input-total, cached-input, output, reasoning-output, context window, messages-used, billing mode).
  - cost: thread rollup totals updated by per-run DELTA (no double count on re-report); per-run breakdown updated; run-count counts this run exactly once.
  - view/detail: token-usage section reflects it. run: folded progress advances.
  - items/raw/tools/appr/pend-appr/dec/inbox/r4t/rbt/thr/bind/ctl: unchanged.
- × same run, second usage report: usage: replaced/updated; cost: totals move by the delta only — cross-run sums stay correct; run-count unchanged.
- × terminal: Ambiguity #2 (status sticky; trail retention of late usage).

**Write: observation — tool-call**
- × pending / claimed / blocked: tools: entry with id/type/name/status. run: folded progress advances. view/detail: status unchanged. items/raw/appr/pend-appr/usage/cost/dec/inbox/r4t/rbt/thr/bind/ctl: unchanged.

**Write: observation — approval-request**
- × pending or claimed:
  - pend-appr: durable row `:pending` with the native JSON-RPC request id and run back-pointer.
  - appr: approval id appears on the run's trail. run: `:blocked-awaiting-approval`. view/detail: status blocked.
  - inbox: unchanged (already consumed by a claim; evidenced that the approval also folds on a still-unclaimed `:pending` run).
  - items/raw/tools/usage/cost/dec/r4t/rbt/thr/bind/ctl: unchanged.
- × blocked (second approval): a second pending row + trail entry; run stays blocked.
- × terminal: Ambiguity #2.

**Write: observation — run-finished**
- × claimed (streaming complete):
  - run: `:succeeded` (terminal). view/detail: `:succeeded`, full ordered items, final usage.
  - items/raw/tools/appr/usage: all retained, queryable forever. cost: final totals stand.
  - inbox: no entry. dec/r4t/rbt/thr/bind/pend-appr/ctl: unchanged.
- × blocked with unresolved approvals: Ambiguity #5 (disposition of the pending row at close).
- × pending (finished without a claim — direct/fake streams): folds; run `:succeeded` (test flows stream without claims in places).
- × terminal: sticky; no double-close, no read regresses.

**Write: control — cancel**
- × claimed / blocked:
  - run: `:cancel-requested` marker (truth still pending provider confirmation). ctl: durable record.
  - Executor reads the marker → interrupts → abort-confirmation observation → run: `:cancelled`; view/detail: cancelled; items/usage up to the abort retained.
  - inbox/dec/r4t/rbt/thr/bind/pend-appr: unchanged.
- × pending (never claimed): Ambiguity #3 — no executor exists to confirm; the kernel needs a close-out path and the run must not remain claimable.
- × succeeded / failed / cancelled: ctl recorded; run status unchanged (sticky) **[INFERRED]**.
- × does-not-exist: no state invented (Ambiguity #3 covers whether the control is recorded or rejected).

**Write: control — approval-resolve (approved)**
- × blocked with a pending approval:
  - pend-appr: cleared (nil). appr: outcome recorded on the trail. ctl: durable record carrying the native request id.
  - run: unblocked, resumes executing. view/detail: unblocked.
  - The executor replies to the provider using the native request id carried on the control record (never inferred from UI state).
- × blocked, approval already expired: pend-appr: already nil — no resurrection; appr: stays `:expired`; ctl: records the late attempt; run: unchanged.
- × unknown approval id: no-op on all rows; nothing invented.

**Write: control — approval-resolve (declined)**: same read surface; the provider is
informed of the decline; the turn continues or fails per provider behavior; appr records
the decline.

**Write: control — steer**
- × claimed (in-flight): ctl: durable; the injected input reaches the provider mid-turn; subsequent provider events appear in items as usual **[INFERRED — Ambiguity #7]**.
- × any other state: recorded; no effect on a non-executing run **[INFERRED]**.

**Write: control — compact**
- × any active thread state: ctl: durable; provider compaction requested; a compaction marker is recorded on the thread/run; items/raw/usage/appr from before compaction remain fully queryable (the kernel trail outlives the provider's context window).

**Write: stale-approval marking (executor death)**
- × blocked with unresolved approval(s):
  - pend-appr: nil (cleared — awaiting reads observe removal). appr: approval(s) `:expired` on the trail. ctl: stale record with reason `:executor-stale`, queryable by the derived control id.
  - run: follows the recorded restart policy — `:fail-on-stale-approval` → `:failed` with structured error (reason: approval declined; decision: expired); the policy is echoed on the run row. The protocol returns the stale approval ids, the action (`:failed`), and the policy.
  - view/detail: `:failed`. items/raw/tools/usage/cost: retained. inbox/dec/r4t/rbt/thr/bind: unchanged.
- × run with no unresolved approvals: empty stale list; no status change; idempotent.
- × terminal run: approvals cleaned; status not regressed **[INFERRED]**.

### Entity B: LLMThread (+ space binding)

States: does-not-exist · bound (created, bound to a space, no native id) · native-bound
(provider thread/session id recorded).

**Write: turn-run request (first for a space)**
- × does-not-exist: thr: created with ordered run list `[run-1]`; bind: space → thread; rbt: `[run-1]`; items-by-thread: empty; cost: absent/zero; run/dec/inbox per Entity A.
**Write: turn-run request (follow-up)**
- × bound: thr: run list grows in order; bind: unchanged; the new run carries NO native id (none known yet); r4t maps the new turn to the new run.
- × native-bound: thr: run list grows; the NEW RUN INHERITS the native thread id at acceptance (run row exposes it; the executor's run context receives it) — never a second turn appended to an old run; items for each run stay partitioned per run id.
**Write: observation carrying a native thread id**
- × bound: thr: native id recorded (→ native-bound); subsequent runs inherit it. Claude: session id additionally recorded on the run row. bind/rbt: unchanged.
- × native-bound: the same id re-asserted → no change; a CONFLICTING id must not silently flip the binding **[INFERRED — Ambiguity #8]**.
**Write: fork-lineage request (fork-from-native-thread reference)**
- × parent native-bound: the child is a NEW thread identity; parent thr/bind/rbt unchanged (plurality preserved); the child binds its native id only after the fork-completed observation is durable; the executor must not start the child's first turn before that binding is visible (crash between fork and first turn must not lose the mapping).
**Reads in every state**: thr (nil → row), bind (nil → thread id), rbt (empty → ordered runs), items-by-thread (empty → full history), cost (absent → rollup). All safe when absent.

### Entity C: Approval

States: does-not-exist · pending · resolved-approved · resolved-declined · expired.

- **approval-request observation × does-not-exist**: pend-appr: `:pending` row with native JSON-RPC request id + run id; appr: on the run's trail; run: blocked; ctl: unchanged.
- **approval-request × pending (duplicate id replay)**: no duplicate row; idempotent **[INFERRED]**.
- **approval-resolve × pending**: pend-appr: nil; appr: outcome on the trail; run: unblocked; the native id is used exactly once, by the live process.
- **approval-resolve × expired**: no resurrection — pend-appr stays nil; appr keeps `:expired`; ctl records the late attempt.
- **approval-resolve × resolved**: first resolution wins; second is a no-op on every row.
- **approval-resolve × does-not-exist**: no-op; nothing invented.
- **stale-marking × pending**: appr: `:expired`; pend-appr: nil; ctl: reason `:executor-stale`; run follows its restart policy. A NEW provider process must never answer the OLD native request id.
- **stale-marking × already resolved/expired**: no-op for that approval.

### Entity D: Pending-work inbox entry (per executor lane)

States: absent · present.

- **turn-run request accepted × absent**: present — the run is discoverable by its lane key. inbox read: contains the run id; reading an empty/unknown lane returns empty, never errors.
- **turn-run request replay × present**: still exactly one entry.
- **claim granted × present**: absent — removed atomically with the grant. inbox read: no longer contains the run id; run read: `:claimed`.
- **claim not granted × present/absent**: unchanged.
- **terminal transitions**: the entry must already be absent for claimed runs; a run that reaches a terminal state without ever being claimed must not remain claimable **[INFERRED — Ambiguity #3]**.

### Entity E: Thread cost rollup

States: absent · populated(n runs).

- **token-usage observation, new run × absent/populated**: totals += the run's usage; per-run breakdown gains the run; run-count += 1. cost read: self-describing rollup with totals (input-total, cached-input, output, reasoning-output summed across runs), per-run usage, and run count.
- **token-usage observation, same run again × populated**: totals adjusted by DELTA only (no double count); run-count unchanged; the per-run entry updated. usage read: latest per-run record.
- **any non-usage write**: rollup unchanged.
- **corruption/doubt**: repair rule — rebuild from canonical per-run usage; the rollup is derived, never source truth.

### Entity F: Request/decision trace

States: absent · recorded(accepted) · recorded(rejected).

- **turn-run request × absent**: recorded; dec returns status, event ids (accepted) or reason + empty event ids (rejected); decided-at deterministic from the request's own time.
- **any replay × recorded**: the same decision returned; never two decisions for one request id.
- **all later writes**: the decision is immutable and queryable forever, in every run state.

---

## Ambiguities flagged (for the next phase to resolve, not silently default)

1. **Kernel-side rejection rows**: validation is test-evidenced as a pure pre-append check;
   whether an appended-but-invalid request mints a durable rejected decision inside THIS
   kernel (vs being filtered upstream) is asserted by the contract doc (invariant I16) but
   not by the module's tests.
2. **Observations after terminal status / for unknown runs**: "never drop" vs "terminal is
   sticky" — the disposition of late or orphaned observations (durably retained? folded
   into the trail without a status change?) is unspecified; likewise the surfacing of a
   permanently unfilled sequence gap (a run that never closes).
3. **Cancel of a never-claimed run**: no executor exists to confirm the interrupt; the
   kernel needs a close-out path and must ensure the run is no longer claimable.
4. **Duplicate-delivery idempotency of observations** (same sequence twice): required by
   determinism plus at-least-once delivery, but not directly test-evidenced.
5. **run-finished with unresolved approvals**: the pending approval row's disposition at a
   successful close is unspecified.
6. **View boundedness**: the spec requires a bounded live stream view; tests exercise only
   small runs — the bounding policy (window size, truncation marker) is undefined.
7. **Steer semantics per state**: a spec'd control type with no test evidence for its
   effect in any lifecycle state.
8. **Conflicting native thread ids** on later observations for an already-bound thread:
   no defined behavior.
9. **Where send-idempotency lives**: the roadmap places duplicate-send dedup at the
   world-first bridge; the LLM kernel's own duplicate-request behavior (replayed run
   request) is required to be non-duplicating, but its decision-replay shape is only
   contract-doc-evidenced.
10. **Thread cardinality**: one active LLM thread per space per agent kind is a working
    default (open Sid-level decision — model-comparison lanes would relax it).
11. **`:passive-observe` auth mode**: rejected for active runs; its legitimate consumer
    (passive transcript observation) lies outside this module's evidenced surface.
