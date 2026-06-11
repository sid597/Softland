# Plan

<!-- Phase 1 Step 5. Fill in after completing Steps 1-4. After completing this, fill in PLAN_VALIDATION.md. -->
<!-- RETROSPECTIVE Phase 1: derived from IMPLICIT_SPEC.md only. Code-blind by design. -->

Module: **SpaceKernelModule** — the local-world kernel. One companion module is assumed to exist and is
referenced only through mirrors: **LLMKernelModule** (owns run execution state: `llm-run`, `llm-pending`,
`llm-control`, `approvals-by-run`/`pending-approval`, raw items `item-by-id`/`items-by-run`, `cost-rollup`,
`run-detail`). The boundary rule from the spec is binding: Space owns turns and frozen context bundles and
**never runs the model**; everything Space needs from the LLM side crosses as durable depot records.

Throughput is low everywhere (hundreds–thousands of requests/day, bursty), latency is interactive
(decision + canonical readability in tens of ms; ~100ms-class). Reads vastly outnumber writes. This means:
optimize for read seeks and correctness under retry, not for write throughput.

---

## Reads

Every read must be safe in every entity state: absent → `nil`/empty, never an error. All point lookups below
use `keypath` (navigates to nil on absence — satisfies the always-readable rule). Access-method decision per
phase rules: single path against one PState on one partition → `foreign-select-one`; anything needing >1
PState read per result → query topology.

| # | Read | Access method | Path expression | Partition (routing key) |
|---|------|---------------|-----------------|-------------------------|
| R1 | space by id (title, status, turn-count, parent lineage, llm thread id) | `foreign-select-one` | `[(keypath space-id) (submap :title :status :turn-count :parent-space-id :llm-thread-id)]` on `$$spaces` | hash(space-id) |
| R2 | space fork graph (children of a parent) | `foreign-select` | `[(keypath space-id) :children ALL]` on `$$spaces` (sorted-set range scan) | hash(space-id) |
| R3 | ordered turn ids per space | `foreign-select` | `[(keypath space-id) (sorted-map-range ...)]` (or full `ALL`) on `$$turn-order` — seq→turn-id pairs, ordered | hash(space-id) |
| R4 | turn by id (kind, prompt, refs, bundle/run/control pointers) | `foreign-select-one` | `[(keypath turn-id)]` on `$$turns` | hash(turn-id) |
| R5 | bundle-by-turn / run-by-turn / control-by-turn | `foreign-select-one` | same read as R4 — pointers are **fields of the turn record** (`:bundle-id`, `:run-id`, `:control-id`); 1 seek answers all three | hash(turn-id) |
| R6 | context bundle by id (options, `sha256:` hash, rendered input) | `foreign-select-one` | `[(keypath bundle-id)]` on `$$bundles` | hash(bundle-id) |
| R7 | bundle by turn | client: R4 then R6 (2 point lookups, ids known) — no query topology needed; UI already holds turn id | — |
| R8 | run dispatch by run id (`read-llm-run-request`) | `foreign-select-one` | `[(keypath run-id)]` on `$$dispatches` | hash(run-id) |
| R9 | slice / overlay / derivative by id | `foreign-select-one` | `[(keypath material-id)]` on `$$materials` (polymorphic record) | hash(material-id) |
| R10 | patch proposal by id | `foreign-select-one` | `[(keypath proposal-id)]` on `$$patch-proposals` | hash(proposal-id) |
| R11 | event by id | `foreign-select-one` | `[(keypath event-id)]` on `$$events` | hash(event-id) |
| R12 | decision by request id (awaitable) | `foreign-select-one` `[(keypath request-id)]` on `$$decisions`; additionally the decision value is returned in the depot-append ack via `ack-return>` so the awaiting caller needs zero polling on the happy path | hash(request-id) |
| R13 | send-by-idempotency-key | `foreign-select-one` | `[(keypath space-id key)]` on `$$send-idempotency` (key is space-scoped — see Ambiguity A-keys) | hash(space-id) |
| R14 | catalog object by id / **object-detail projection** | `foreign-select-one` | `[(keypath object-id)]` on `$$catalog` — the stored record carries `:object/type` + salient fields, so it is self-describing; no query topology needed for a 1-seek read | hash(object-id) |
| R15 | relations in/out by object id / **object-relations projection** | query topology `object-relations` (two subindexed-set reads on one partition batched into one roundtrip) | see Query Topologies | hash(object-id) |
| R16 | **chat-canvas projection** per space | query topology `chat-canvas` (space record + turn order + latest turn record = multiple PStates) | see Query Topologies | hash(space-id), leading-partitioner optimized |
| R17 | run-detail projection, cost-rollup per llm thread, raw item by id/by run, approvals/pending-approval | **owned and served by LLMKernelModule** — Space neither stores nor proxies these; clients read them from the LLM kernel directly. Listed here only because the spec's read matrix exercises them; the cross-module contract section states what Space guarantees about them | LLM kernel partitions |

Validation pass (app-design Step 3): every Space-owned read above has a one-path expression against one
designed PState except R15/R16, which are exactly the multi-read cases the rules route to query topologies.

## Writes

All client-facing writes are requests appended to one depot (`*space-requests`); observation-driven writes
arrive via a mirror of the LLM kernel's observation depot. Every request carries: actor, request time
(`:request/at` — the **only** time source; topologies never read wall clock), caller-supplied request id,
caller-supplied entity ids, and a routing space id. All derived ids are pure functions of the request id:
`<req>/event/<entity>`, `<req>/llm-request`, `<req>/llm-control` (spec's collision-instead-of-duplication
property).

| # | Write op | Depot | Record type (`:request/type`) |
|---|----------|-------|------------------------------|
| W1 | create space | `*space-requests` | `:space/create` {space-id, title?, actor, at, request-id} |
| W2 | compose-and-send | `*space-requests` | `:space/compose-and-send` {space-id, turn-id, bundle-id, run-id, prompt, refs[{kind,id}], execution-options, idempotency-key?, actor, at, request-id} |
| W3 | comment turn | `*space-requests` | `:turn/comment-create` {space-id, turn-id, overlay-id, prompt, refs?, actor, at, request-id} |
| W4 | slice turn | `*space-requests` | `:turn/slice-create` {space-id, turn-id, slice-id, source-item-id?, source-content-hash, snapshot-text?, source-content-text?, actor, at, request-id} |
| W5 | derivative turn | `*space-requests` | `:turn/derivative-create` {space-id, turn-id, derivative-id, content, source-ref?, actor, at, request-id} |
| W6 | patch accept / reject | `*space-requests` | `:turn/patch-accept` / `:turn/patch-reject` {space-id, turn-id, proposal-id?, reason?, actor, at, request-id} |
| W7 | tool-approval resolve | `*space-requests` | `:turn/tool-approval-resolve` {space-id, turn-id, run-id, approval-id, decision (:approved/:expired/:declined), native-correlation-id (passed through verbatim), actor, at, request-id} |
| W8 | cancel | `*space-requests` | `:turn/cancel` {space-id, turn-id, run-id, reason, actor, at, request-id} |
| W9 | compact | `*space-requests` | `:turn/compact-request` {space-id, turn-id, run-id, strategy, actor, at, request-id} |
| W10 | steer | `*space-requests` | `:turn/steer` {space-id, turn-id, run-id, payload, actor, at, request-id} |
| W11 | fork-from-span | `*space-requests` | `:space/fork-from-span` {parent-space-id (= routing key), child-space-id, turn-id, slice-id, bundle-id, run-id, snapshot-text, source-content-hash, prompt?, execution-options, from-native-thread-id (caller-supplied verbatim), child-native-thread-id?, actor, at, request-id} |
| W12 | patch-proposal ingestion | `*llm-observations` (mirror, appended by LLM side; Space only **sources** it) | `:codex/patch-proposal`-class observations {proposal-id, run-id, space-id, turn-id, summary, files[{path, hunk-count}], at} — fields preserved verbatim into the proposal |
| W13 | accepted-fact fan-out (internal) | `*accepted-facts` (`:disallow`) | one record per accepted request: {request-id, space-id, ordered fact family with types/ids/salient fields} — consumed by the catalog topology |
| W14 | run dispatch (cross-module) | `*llm-intake` (mirror of LLM kernel intake depot) | `:llm/run-request` {dispatch-id = `<req>/llm-request`, run-id, turn-id, space-id, bundle-id, bundle-hash, llm-thread-id, fork fields?, at} |
| W15 | control dispatch (cross-module) | `*llm-intake` (mirror) | `:llm/control` {control-id = `<req>/llm-control`, control-type (:approval/resolve, :turn/cancel, :compact/request, :turn/steer), run-id, turn-id, space-id, decision?, native-correlation-id?, reason?, strategy?, payload?, actor, at} |

Validation (op 10) is a **pure client-side builder**: shape errors (`:request/type-invalid` — e.g. a run
request whose request type is a control type) are caught before any append; building requests/observations
mutates nothing (pure-builders guarantee). Defense in depth: the topology re-checks the type discriminator
and mints a `:rejected :request/type-invalid` decision for malformed records that reach the depot anyway,
keeping "one request → exactly one durable decision" true even for bad appends.

---

## PState Design

Worked backwards from the reads. All values are concrete types — **no `Object` anywhere**. Polymorphic
positions use `definterface` + `defrecord` per the schema rules. Record types referenced below:
`JournalEntry`, `DecisionRecord`, `FactsRecord`, `EventRecord`, `TurnRecord`, `BundleRecord`,
`OptionsRecord`, `DispatchRecord`, `ProposalRecord`, `FileRecord`, `SendFacts`, `CatalogObject`,
and `IMaterial` implemented by `SliceMaterial` / `OverlayMaterial` / `DerivativeMaterial`.

### Space-partition group (key = space id; colocated with `*space-requests`)

**`$$spaces`** `{String (fixed-keys-schema {:title String, :status clojure.lang.Keyword, :turn-count Long, :parent-space-id String, :llm-thread-id String, :created-by-request String, :children (set-schema String {:subindex? true})})}`
- Obvious core: point lookup by space id (R1) is a 1-seek submap read. `:children` is the fork graph
  (R2): unbounded (heavily-explored spaces are shallow-but-wide) → subindexed sorted set, per-parent child
  enumeration is 1 seek + range iteration. Keeping children inside the space record instead of a separate
  `$$space-graph` PState avoids a second top-level key per space with the identical partition key (skill
  rule: don't split same-key categories); the fixed-keys + subindexed-set combination keeps R1 from ever
  touching the children set.
- `:created-by-request` exists for the duplicate-id rule (Ambiguity 1): conditional creates can distinguish
  "my own retry" from "different request reusing the id".

**`$$turn-order`** `{String (map-schema Long String {:subindex? true})}` — space id → seq → turn id.
- Option A: subindexed **vector** of turn ids, appended per turn. Append = read size + write; under stream
  retry an append is **non-idempotent** and would need the journal anyway; replaying a journaled request
  cannot re-target "the same append".
- Option B (chosen): subindexed **sorted map** seq→turn-id where seq is assigned once on first processing
  and pinned in the journal entry. The write becomes `termval` at a fixed key — **idempotent under replay**.
  Query cost identical to A (ordered range scan: 1 seek + N×5µs for N turns; latest turn = point lookup at
  `(dec turn-count)` using the count from `$$spaces`, 1 seek). Chosen for replay-correctness at zero read
  cost. Unbounded (hundreds–thousands of turns) → subindexed.

**`$$request-journal`** `{String (map-schema String JournalEntry {:subindex? true})}` — space id → request id → entry.
- The dedup spine for the stream topology. `JournalEntry` = defrecord {status, reason, turn-seq,
  event-ids (small vector, ≤5), facts (FactsRecord), replayed?, idempotency-key}. Written once per request,
  if-absent, **atomically with** the order/count/idempotency writes in the same task segment (single-threaded
  task ⇒ check-then-act is safe). Unbounded per space → subindexed. Never deleted (skill rule; decisions are
  forever-readable). Obvious design: it must live on the deciding partition or the dedup check races.

**`$$send-idempotency`** `{String (map-schema String SendFacts {:subindex? true})}` — space id → key → SendFacts.
- SendFacts = defrecord {original-request-id, event-ids, space-id, turn-id, bundle-id, bundle-hash, run-id,
  dispatch-id, llm-thread-id}. Must be colocated with the decision point: "under concurrent replays of the
  same key, at most one set of facts is ever minted" is only achievable if check-and-set is atomic with
  acceptance — same task, same event. Keys unbounded over a space's lifetime → subindexed. (Scoping decision
  recorded under Ambiguities.)

### Entity-keyed group (key = entity id; reached by `|hash` hop from the space partition)

**`$$decisions`** `{String DecisionRecord}` — request id → defrecord {status (:accepted/:rejected), reason,
space-id, event-ids (vector of String, ≤5 — not subindexed, fixed small), facts (FactsRecord: turn-id,
bundle-id, bundle-hash, run-id, dispatch-id, llm-thread-id, control-id, control-type), replayed?, at}.
Obvious: point lookup by request id, write-once termval. Decision **carries no control-type for patch
resolutions** (spec op 5) and carries it for control turns (op 6).

**`$$events`** `{String EventRecord}` — event id (`<req>/event/<entity>`) → defrecord {event-type, request-id,
space-id, entity-id, at}. Obvious: point lookup, write-once, deterministic key.

**`$$turns`** `{String TurnRecord}` — turn id → defrecord {kind, space-id, prompt, refs (vector of {kind,id}
records), bundle-id, run-id, control-id, proposal-id, actor, at}.
- Option A: separate index PStates `$$bundle-by-turn`, `$$run-by-turn`, `$$control-by-turn` (all keyed by
  turn id — same key, same partition). Cost: a "full turn read" (R4+R5) = up to 4 seeks; 3 extra PStates of
  pure duplication.
- Option B (chosen): pointers as fields of the turn record. R4+R5 = **1 seek**. The skill rule against
  splitting same-schema/same-key categories applies directly. The absent-pointer semantics (`bundle-by-turn`
  empty for space-only turns — the no-LLM-side-effect guarantee) fall out as nil fields.

**`$$bundles`** `{String BundleRecord}` — bundle id → defrecord {turn-id, hash (sha256:-prefixed String),
rendered-input String, options OptionsRecord {agent-kind, model, approval-policy, sandbox, cwd}, at}.
Write-once, immutable forever; rendered input can be KBs–MBs, read-few → a single serialized value under a
top-level key is exactly right (one seek to read, never trimmed, no inner indexing needed). Non-bundle-owned
options are excluded at render time by **allowlist** (only the five bundle-owned keys are ever written), so
exclusion is structural, not a filter that can drift.

**`$$materials`** `{String IMaterial}` — material id → one of `SliceMaterial` {snapshot-text, snapshot-hash,
source-content-hash, source-item-id, turn-id, space-id, at}, `OverlayMaterial` {text, refs, turn-id,
space-id, at}, `DerivativeMaterial` {content, authorship = :user, render-as = :user-authored, source-ref,
turn-id, space-id, at}.
- One PState, not three: identical access pattern (point lookup by id) and partition key; the skill rule says
  one PState with a category dimension. Different field sets per kind → `definterface IMaterial` + defrecords
  (the documented polymorphic-data pattern), not a loose fixed-keys union.

**`$$dispatches`** `{String DispatchRecord}` — run id → defrecord {dispatch-id, run-id, turn-id, space-id,
bundle-id, bundle-hash, llm-thread-id, from-native-thread-id, child-native-thread-id, at}. Space's durable,
traceable copy of the cross-kernel hand-off (R8); write-once termval. Keyed by run id because that is the
read key ("run-dispatch by run id").

**`$$patch-proposals`** `{String ProposalRecord}` — proposal id → defrecord {status (:pending/:accepted/
:rejected), run-id, space-id, turn-id, summary, files (vector of FileRecord {path, hunk-count}),
resolution-turn-id, resolution-reason, at}. Point lookup by proposal id (the only exercised read). Status
transitions in place; conditional writes give first-resolution-wins and redelivery safety (see topology
section). Per-space/per-run listing is flagged-but-unexercised in the spec → no secondary index now; if it
lands later it is a new colocated PState fed by the same topology, not a migration of this one.

### Catalog group (key = catalog object id = pure function of (type, entity id), e.g. `"obj:turn:<turn-id>"`)

**`$$catalog`** `{String CatalogObject}` — object id → defrecord {object-type (:space/:turn/:context-bundle/
:llm-turn-run/:slice/:overlay/:derivative/:llm-item), entity-id, salient (small map of
clojure.lang.Keyword→String: title for spaces, bundle-id for turns, turn-id for bundles, llm-thread-id for
runs, authorship for slices/derivatives)}. Object ids derivable by any caller without lookup (spec op 8).
Self-describing → R14 is a single seek with no wrapper query topology.

**`$$relations`** `{String (fixed-keys-schema {:out (set-schema String {:subindex? true}), :in (set-schema String {:subindex? true})})}` — object id → adjacency.
- In/out per object are unbounded over time (relations grow monotonically, never removed) → both sets
  subindexed. Set-add is naturally idempotent — load-bearing for the stream catalog topology. Reverse edges
  are first-class (`:in`), satisfying "reverse edges queryable" with 1 seek + range iteration per direction.

### Deliberately NOT designed as PStates

- **Chat-canvas as a materialized projection PState** — rejected. It would duplicate the full unbounded turn
  order per space and add write amplification on every turn, to save ~2 seeks on a read that is already
  ≤3 seeks via query topology. Spec requires projections to be pure functions of canonical state, droppable
  and rebuildable at any time — a query topology is the degenerate-perfect form of that (nothing stored,
  nothing to rebuild).
- **LLM-side state** (`llm-run`, `llm-pending`, approvals, items, cost-rollup) — owned by LLMKernelModule.
  Space's plan obligates the contract (below), not the storage.

---

## Depots

- **`*space-requests`** — `(hash-by :route/space-id)`. Every request record carries `:route/space-id`:
  the target space for W1–W10, the **parent** space for fork (W11) because the fork decision (parent
  existence, parent fork-graph write) must be made atomically on the parent's partition. Event types: all of
  W1–W11. One depot, not several: all request types are order-dependent on the same entity (turn ordering,
  idempotency check, create-vs-implicit-create races are all serialized by landing on the space's task in
  append order), and they share one latency/ack profile (interactive, `:ack`). Dispatched inside the
  topology with `<<subsource`/case on `:request/type`.
- **`*accepted-facts`** — `:disallow`, appended via `depot-partition-append!` (`:append-ack`) once per
  accepted request from the requests topology, on the local (space) partition. Consumed by the catalog
  topology. `:append-ack` (not `:ack`) so the interactive decision path doesn't wait on catalog derivation
  and cannot deadlock-couple the two topologies.
- **`*llm-intake`** — `(mirror-depot setup *llm-intake "…/LLMKernelModule" "*intake")`. Cross-module
  dispatch target for W14/W15. Appends routed with `|hash$$` by `:run/id` (contract: the LLM kernel
  partitions its intake by run id so run-request and all controls for one run serialize on one LLM-side task).
- **`*llm-observations`** — `(mirror-depot setup *llm-observations "…/LLMKernelModule" "*observations")`.
  Sourced (not appended) by Space for patch-proposal ingestion. High-volume token stream; Space filters to
  patch-like observation types as the first operation and drops everything else before any partitioner hop.

Colocation: `*space-requests` hash key = top-level key of `$$spaces`/`$$turn-order`/`$$request-journal`/
`$$send-idempotency` → the entire decision segment runs with zero repartitioning. Entity-keyed PStates are
reached by one `|hash` hop each, which is unavoidable given reads are by entity id (caller-supplied ids can't
be made to colocate with their space under hash partitioning).

---

## Topologies and PStates

### `space-core` — **stream**, owns all canonical PStates

Why stream, not microbatch: decisions are awaitable interactive-path latency ("tens of ms desirable",
"~100ms" send path, UI blocks on the decision). Microbatch's ≥300ms floor violates the spec's latency
requirements; stream + `:ack` makes `foreign-append!` return exactly when the decision and all canonical
facts are visible, and `ack-return>` hands the decision record back in the ack so the happy path needs zero
polling. Retry mode **`:all-after`**: per-partition append order is preserved under retry, so a transiently
failing `space/create` cannot be leapfrogged by a pipelined `compose-and-send` that implicitly creates the
same space (replays are harmless — every record's processing is journal-deduplicated).

Sources: `*space-requests` (all request types) and `*llm-observations` (patch extracts). Both kinds of
writes to `$$patch-proposals` therefore live in one topology, satisfying single-owner-per-PState.

**Event flow for requests** (transaction scope = between partitioners; every segment analyzed for
partial-apply + replay):

*Segment 1 — space partition (the decision point; all writes in this segment are atomic with each other):*
1. `local-select>` journal entry for `(space-id, request-id)`.
   - **Hit** → this is a same-request retry/redelivery: skip all Segment-1 mutations, re-emit the recorded
     decision + facts downstream (branch 2 below).
   - **Miss** → check `$$send-idempotency` (sends only): **key hit** → idempotency replay: write a journal
     entry marked `replayed?` pointing at the original facts, emit decision-only (branch 3). **Key miss** →
     fresh request (branch 1).
2. Branch 1 (fresh): state-level checks against `$$spaces` (exists? for space-only/control turns →
   `:rejected :space/not-found`; exists-with-different-creator for `space/create` → `:rejected :space/exists`;
   parent exists? for fork). Compute the decision **purely** from the request payload + local state: assign
   turn seq = current `:turn-count`, render the bundle (pure function: prompt + refs rendered as
   `object:<id>` / `slice:<id>` / `user-authored-derivative:<id>` with **no existence checks**; options
   filtered by bundle-owned allowlist), compute `sha256:` hash, derive event ids in pinned order
   `[space? slice? turn bundle? run?]`, derive dispatch/control ids. Write atomically: journal entry,
   `$$spaces` (conditional create with `:created-by-request`, or `term inc` on `:turn-count` — guarded by
   journal-create so it executes at most once), `$$turn-order` at the pinned seq (`termval`), idempotency
   entry (if-absent), fork-graph `:children` set-add (fork, on parent). Rejected requests write only the
   journal entry.
3. All branches emit downstream with everything needed carried in dataflow vars (no later segment re-reads
   space state).

*Segments 2..n — entity partitions (one `|hash` hop each; order: bundle before dispatch):*
`$$turns`, `$$bundles`, `$$materials`, `$$dispatches`, `$$events` (via `ops/explode` over the event list),
`$$decisions` (+ `ack-return>` of the decision), `$$patch-proposals` resolution transition, mirror appends
to `*llm-intake`, internal append to `*accepted-facts`.

**Per-PState write idempotency audit (required for stream):**

| Write | Idempotent? | Why correct under at-least-once replay |
|---|---|---|
| `$$request-journal` entry | yes | keyed (space-id, request-id), written if-absent; the guard itself |
| `$$spaces` create | yes | conditional create; `:created-by-request` distinguishes own-retry (skip, already created) from foreign id reuse (skip, never clobber — Ambiguity 1) |
| `$$spaces` `:turn-count` inc | **non-idempotent → deduplicated** | executed only in the journal-create branch; journal write + inc are in the same task segment (atomic per single-threaded task event); replay takes the journal-hit branch and never re-increments |
| `$$spaces` `:children` set-add (fork) | yes | set conj of child id |
| `$$turn-order` write | yes | `termval` at journal-pinned seq (this is why it's a sorted map, not an appended vector) |
| `$$send-idempotency` | yes | write-if-absent, atomic with decision on one task — concurrent same-key sends serialize on the partition, exactly one mints facts |
| `$$turns`/`$$bundles`/`$$materials`/`$$dispatches`/`$$events`/`$$decisions` | yes | `termval` keyed by deterministic/caller ids; values are pure functions of (request payload, journal-pinned seq) → byte-identical on replay. `$$materials` and child-`$$spaces` (fork) writes are conditional-if-absent to enforce never-clobber under foreign id reuse |
| `$$patch-proposals` create (obs source) | yes | write **iff absent** by proposal id → redelivery cannot duplicate a pending proposal and cannot reset a resolved one (`:pending` is only ever written into absence) |
| `$$patch-proposals` resolution | yes | conditional transform: if `:status = :pending` → set status/resolution-turn/reason; else if `resolution-turn-id` = this turn → already applied (no-op); else → lost the race, no-op (first-resolution-wins). Redelivered ingest after resolution can't resurrect `:pending` (covered by create-iff-absent above) |
| `*llm-intake` mirror appends | at-least-once → **deduplicated at consumer** | see cross-module contract below |
| `*accepted-facts` append | at-least-once → consumer idempotent | catalog topology writes are termval/set-add |

**Partial apply + replay across hops:** Segment 1's writes flush together; a crash before flush replays the
record and redoes Segment 1; a crash after flush but mid-tree replays the record, hits the journal, and
**re-emits all downstream effects** (this is required — the first attempt may have died before, say, the
bundle write), all of which are idempotent or consumer-deduplicated. Therefore the all-or-nothing fact-family
invariant (turn ⇒ bundle ⇒ dispatch eventually all exist) converges under any crash pattern: transient
absence is possible (spec-tolerated), permanent inconsistency is not, because retry continues until the whole
event tree succeeds and `:ack` only releases the caller then.

**Idempotency-key replay vs same-request retry are different branches by design:** key-replay (different
request id) emits decision + journal only — zero fact writes, **zero dispatch** (no LLM-side row for the
replay's fresh ids, as the spec demands); same-request retry re-emits everything (collides harmlessly).

PStates owned: `$$spaces`, `$$turn-order`, `$$request-journal`, `$$send-idempotency`, `$$decisions`,
`$$events`, `$$turns`, `$$bundles`, `$$materials`, `$$dispatches`, `$$patch-proposals` (full schemas above).

### `catalog` — **stream**, owns `$$catalog`, `$$relations`

Sources `*accepted-facts`. Why stream: spec wants sub-second convergence of catalog/relations after the
decision ("tests poll-await them"); microbatch's floor works against that for zero correctness gain — every
write here is idempotent (`termval` keyed by pure-function object ids; set-adds for edges), so at-least-once
retry is already exactly-once in effect. Why a separate topology fed by an internal depot rather than inline
hops in `space-core`: (a) keeps the awaited interactive event tree short — the spec explicitly allows
catalog to converge after the decision; (b) gives "derived purely from accepted facts" a single enforcement
point. Duplicate `*accepted-facts` records (from space-core retries) are absorbed by idempotent writes.

Work per record: explode fact family → per object `|hash object-id` → `termval` CatalogObject; per edge
(space→turn, turn→bundle, turn→run, bundle→run, turn→material, raw-item→slice) two set-adds (`:out` on
source partition, `:in` on target partition — two hops). Lazy promotion: an `:llm-item` CatalogObject is
written only when a slice fact references a raw item — observation alone never reaches this topology, so
raw items are uncatalogued until referenced, exactly as specced.

Retry mode `:individual` (writes are order-insensitive and idempotent).

### Query topologies — see next section.

---

## Query Topologies

### `chat-canvas` `[*space-id :> *canvas]`
Returns `{:projection/type :chat-canvas, :projection/source :canonical-space-state, :space/id, :title,
:status, :turn-order [turn-ids...], :latest-turn TurnRecord-or-nil}` — self-describing, declares its
canonical source, pure function of canonical PStates.

- Leading `(|hash *space-id)` → client-side routed (leading-partitioner optimization).
- Input example 1 — space with 50 turns: read 1: space submap (1 seek, meaningful); read 2: `$$turn-order`
  range scan (1 seek + 50×5µs, meaningful); read 3: latest turn = `|hash` to turn partition, point read
  `$$turns` (1 seek, meaningful). **3 total, 3 meaningful.** Cost ≈ 1.5ms + 0.25µs·iter — well inside budget.
- Input example 2 — missing space id: read 1: space submap → nil. Emit nil canvas, **skip reads 2 and 3**.
  **1 total, 1 meaningful.**
- Input example 3 — empty space (turn-count 0): read 1 only; `:turn-count 0` from the space record proves
  order is empty → emit canvas with `[]` order and nil latest-turn **without touching `$$turn-order` or
  `$$turns`**. **1 total, 1 meaningful.**
- **Variable** → handled dynamically with `<<if` branches keyed off the space record (`nil?` /
  `zero? turn-count`), not padded fixed reads. `|origin` after the conditional join; single emit of `*canvas`.

### `object-relations` `[*object-id :> *relations]`
Returns `{:projection/type :object-relations, :object/id, :out [...], :in [...]}` (empty vectors for
unconnected objects — "empty, not error").

- Leading `(|hash *object-id)` → both subindexed set scans happen on one partition in one roundtrip
  (the reason this is a query topology instead of two foreign-selects): `local-select>` `(subselect [:out ALL])`
  and `(subselect [:in ALL])` on `$$relations`.
- Input example 1 — run object after a send: 2 reads, both meaningful (in = {turn, bundle}, out = {}… the
  empty direction is still the answer being asked for). Input example 2 — object with no edges: 2 reads,
  both are the asked-for answer. **Fixed at 2 reads** for all inputs — the read targets don't depend on
  input shape, and neither read can be proven empty without performing it. No dynamic handling needed.
- `{:allow-yield? true}` on both scans (adjacency can grow large; don't starve the task thread).

No other query topologies: every remaining read is a single-path point read (see Reads table), and
object-detail is served by the self-describing `$$catalog` record in one seek.

---

## Design Decisions

### Subindexing
- `$$spaces :children`, `$$relations :in/:out` — unbounded growth (forks, monotone relation graph) → subindexed sorted sets, range-scanned.
- `$$turn-order` inner map — hundreds–thousands of turns → subindexed; ordered range scan is the canvas read; point read of last seq via `turn-count - 1`.
- `$$request-journal`, `$$send-idempotency` inner maps — one entry per request/keyed-send forever (never deleted) → subindexed.
- NOT subindexed: decision `event-ids` vector (≤5, pinned), proposal `files` vector (a few hunks per proposal), `OptionsRecord`/salient maps (fixed small) — application-bounded small structures.

### Colocation
- Depot hash key (`:route/space-id`) = key of all Segment-1 PStates → decision, ordering, idempotency, and fork-graph writes need zero repartitioning and are mutually atomic on one task.
- Entity-keyed PStates accept one `|hash` hop each on the write path (amortized: once per accepted request) to buy 1-seek foreign reads by exactly the ids the read contract uses. At this throughput, hops are negligible; read-path seeks dominate.

### Cross-module dispatch contract (Space → LLM kernel) — exactly-once-or-deduplicated, stated mechanism
1. **Transport:** `depot-partition-append!` to the mirrored `*llm-intake`, routed `|hash$$` by `:run/id`,
   ack level `:append-ack` — Space's event tree (and thus the caller's `:ack`) completes only after the
   dispatch/control record is durably in the LLM depot. "Space guarantees the dispatch is durable."
2. **Delivery semantics:** at-least-once (Space-side stream retries re-append; mirror ack does not wait for
   LLM topologies).
3. **Dedup mechanism:** dispatch id `<req>/llm-request` and control id `<req>/llm-control` are deterministic.
   The LLM kernel's intake topology creates its run row / control row **iff the id is absent** (conditional
   create on its own partition, atomic on its task). A redelivered record collides with itself and is
   dropped. Net effect: exactly-once run/control materialization under any retry pattern. Idempotency-key
   replays never reach the intake at all (decision-only branch).
4. **Ordering:** intake partitioned by run id ⇒ the run-request and all subsequent controls for that run
   serialize on one LLM-side task; terminal-state guards (below) make order races non-corrupting anyway.
5. **LLM-side guard obligations (contract, enforced in LLMKernelModule):** status transitions are
   guarded — terminal states (`:cancelled`/`:failed`/`:completed`) are never overwritten by late controls or
   redelivered records; pending-queue entries are removed exactly when status leaves `:pending` and never
   re-added for terminal runs; approval resolution survives redelivery (resolve-if-pending, keyed by
   approval id, native correlation id passed through verbatim); compaction history is appended keyed by
   control id (re-delivery collides, no duplicate entries).

### Bundle-before-run ordering and fork-binding durability under replay
- The `$$bundles` write precedes the `*llm-intake` append in the dataflow, but stream PState flushes are not
  globally ordered against mirror appends — so ordering is **enforced at the consumer as a claim gate**, the
  same shape the spec mandates for fork bindings: the executor may not claim a run unless (a) the bundle id
  on the run resolves via the LLM kernel's mirror of `$$bundles`, and (b) for forks, the native thread
  binding is durable. A failed gate check returns not-spawned (`:fork-binding-not-durable` for (b)), invokes
  no adapter, leaves the run `:pending` and unclaimed, and is re-checkable and side-effect-free. Replay
  cannot regress either gate: bundles are write-once with deterministic content (a replayed write is
  byte-identical), and fork fields ride the dispatch record whose redelivery is dropped by id-dedup —
  a durable binding can never be overwritten back to absent.
- **Bundle-freeze under replay:** the rendered input + hash are pure functions of the request payload
  (refs rendered by id with no existence checks, options allowlisted, timestamps from the payload), so every
  retry freezes the identical bundle and the decision's hash always equals the bundle's hash. Frozen-forever
  holds because no code path writes a bundle key twice with different bytes and no mutation path exists.

### Determinism / rebuildability
No wall-clock reads (all times from request/observation payloads), no randomness (all ids caller-supplied or
derived), no iteration-order leaks into state (ordered things are explicit: pinned event-id order
`[space? slice? turn bundle? run?]`, journal-pinned turn seqs; sets are semantically unordered). Replaying
the depots from empty therefore reproduces byte-identical canonical state, catalog, and projections —
projections doubly so, being query topologies over canonical state.

### Ambiguity resolutions (all 12 from IMPLICIT_SPEC)
1. **Duplicate entity ids across different requests** — Rule: ids are a caller contract (globally unique);
   kernel behavior is *reject where atomically checkable, never-clobber elsewhere*. `space/create` on an
   existing id → `:rejected :space/exists` (checkable on the deciding partition). Entity-keyed creates
   (turn/bundle/material/child-space/run ids) are conditional-if-absent: a foreign reuse silently keeps the
   first writer's data (decision may overclaim in this caller-bug case — accepted residual, recorded here
   because the alternative is cross-partition pre-checks that break single-event atomicity).
2. **Patch-accept with missing/unknown proposal id** — stays a silent no-op turn (observed contract).
   Reason: resolution targets live on another partition; a decision-time existence check would either break
   Segment-1 atomicity or force the decision to depend on cross-partition state. The turn records the act;
   the conditional transform finds nothing and mutates nothing.
3. **Re-resolution races** — first-resolution-wins on proposals (conditional transform guarded by current
   status + resolution-turn-id); approval double-resolve: resolve-iff-pending, second is a recorded turn with
   no state effect; cancel of terminal run / compact of missing run: control recorded, LLM-side guarded
   no-op. All non-corrupting by construction.
4. **Observation redelivery** — proposals created iff-absent; resolutions never reset by ingest; approval
   resolution survives redelivery (LLM-side resolve-iff-pending). Required-by-retry-safety rows in the spec's
   matrix hold structurally.
5. **Dangling refs render without existence checks** — kept, deliberately: rendering must be a pure function
   of the request for determinism and replay-identical bundles; existence checks would add cross-partition
   reads and make frozen content depend on read timing. Dangling refs are the requester's responsibility.
6. **Fork edge cases** — fork from nonexistent parent: `:rejected :space/not-found` (decided atomically on
   the parent partition — consistent with space-only-turn asymmetry; only compose-and-send implicitly
   creates). Binding never durable: run pends forever in this design (gate is re-checkable and free; no
   timeout policy invented — flagged for ops monitoring). Child-space-id collision: rule 1 (never-clobber).
7. **`:turn/steer`** — validated, turn recorded, control derived and dispatched like compact; run-side
   semantics deferred to the LLM kernel (recorded-only until specified).
8. **Run success path** — out of Space scope; `:completed` is an LLM-kernel terminal state covered by the
   terminal-guard contract. Space stores nothing run-status-shaped, so no schema change is needed when it lands.
9. **Event re-mint on sends into existing spaces** — no space event re-minted: event list is
   `[turn bundle run]` when the space exists, `[space turn bundle run]` on implicit creation. Deterministic
   from pre-state, pinned in the journal.
10. **Space lifecycle** — `:active` only; no archive/close/delete; nothing is ever deleted (skill rule:
    reads can arrive at any time, forever).
11. **Overlay cataloging + turn→material edges** — resolved uniformly: ALL accepted material facts are
    eagerly cataloged (overlays included, type `:overlay`) and every material turn gets a turn→material edge.
    One rule, no special cases; supersets the exercised contract without contradicting it.
12. **Cost-rollup zero state** — LLM-kernel owned; contract: **absent** before the first usage observation
    (consistent with the universal absent-not-error read rule).
- **A-keys (from op 2/13):** idempotency keys are **space-scoped** — the exactly-one-wins guarantee requires
  the key check to be atomic with acceptance, which requires colocation with the deciding partition.
  Cross-space same-key sends (flagged untested in spec) are therefore independent — documented behavior.
- **llm-thread-id provenance** (unpinned in spec): derived deterministically as `<space-id>/llm-thread`,
  stored on the space record, the dispatch, and the run catalog object. No lookup, replay-stable.
- **Parent native thread id at fork** (unpinned): supplied verbatim by the fork request (caller reads it from
  run-detail) — keeps the decision a pure function of request + local space state; Space passes it through
  unaltered per the hand-off obligation.

---

## State primitive selection

- `$$spaces` (PState): O(1) writes per request (create/inc/title/child-add). Durable — canonical truth.
- `$$turn-order` (PState): O(1) per accepted turn. Durable — canonical order; the hot canvas read.
- `$$request-journal` (PState): exactly 1 write per request. Durable — **must** survive worker restart or
  stream retries after a crash would double-apply non-idempotent Segment-1 effects; this is the dedup spine.
- `$$send-idempotency` (PState): ≤1 write per keyed send. Durable — replay detection must survive restarts.
- `$$decisions`, `$$events` (PStates): 1 / ≤5 writes per request. Durable — the audit contract is forever-readable.
- `$$turns`, `$$bundles`, `$$materials`, `$$dispatches`, `$$patch-proposals` (PStates): O(1) writes per
  request/observation-extract. Durable — canonical facts; bundles explicitly "stored full fidelity, never trimmed".
- `$$catalog`, `$$relations` (PStates): O(fact-family) ≈ ≤6 objects + ≤6 edges per accepted request —
  bounded by inputs the application controls. Durable derived view: rebuildable in principle from depots, but
  write volume is trivially bounded and the read contract (detail/relations panes, point reads at any time)
  wants indexed durable storage, so PState, not cache.
- **TaskGlobals: none.** Space holds no external clients, no caches, no executor (the executor and its
  spawn-registry live in the LLM kernel, outside this module). Therefore there is **no non-durable state in
  this module and no rebuild path is required** — every byte of module state is a durable, replicated PState,
  and topologies resume from persisted depot offsets on worker restart (PStates are not recomputed from
  depot history; they are durable storage in their own right).
- External systems: none touched by Space directly. Model execution is reached only through the
  `*llm-intake` mirror depot (durable hand-off, deduplicated by deterministic ids); never via in-topology
  side effects — so stream retries can never double-spawn anything physical from this module.
