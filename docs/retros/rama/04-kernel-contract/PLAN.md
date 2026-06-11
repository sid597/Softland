# Plan

<!-- Phase 1 Step 5. Fill in after completing Steps 1-4. After completing this, fill in PLAN_VALIDATION.md. -->
<!-- RETROSPECTIVE mode. Re-derived from IMPLICIT_SPEC.md only (code-blind).
     Module: TextKernelModule (the V0/V1 text-artifact kernel instance).
     Layer (b) — the shared contract artifacts — is designed in the
     "Contract Layer Artifacts" and "C1–C19 Enforcement Map" sections below. -->

## Architecture summary (orientation for the sections below)

One client-appendable **intent depot** partitioned by `:routing/key`. One
**microbatch topology** (`"kernel"`) consumes it and, **without any partitioner
hop**, performs on the routing key's task: verbatim request storage → dedup
guard → envelope validation → type dispatch → pure interpret (accept/reject) →
decision write → event write → pure per-event-type materialization → projection
update. Because the topology body contains zero partitioners, every state
change caused by one request commits atomically on one task — this is the
structural enforcement of C4's per-key event-boundary atomicity. Reads are
foreign selects routed by the same key (`:pkey` = routing key), plus one
gather-style query topology for the branch-wide status read.

The center loop, mapped to Rama:

```
ActionRequest --foreign-append! :append-ack--> *text-intent-depot (hash-by :routing/key)
  --microbatch "kernel"--> [store verbatim | validate | interpret | decide]
  --accepted--> KernelEvent row + materializations + projections   (same task, same event)
  --rejected--> Decision row only                                   (no world state)
UI / tests --foreign-select / query topology / await-* polling--> PStates
```

## Reads

All reads are callable at any time in any entity state; nonexistent ids return
nil/empty, never throw (`keypath` on absent key navigates to nil). All reads
are eventually consistent with appends and monotonic per key. Routing rule:
every PState below lives on the task `hash(routing-key)` because the writing
event never leaves the depot's ingress partition; client read helpers
therefore pass `{:pkey routing-key}` explicitly. For the text kernel the
routing key is constructed as `[:artifact <artifact-id>]` by a shared-core
constructor (`text-routing-key`), so truth/projection read helpers derive it
from the artifact id; audit read helpers take the routing key (or the full
request envelope, which carries it).

Audit reads (Op A6.1):
- **read-request** (request-id) → verbatim stored request.
  `foreign-select-one (keypath request-id) $$requests {:pkey routing-key}`,
  unwrap the `StoredRequest` record. 1 seek.
- **read-decision** (request-id) → decision with status/reason/errors/routing-key/event-id.
  `foreign-select-one (keypath request-id) $$decisions {:pkey routing-key}`. 1 seek.
- **read-event** (event-id) → the `KernelEvent`, or nil if never created.
  `foreign-select-one (keypath event-id) $$events {:pkey routing-key}`. 1 seek.

Truth reads (Op A6.2), all keyed by artifact-id, pkey derived:
- **read-artifact** (artifact-id) → scalar row incl. `:root-event-id`, head pointer.
  `foreign-select-one [(keypath artifact-id) (submap :artifact/id :artifact/type :root-event-id :head-revision-id :created-time-ms)] $$artifacts`.
  Selecting scalar fields avoids touching the subindexed `:revisions` child. 1 seek.
- **read-text-head** (artifact-id) → `{:revision/id ... :event/id ... :text/content <exact bytes>}`.
  `foreign-select-one (keypath artifact-id) $$text-heads`. 1 seek. Content
  round-trips byte-for-byte because the head row stores the ingested string verbatim.
- **read-units** (artifact-id) → all units, document order.
  `foreign-select [(keypath artifact-id) ALL] $$units`. Subindexed sorted map
  keyed by `Long` line index ⇒ sorted iteration = document order.
  1 seek + N iterations (N = line count).
- **read-unit-statuses** (branch-id) → map unit-id → status row, across artifacts.
  Cross-partition gather ⇒ query topology `unit-statuses-on-branch` (see Query
  Topologies). One client roundtrip.
  (Colocated variant for interpreters/UI: `(keypath artifact-id branch-id)` on
  `$$unit-statuses`, 1 seek — used internally for membership checks.)

Projection reads (Op A6.3), keyed (branch, artifact):
- **read-canonical-view** (branch, artifact) → ordered rows of non-rejected units.
  `foreign-select [(keypath artifact-id branch-id :canonical) ALL] $$projection-views`.
  ~2 seeks (top key, subindexed rows) + N iterations; rows sorted by line index.
- **read-discarded-view** (branch, artifact) → ordered rows of rejected units.
  Same path with `:discarded`.
- Rows are not bare strings: each carries `:unit/id`, `:preview`, `:target/kind :unit`,
  `:target/id`, `:provenance/root-event-id` (lawful compression / C19).

Awaiting affordances (Op A6 consistency requirement):
- **await-decision** (runtime, request|{routing-key, request-id}, timeout) —
  client-side poll loop over read-decision with backoff; returns the decision
  or times out. No sleeps in tests.
- **await-materialized** (runtime, pred-read, timeout) — same poll loop over an
  arbitrary read (e.g. "units visible"). Both are client helpers, not topologies;
  they bridge `:append-ack` (durable-only) to processed state per C13.

Single-`foreign-select` rule check: every read above is one PState/one
partition except the branch-wide status read, which is the only query topology.

## Writes

Every write is one `foreign-append!` of a full ActionRequest envelope to the
single intent depot, ack level `:append-ack` (hardcoded in the client helper —
C13). One depot record encodes all side effects of the operation. Typed
helpers construct envelopes via the shared core and mint omitted ids
client-side with `ops/random-uuid7` (string-encoded) **before** the append (C7).

| Operation | Request type | Routing key | Accepted event type |
|---|---|---|---|
| Op A1 ingest text artifact | `:artifact/ingest` | `[:artifact id]` | `:artifact/ingested` |
| Op A2 unitize (derive line units) | `:artifact/unitize` | `[:artifact id]` | `:artifact/unitized` |
| Op A3 set unit status | `:unit/status-set` | `[:artifact id]` | `:unit/status-set` |
| Op A4 compatibility record | `:compat/record` | caller-supplied | `:compat/recorded` |
| Op A5 raw append (any/unknown type) | any | caller-supplied | none (rejected) unless a known type accepts |

Notes:
- A2 (Ambiguity A6 decision): unitization enters through the **intent ingress
  as its own request type**, target `{:target/kind :event, :target/id <ingest-event-id>}`.
  Rationale: (i) the entity matrix mandates distinct observable states a1
  (ingested, units not yet derived) and a2 (units derived), so unitization
  cannot ride inside the ingest event; (ii) C2 demands a single front door —
  a runtime call that wrote units directly would construct world facts outside
  interpretation; (iii) sharing the artifact's routing key serializes it with
  ingest, so "unitize an event that doesn't exist" rejects deterministically.
  The runtime helper `unitize-lines!` is just a typed envelope constructor.
- The accepted `:artifact/unitized` event **carries the derived unit specs**
  (id, index, preview, char range) computed by interpret from the durable
  ingest event's content. This makes materialization a self-contained pure
  fold (C10) and replay-invariant without re-reading (C8).
- A4 (`:compat/record`, Ambiguity A8 decision): accepted iff the envelope is
  valid; produces `:compat/recorded` whose payload wraps the request payload;
  materializes nothing beyond the event row. The event IS the durable record;
  adding a PState nobody reads would violate the spirit of C17.

## PState Design

All PStates are written by the single `"kernel"` microbatch topology and live
on the ingress partition (`hash(routing-key)`); top-level keys are domain ids,
partition routing for foreign reads is via `:pkey`.

Envelope storage classes (shared core, see Contract Layer Artifacts):
`StoredRequest` (defrecord wrapping the verbatim request map), `Decision`
(defrecord: request-id, status, reason, errors, event-id, routing-key,
time-ms), `KernelEvent` (defrecord: event-id, event-type, ordering, target,
actor, branch, payload, causality, provenance, policy, time-ms), and
`IEventPayload` defrecord variants per event type (`IngestedPayload`,
`UnitizedPayload`, `StatusSetPayload`, `CompatRecordedPayload`). Defrecords are
concrete leaf classes — no `Object` schema anywhere — each constructed at
exactly one code site, which is where field correctness is enforced.

1. **$$requests** `{String StoredRequest}`
   Not obvious — verbatim storage of possibly-malformed maps fights typed schemas:
   - Option A: `fixed-keys-schema` of the envelope — REJECTED: malformed
     requests (junk keys, wrong types) must be stored verbatim (`(= request
     (read-request ...))` even for rejected requests); a typed envelope schema
     would refuse exactly the writes the audit contract requires.
   - Option B: `{String Object}` — REJECTED: `Object` schema banned.
   - Option C (chosen): wrap in `StoredRequest` defrecord. The request is an
     opaque audit value that is never path-navigated into — read whole, stored
     whole. A leaf record class is the typed way to say that. Query cost
     unchanged (1 seek).

2. **$$decisions** `{String Decision}`
   Not obvious — the transitional routing key (`[:artifact id]`, a
   heterogeneous vector; Ambiguity A9) cannot be typed in a fixed-keys schema
   without `Object`:
   - Option A: fixed-keys row + routing key as pr-str String — REJECTED: the
     decision must carry the **same** routing key value as the request (C4);
     encode/decode at the read boundary is a value-fidelity hack.
   - Option B (chosen): `Decision` defrecord (leaf class) carrying the routing
     key verbatim. Single construction site (the decide step) is the
     enforcement point. 1-seek point read; first-class top-level map keeps
     per-request O(1) addressability.
   `:errors` is a vector of `{:type <keyword>, ...}` maps (validation error
   records); nil/empty for accepted. Accepted decisions: `:reason` nil,
   `:event/id` = accepted event id. Rejected: `:event/id` nil. `:time-ms`
   copied from the request (never the wall clock — C8).

3. **$$events** `{String KernelEvent}`
   Obvious: point lookup by event-id; events are immutable, written once on
   acceptance, never modified or deleted. Payload field holds the
   `IEventPayload` variant (polymorphic-data rule: definterface + defrecord,
   not a kitchen-sink fixed-keys). `ordering` is `{:key <routing-key>}` so
   `[:ordering :key]` = the request's `:routing/key`. Events double as the
   permanent revision-content store (see $$artifacts Option discussion).

4. **$$artifacts** `{String (fixed-keys-schema
     {:artifact/id String
      :artifact/type clojure.lang.Keyword
      :root-event-id String
      :head-revision-id String
      :created-time-ms Long
      :revisions (map-schema String String {:subindex? true})})}`
   key = artifact-id; `:revisions` maps revision-id → ingest event-id (lineage,
   A3 traceability). Not obvious — where does revision content live?
   - Option A: separate `$$revisions {artifact-id {revision-id RevisionRow}}`
     storing full content per revision — REJECTED for V0: no read in the
     contract surface ever reads a non-head revision's content; every ingest
     event already carries the content durably and immutably in `$$events`.
     A written-but-never-read table is pure write amplification (content
     stored 3×: event, head, revisions).
   - Option B (chosen): scalar artifact row + `:revisions` id→event-id map;
     non-head revision content is an audit read (`read-event` of the
     revision's event). Costs nothing on the contract read surface;
     read-artifact = 1 seek on scalar fields (the `submap` read never touches
     the subindexed `:revisions` child, and big strings never live in this row).

5. **$$text-heads** `{String (fixed-keys-schema
     {:revision/id String :event/id String :text/content String})}`
   key = artifact-id. Denormalized current-head content. Not obvious:
   - Option A: read head via $$artifacts head pointer → revision/event lookup:
     read-text-head = 2–3 seeks ≈ 1.0–1.5ms.
   - Option B (chosen): dedicated head row overwritten atomically (`termval`)
     on each accepted ingest: read-text-head = 1 seek ≈ 0.5ms. Dominant read
     ("full-content read of the head revision"), read-dominated workload ⇒
     denormalize; write cost is one extra colocated write per accepted ingest,
     amortized. Kept separate from $$artifacts so reading artifact metadata
     never deserializes a whole document (nested fixed-keys values are stored
     as one serialized blob — mixing content into the artifact row would make
     every metadata read load the document).
   Head swap on re-ingest is a single `termval` ⇒ head is always exactly one
   of {old content, new content}, never a blend (A3 invariant).

6. **$$units** `{String (map-schema Long
     (fixed-keys-schema
       {:unit/id String
        :unit/index Long
        :unit/preview String
        :anchor/type clojure.lang.Keyword     ; :text/range
        :anchor/revision-id String
        :anchor/start Long
        :anchor/end Long
        :provenance/root-event-id String})
     {:subindex? true})}`
   key = artifact-id; inner key = line index. Holds the **head revision's**
   units only (A3 decision); historical units remain recoverable from
   `:artifact/unitized` events (audit reads never collapse). Not obvious —
   inner key choice:
   - Option A: inner key = unit-id String. Lexicographic sort breaks document
     order (`line/10` < `line/2`) unless ids are zero-padded (ugly, caps width);
     enumeration must re-sort.
   - Option B (chosen): inner key = `Long` index. Sorted subindexed map ⇒
     `ALL` yields document order natively; enumeration is 1 seek + N
     iterations (range scan, not N point reads — the spec's "efficient
     per-artifact grouping"). Unit-id is defined as
     `"<artifact-id>/<revision-id>/line/<n>"` — stable, deterministic,
     re-derivable (contract requires identity stability, not a format), and
     bijective with (artifact, revision, index), so status-set validation
     parses the index for an O(1) existence probe and gets the
     artifact-mismatch and stale-revision checks for free. The revision-id
     component guarantees prior judgments can never silently re-attach to
     different text after re-ingest (A3 invariant).
   Anchor fields flattened into namespaced keys (one serialized row, fewer
   nesting levels); the read helper reshapes rows to
   `{:unit/id .. :unit/preview .. :anchor {:anchor/type :text/range ...}
     :provenance {:root-event-id ..}}` for the API surface.

7. **$$unit-statuses** `{String (map-schema String
     (map-schema String
       (fixed-keys-schema {:status clojure.lang.Keyword
                           :reason String
                           :event/id String
                           :time-ms Long})
       {:subindex? true}))}`
   key = artifact-id → branch-id → unit-id → status row. Outer key MUST be the
   artifact (routing key) so the status write, the projection move, and the
   interpreter's existence/transition reads are colocated and atomic in one
   event (C4). A branch-keyed top-level layout was rejected: writes would need
   a partitioner hop, voiding single-event atomicity, and branch-keyed foreign
   reads would still miss shards on other partitions. Exactly one current
   status per (branch × unit) — `termval` overwrite; judgment history lives in
   $$events/$$decisions (current-state reads collapse; audit reads never
   collapse). `:reason` preserved (auditable judgment). `:event/id` links the
   row to its accepting event (C10 provenance).

8. **$$projection-views** `{String (map-schema String
     (fixed-keys-schema
       {:canonical (map-schema Long ProjRow {:subindex? true})
        :discarded (map-schema Long ProjRow {:subindex? true})}))}`
   where `ProjRow = (fixed-keys-schema {:unit/id String
                                        :preview String
                                        :target/kind clojure.lang.Keyword
                                        :target/id String
                                        :provenance/root-event-id String})`
   key = artifact-id → branch-id → two row maps keyed by line index (document
   order). Not obvious — materialized vs derived-at-read:
   - Option A: query topology deriving views from $$units + $$unit-statuses
     per read: 2 seeks + (N+M) iterations per UI re-read, no write
     amplification.
   - Option B (chosen): materialized projection table: 2 seeks + N iterations
     per read, and one row insert per unit on unitize / one row move per
     status-set (bounded by line count, same event, batched at microbatch
     flush). Chosen because (i) the UI subscription surface is the
     read-dominated hot path and pre-rolled views are exactly what the
     `projection-*` convention names (C16/C19); (ii) the canonical/discarded
     **partition invariant** (a unit in exactly one view) is enforced at one
     write site instead of re-derived by every reader; (iii) per-key
     atomicity of "status row + view membership" is trivially visible. One
     PState with `:canonical`/`:discarded` as a category dimension — not two
     PStates — per the same-schema-same-key rule.
   Per C19 the table is derived and disposable: rebuild = re-fold
   $$units × $$unit-statuses (or replay the depot from `:beginning`); it is
   never read as truth by interpret.

9. **$$branches** `{String (fixed-keys-schema
     {:branch/id String :parent/branch-id String :created/event-id String})}`
   **SCAFFOLD (C17, declared, documented, intentionally unwritten in V0).**
   Reserved for V1+ branch forking; zero writers, zero readers today. V0
   enforces single-branch via interpretation (A11: non-default branch ⇒
   reject), which is what keeps the branch maps in PStates 7–8 legitimately
   non-subindexed.

10. **$$policies** `{String (fixed-keys-schema
      {:actor/id String :capabilities (set-schema clojure.lang.Keyword)})}`
    **SCAFFOLD (C17).** Reserved for post-genesis capability enforcement; the
    envelope's mandatory `:action/capability` field is validated for presence
    NOW so enabling policy later changes no envelope (Entity 6 / genesis
    rule). Zero writers/readers in V0.

## Depots

- **`*text-intent-depot`**: `(hash-by :routing/key)`, client-appendable.
  Request types: `:artifact/ingest`, `:artifact/unitize`, `:unit/status-set`,
  `:compat/record`, plus arbitrary/unknown envelopes (Op A5 — the generic
  ingress every typed helper uses). This is the kernel's single **intent**
  ingress (C11 structural mandate); the text kernel is intent-only — no claim,
  observation, or control depots are declared (structure must be earned).
  - Partitioner: keyword extractor `:routing/key` — safe on any appended value
    (keyword lookup on a non-map returns nil; nil hashes to a stable
    partition, so even garbage records land deterministically and get decided).
  - All of one routing key's requests share a depot AND a partition ⇒ per-key
    serialization at ingress (C4): ingest, unitize, and status-set for one
    artifact are mutually ordered; different artifacts have no mutual order.
  - Ack: clients append with `:append-ack` (durable append only — C13). With a
    microbatch consumer there is no ack-time processing coordination at all,
    which makes "ack ≠ processed" structural rather than conventional.
  - Trimming: none, ever. The depot is the permanent ingested history;
    replay-invariance (C8/A10) requires re-folding it from `:beginning`.
- No internal (`:disallow`) depots — single topology, no cross-topology handoff.
- No tick depots — no time-driven processing in this kernel.

## Topologies and PStates

- **kernel: microbatch**, because:
  1. No operation requires single-digit-ms visibility; the contract is
     explicitly asynchronous (poll `await-decision`/`await-materialized`;
     decision + materialization visible within hundreds of ms is acceptable).
  2. No value needs returning through the ack path — C13 *forbids* coupling
     ack to processing, so stream's `ack-return>`/`:ack` coordination (its
     main advantage) is contractually excluded.
  3. Exactly-once microbatch semantics make every PState write replay-safe at
     the processing level with no extra machinery; cross-partition atomicity
     is not needed (zero partitioners) but harmless.
  4. Throughput: per-line fan-out on ingest/unitize batches all subindex
     writes into one flush.

  The topology body contains **no partitioners**: depot ingress lands each
  record on `hash(:routing/key)` and every owned PState is written on that
  task. One depot record ⇒ one atomic group of writes ⇒ C4 atomicity and the
  matrix's "never a half-materialized artifact" hold structurally. Two
  same-key records in one microbatch are processed sequentially on the task
  and the second sees the first's writes (single-threaded task model), so
  deterministic acceptance reads "durable state at its position in the key's
  serialized history" exactly.

  Idempotency audit (defense in depth — required even under microbatch,
  because a client may retry `foreign-append!` after a timeout, producing a
  *second depot record* with the same request-id, which exactly-once
  processing does NOT dedupe):
  - Dedup guard (first step per record): `local-select> (keypath request-id)
    $$decisions` — if a decision exists, the record is a duplicate; stop.
    Because all writes for a record commit atomically (no partitioner),
    decision-exists ⟺ all effects applied; the guard is a complete dedup.
    This implements C8 idempotency and resolves Ambiguity A4 as
    **first-write-wins** (the audit record is never merged or rewritten; a
    duplicate is a durable no-op). 1 extra seek per record.
  - Event-id collision guard (accept path): `local-select> (keypath
    proposed-event-id) $$events` — a *different* request proposing an existing
    event id is rejected `:event-id-conflict` (A4 second half); event
    immutability (Entity 2, e1) is never violated by an overwrite. 1 seek.
  - Every PState write is `termval` (or whole-subtree replace/`VOID>` on
    re-ingest) keyed by caller-minted ids ⇒ naturally idempotent. No counters,
    no `AFTER-ELEM` appends, no topology-side id generation anywhere (C7 —
    ids minted client-side with `ops/random-uuid7` before ingress; replaying
    the same record can never produce different ids).

  Per-record processing order (one task, one atomic group):
  1. Derive audit key: the request's `:request/id` if it is a valid string;
     otherwise a deterministic surrogate `"invalid-id/" + content-hash(record)`
     so even id-less garbage gets a durable, re-derivable decision (no silent
     drops, replay-stable).
  2. Dedup guard (above).
  3. Write `$$requests` ← `StoredRequest(verbatim record)` (never normalized).
  4. Envelope validation via shared `request-validation-errors` — runs BEFORE
     type dispatch (observable contract: malformed unknown type rejects
     `:request-invalid`, not `:unknown-action-type`). All applicable typed
     errors collected, not just the first.
  5. Type dispatch (C9 interpret): pure fn `(interpret request state-view)`
     per `:request/type`; `:default` ⇒ reject `:unknown-action-type`.
     State-view reads per type (all colocated local-selects):
     - `:artifact/ingest`: `$$artifacts` artifact-id (exists? ⇒ revision path) — 1 seek.
     - `:artifact/unitize`: `$$events` target event (must exist, be
       `:artifact/ingested`, match the routing key's artifact, and be the
       head revision's event) + `$$units` (already unitized for this revision
       ⇒ reject `:already-unitized`) — 2 seeks. Derives unit specs from the
       event payload's content.
     - `:unit/status-set`: parse unit-id → artifact/revision/index checks
       (mismatch ⇒ reject), `$$units` index probe (missing ⇒ reject
       `:target-unit-not-found`), branch must be `default-branch-id` in V0
       (else reject `:branch-not-found`, A11), status ∈ V0 domain
       `#{:rejected :canonical}` (else reject `:status-invalid`, A7), current
       status read for transition check (V0: any→any within domain, last
       accepted wins) — 2 seeks.
  6. Reject ⇒ write `$$decisions` (status `:rejected`, specific reason,
     errors, nil event-id, request's routing key verbatim). **No other write
     happens** — materializers only ever fold accepted events, so "rejected
     requests never create world state" is structural (C6).
  7. Accept ⇒ event-id collision guard; mint `KernelEvent` with `:event/id` =
     `:proposed/event-id`, `[:ordering :key]` = `:routing/key` (C5); write
     `$$decisions` (accepted, event-id, reason nil — A12) + `$$events` +
     per-event-type pure materializer (C10):
     - `materialize-ingested`: `$$artifacts` row (create with
       `:root-event-id` = this event id, or add revision entry + move
       `:head-revision-id`), `$$text-heads` ← new head (termval swap),
       and on re-ingest (A3): replace `$$units[artifact]` (`VOID>` the
       subindexed map directly, then nothing — new revision starts
       un-unitized) and reset that artifact's `$$projection-views` branch rows
       (`VOID>` each subindexed row map) so views never blend revisions.
       Old units/judgments remain audit-readable via events; status rows keep
       their revision-scoped unit-ids and never re-attach.
     - `materialize-unitized`: for each unit spec in the event payload:
       `$$units[artifact][index]` ← row (with anchor + provenance) and
       `$$projection-views[artifact][branch][:canonical][index]` ← ProjRow for
       the default branch (unjudged ⇒ canonical), unless a `:rejected` status
       row already exists for that unit-id (race: status accepted between
       revisions cannot happen — ids are revision-scoped — but the check keeps
       the fold total).
     - `materialize-status-set`: `$$unit-statuses[artifact][branch][unit-id]`
       ← `{:status .. :reason .. :event/id ..}` (termval) and move the
       projection row between `:canonical`/`:discarded` (delete from one,
       termval into the other — the exactly-one-view invariant is enforced at
       this single write site).
     - `materialize-compat-recorded`: nothing (event row only, A8).

  PStates owned (full schemas in PState Design): `$$requests`, `$$decisions`,
  `$$events`, `$$artifacts`, `$$text-heads`, `$$units`, `$$unit-statuses`,
  `$$projection-views`, plus scaffolds `$$branches`, `$$policies`.

- No stream topology exists in this module. (If one is ever added, every write
  above is already idempotent under replay: termval-only + dedup guard.)

## Query Topologies

- **unit-statuses-on-branch** `[*branch-id :> *statuses]` — the only
  cross-partition read (statuses are stored per-artifact for write atomicity;
  the contract read is branch-wide).
  Shape: `(|all)` → on each task, range-scan `$$unit-statuses` top-level keys
  with `{:allow-yield? true}` (cooperative multitasking on a potentially large
  scan), selecting `(keypath *artifact *branch-id)` submaps via
  `[ALL (subselect ...)]`-style navigation → emit per-artifact status maps →
  `(|origin)` → `aggs/+merge` into one unit-id → status-row map.
  - Input example 1: empty world → T per-task scans (T = task count), 0
    meaningful entries → returns `{}`.
  - Input example 2: 3 judged artifacts on 2 tasks → same T scans, 3 non-empty
    submaps merged.
  - Fixed or variable: the fan-out is **fixed** (one range scan per task —
    inherent to a gather-all; no per-key point reads to pad), the *content*
    per scan is variable and handled by aggregation (`aggs/+merge`), never by
    hardcoded reads.
  - Scale note: acceptable for V0 (single default branch, interactive read
    rate). When branch forking lands, the UI should prefer the colocated
    (branch, artifact)-scoped read (1 seek) and this gather becomes an
    explicitly-paginated admin read.
- Canonical/discarded view reads are **not** query topologies — single
  partition, single PState, one foreign-select each (decision rule 1).

## Design Decisions

- **Subindexing**:
  - `$$units` inner index map, `$$unit-statuses` inner unit map,
    `$$projection-views` `:canonical`/`:discarded` row maps, `$$artifacts`
    `:revisions` map — all subindexed: each grows per-document/per-history
    and any instance can exceed 100 entries (line counts, judgments,
    revisions are unbounded). All four are sorted-scan read patterns (1 seek +
    N iterations), never N point reads.
  - Branch-level maps in `$$unit-statuses`/`$$projection-views` — NOT
    subindexed in V0: branch cardinality is application-enforced at exactly 1
    (A11 rejects non-default branches; `$$branches` is scaffold). This is a
    real enforced bound, not "typically small". Migration trigger: when
    forking lands, convert via subindex migration before lifting the A11
    rejection.
  - Top-level maps are first-class (RocksDB-backed) — individually addressable
    by definition.
- **Colocation**: depot partitions by `:routing/key`; every PState write
  happens on that ingress task with zero partitioner hops; all interpret-time
  state reads (artifact existence, event lookup, unit probe, status read) are
  local. The whole module is single-key colocated by construction — this is
  simultaneously the I/O optimization and the C4 atomicity guarantee.
- **Reads route by routing key (`:pkey`)**: rejected alternative — a global
  `request-id → routing-key` secondary index would either break single-event
  atomicity (cross-key write needs a hop) or add a non-atomic index topology;
  unnecessary because every reader (client helper or test) constructed the
  request or knows the artifact id, from which the routing key is derivable
  via the shared constructor. Assumption to verify in implementation: depot
  `hash-by` and foreign-select `:pkey` use the same key hash (both are Rama's
  standard partitioning of the same value).
- **Microbatch over stream**: latency budget is hundreds of ms; C13 forbids
  ack-coupled processing; exactly-once removes the processing-retry dedup
  class. Stream offers nothing the contract is allowed to use.
- **Events as revision store**: no `$$revisions` content table in V0 — content
  lives once in the immutable event (audit truth) and once in the denormalized
  head (hot read). Avoids a written-never-read table (C17 spirit) and 3×
  content amplification.
- **Empty/edge content (A5)**: empty-string ingest is **accepted** (ingress is
  judgment-free; validation is envelope-shaped, not content-shaped). Line
  derivation is defined deterministically: `""` → 0 units; otherwise split on
  `\n` preserving interior empties and dropping exactly one trailing empty
  segment produced by a trailing newline. Head round-trip is byte-exact
  regardless (round-trip is served by `$$text-heads`, units are views).
- **Runtime lifecycle (Op A7)**: the runtime handle bundles the cluster
  connection plus depot/PState/query foreign handles. `close-text-runtime!` is
  idempotent (double-close guarded) and releases all client resources; reads
  after close fail with a closed-client error, never corrupt state. In-flight
  requests at close are safe by construction: the depot append is the only
  client-side effect, it is durable at `:append-ack`, and the microbatch
  topology persists its consumed offset and resumes after restart — no
  topology state lives in the client. Fresh IPC per test starts from an empty
  world (w0); all reads return nil/empty there without error.

## State primitive selection

- `$$requests` (PState): O(1) write per depot record (one termval). Durable —
  audit truth, never deleted.
- `$$decisions` (PState): O(1) per record. Durable — audit truth.
- `$$events` (PState): O(1) per accepted record (payload size O(document) for
  ingest/unitize — bounded by the input the application controls). Durable —
  THE world facts; also the revision-content archive.
- `$$artifacts` (PState): O(1) per accepted ingest. Durable truth.
- `$$text-heads` (PState): O(1) per accepted ingest (value O(document)).
  Durable denormalized view of truth; rebuildable by re-folding ingest events
  but kept durable because it serves the dominant read.
- `$$units` (PState): O(lines) writes per accepted unitize — bounded by input
  document length. Durable truth (head revision's units).
- `$$unit-statuses` (PState): O(1) per accepted status-set. Durable truth.
- `$$projection-views` (PState): O(lines) per unitize, O(1) per status-set —
  bounded by inputs. Durable but **derived/disposable** (C19): concrete
  rebuild path = re-fold `$$units` × `$$unit-statuses` per artifact (or replay
  the untrimmed depot from `:beginning` through the same materializers).
- `$$branches`, `$$policies` (PStates): zero writes in V0 — declared scaffold
  (C17), documented above.
- TaskGlobals: **none**. No non-durable state exists anywhere in the design,
  so no rebuild-on-restart path is required beyond Rama's own PState
  durability and topology offset resume.
- External systems: **none**. The text kernel's "work" is materialization
  inside the topology (no executor — C15 N/A; no observation ingress needed —
  C11/C12 N/A for an intent-only kernel).

---

# Contract Layer Artifacts (Layer b)

The shared core (one namespace, used by clients, helpers, and every kernel
module) owns:

1. **Envelope constructors**: `action-request` (builds the full C3 envelope;
   mints any omitted `request-id`/domain-id/`proposed-event-id` via
   `ops/random-uuid7` string-encoded, fills `:branch` with
   `default-branch-id`, copies `:action/type` into `:request/type` so drift
   cannot be introduced by the helper; `:time-ms` is caller-supplied or
   helper-stamped at construction — never inside the kernel). `kernel-event`
   and `decision` constructors are used ONLY by kernel accept/decide paths.
2. **Validators**: `request-validation-errors` (pure; returns ALL applicable
   typed errors: `:request/id-missing`, `:request/type-missing`,
   `:routing/key-invalid` (nil/malformed), `:proposed/event-id-missing`,
   `:request/action-type-drift`, `:request/payload-event-id`,
   `:action/capability-missing`, `:target-missing`); `valid-request?` /
   `valid-event?` — mutually exclusive by construction (a request requires
   `:request/type` and must NOT carry top-level `:event/id`; an event requires
   `:event/id`+`:event/type` and must NOT carry `:request/type`); both are
   structural predicates that work on maps and records alike.
3. **Routing-key constructors** per kernel (`text-routing-key` ⇒
   `[:artifact id]`) — the single migration site for the transitional key (A9).
4. **Envelope record classes**: `StoredRequest`, `Decision`, `KernelEvent`,
   `IEventPayload` variants — the three-envelope taxonomy as disjoint concrete
   classes.
5. **Client discipline helpers**: `append-action-request!` (the generic
   ingress: `foreign-append!` with `:append-ack` hardcoded), `await-decision`,
   `await-materialized` (poll/backoff), and — for future kernels that wire
   into others (C14) — `append-into-kernel!` (mirror depot + `|hash$$`
   partition alignment + explicit `:append-ack`), unused by the text kernel.

Module-side contract pattern (every kernel instance, fixed by KERNEL-SHAPE):
one intent depot `(hash-by :routing/key)` → zero-partitioner kernel topology →
dedup guard → shared envelope validation → `interpret` (pure, type-dispatched,
`:default` ⇒ `:unknown-action-type`) → decision write → accepted-events-only
`materialize-*` pure folds → optional `projection-*` tables.

# C1–C19 Enforcement Map

| Req | Enforced at | Violation detectability |
|---|---|---|
| C1 three envelopes | Shared core constructors/validators; Decision/KernelEvent are distinct record classes; requests are maps | Property test: `valid-request?`/`valid-event?` mutually exclusive on any value |
| C2 requests before decisions | Structural: the only client-appendable depot accepts requests; `$$events` written solely by the kernel topology (PState ownership); no API constructs events client-side | Any depot accepting `KernelEvent`s, or a second writer to `$$events`, fails module review/compile-shape check |
| C3 envelope validation | `request-validation-errors` executed in-topology (step 4) before dispatch; helper pre-check is convenience only | Rejected decisions persist the full typed error list — auditable; malformed-unknown-type ordering test |
| C4 routing-key discipline | Depot `hash-by :routing/key` (ingress partitioner mandate); **zero partitioners** in topology ⇒ per-key single-event atomicity is structural; Decision copies the key verbatim; `[:ordering :key]` set from the request at the single mint site; reads route via `:pkey` = routing key | Code review red flag: any partitioner inside the kernel topology; tests assert request/decision/event key equality |
| C5 identity headers | Validation error `:request/payload-event-id` (pre-dispatch); event-id assigned only from `:proposed/event-id` in the accept path; `:event-id-conflict` guard protects immutability; rejected ⇒ event-id nil | `read-event(smuggled-id)` is nil forever; event-id == proposed-id assertion on every accepted op |
| C6 decision contract | Decision + event + materialization written in one atomic group; dedup guard ⇒ exactly one decision per request-id; rejected path writes only request+decision (materializers fold events, which exist only on accept) | Matrix tests per state: rejected ⇒ truth/projection reads byte-identical; one-decision-per-id |
| C7 mint before ingress | Client helper mints via `ops/random-uuid7` pre-append; topology contains no id-generation construct (no ModuleUniqueIdPState, no random, no clock) | Grep-level check on module code; replay/duplicate tests: same record ⇒ same ids |
| C8 determinism laws | `interpret` = pure fn(request, colocated-state-view); time copied from request; idempotency = decision-existence guard + termval-only writes; replay-invariance = self-contained events + pure folds + untrimmed depot (`:start-from :beginning` rebuild) | Deterministic-acceptance & idempotency: duplicate-append tests; replay-invariance: planned cold-replay property test (A10 — stated, no oracle yet) |
| C9 interpret pattern | Type-dispatched pure `interpret` with `:default` ⇒ `:unknown-action-type`; envelope validation strictly precedes dispatch | Unknown-type and malformed-unknown ordering tests; crash = contract failure |
| C10 materialization pattern | Per-event-type pure `materialize-*` fns, invoked only on accepted events; every derived row schema carries `:event/id`/`:provenance/root-event-id` slots filled at the single write site | Row-content tests (unit/projection rows trace to ingest event); schema slots make missing provenance visible |
| C11 depot-family taxonomy | Module declares exactly one intent depot; no claim/obs/control (intent-only kernel; structure earned) | Module inventory vs KERNEL-SHAPE entry (see C18 guard) |
| C12 back-arrow | N/A operationally (no external workers); upheld structurally: no UI-facing side channel exists — all reads go through PStates | Architecture review: any non-PState read surface is a violation |
| C13 ack semantics | `:append-ack` hardcoded in `append-action-request!`; microbatch consumer cannot couple ack to processing (`ack-return>` is stream-only) — structural; `await-*` helpers provided | No `:ack`-dependent read-after-write anywhere; tests poll, never sleep |
| C14 cross-kernel wires | N/A for text (no outbound wires); shared `append-into-kernel!` helper codifies partition-aligned, acked appends + declared-read-surface rule for kernels that wire | Helper is the only sanctioned cross-kernel write path; direct mirror-PState writes are impossible in Rama anyway |
| C15 executor option | N/A — text work executes as in-topology materialization; no executor, no spawn registry | Module has no TaskGlobal/executor code to audit |
| C16 naming/vocabulary | `$$requests/$$decisions/$$events/$$artifacts/$$text-heads/$$units/$$unit-statuses` (plural primaries), `$$projection-views` (`projection-*`), no `-by-*` needed in V0; single `:request/type` dispatch site, no cross-context keyword collisions | Naming review against KERNEL-SHAPE conventions |
| C17 scaffold declared | `$$branches`, `$$policies` declared with scaffold docstrings + plan documentation (zero writers/readers, reserved contracts named) | Undocumented unused PState = violation; this plan is the documentation |
| C18 shape maintenance | Process rule: this module's entry (depots, pstates, topology, request/event types) is added to KERNEL-SHAPE `:examples` in the same change | Suggested guard: test asserting the module's declared inventory matches its KERNEL-SHAPE entry |
| C19 projections preserve identity | `ProjRow` schema includes `:unit/id`, `:target/kind`, `:target/id`, `:provenance/root-event-id`; filled at the single materializer site; projections never read as truth; rebuild path documented | View-row content tests; rebuild = re-fold `$$units` × `$$unit-statuses` |

# Ambiguity Decisions (A1–A12)

- **A1** (text-instance doc is a pointer): designed from kernel.clj-derived
  spec + trail + tests only, as instructed; reconcile against the anchors doc
  in a later pass if it surfaces additional contract content.
- **A2** (read surface vs storage): resolved by this plan — storage layout per
  PState Design; every spec read capability has a named access path above.
- **A3** (re-ingest semantics): **accept-as-new-revision.** The envelope
  carries an explicit `revision-id` and V0 names "text revision
  materialization" — rejection would make both dead weight. Mechanics: head
  `termval` swap (never a blend), `:revisions` lineage map (root-event-id
  unchanged = traceable), revision-scoped unit-ids (judgments can never
  re-attach to different text), `$$units`/views reset to the new
  (un-unitized) head, old revisions/units/judgments permanently audit-readable
  via events. All Entity-3 either-way invariants hold.
- **A4** (duplicate/colliding identity): duplicate request-id append ⇒
  **first-write-wins durable no-op** (decision-existence guard) — preserves
  one-decision-per-id, makes client retry-after-timeout safe, and prevents a
  second append from rewriting audit history (immutability). Distinct request
  proposing an existing event-id ⇒ **reject `:event-id-conflict`** — event
  immutability is non-negotiable (Entity 2 e1).
- **A5** (empty content): **accepted**; `""` unitizes to zero units; trailing
  single newline drops exactly one trailing empty segment; interior empty
  lines are units. Head round-trip is byte-exact independently of unitization.
- **A6** (unitization ingress): **own request type `:artifact/unitize`
  through the intent ingress**, target kind `:event` (payload carries no
  `:event/id` — C5 stays total). Re-unitize of an already-unitized head
  revision ⇒ reject `:already-unitized` (idempotent in effect); replay of the
  same request ⇒ dedup no-op. Rationale under Writes.
- **A7** (status vocabulary): V0 domain `#{:rejected :canonical}` (`:canonical`
  = explicit un-reject so the view partition stays total and re-judgment is
  meaningful); unknown status ⇒ reject `:status-invalid`; transitions
  unconstrained within the domain, last accepted wins; vocabulary extension is
  an interpret-level change only (no envelope/schema change).
- **A8** (`:compat/record`): accept-if-well-formed; event `:compat/recorded`
  wraps the request payload verbatim; no materialization beyond the event row.
  Legacy paths get durable, decided, auditable records through the front door
  without inventing an undocumented materialization.
- **A9** (transitional routing key): keep `:routing/key` = `[:artifact id]`;
  construction is centralized in the shared core's per-kernel routing-key
  constructors, so a future migration to domain-specific keys touches one
  site plus a depot-record migration — the discipline (C4), not the shape, is
  what the module enforces.
- **A10** (replay-invariance): designed-for (self-contained events, pure
  folds, deterministic interpret, untrimmed depot, `:start-from :beginning`
  rebuild path) but treated as stated-not-verified; the cold-replay /
  retry / partitioner-consistency property tests are named as the missing
  oracles for a later phase.
- **A11** (non-default branch): **reject `:branch-not-found`** in V0. Keeps
  branch cardinality application-enforced at 1 (which legitimizes the
  non-subindexed branch maps), guarantees default-branch state is unaffected,
  and reserves accept-and-create for the V1 fork design with `$$branches`.
- **A12** (accepted-decision metadata): accepted decisions carry
  `:decision/reason` nil and empty `:errors`; the `policy` slot on
  `KernelEvent` and `Decision` fields stay reserved (nil) until policy
  enforcement lands — envelope stable, no information invented.
