# LLM Track Derived Contract Layer

Status: derived contract draft, 2026-05-10.

This doc derives the executable contract from:

```text
llm-track-v2.md
llm-track-canonical.md
three-depot-current-system.md
slice-a-compute-run-command.md
action-request-kernel-routing.md
```

The canonical doc answers "what is this architecture?" This doc answers
"what does Rama actually receive, decide, materialize, and let the UI query?"

Origin pressure:

```text
How do we store an LLM chat artifact that is simultaneously:
  - a raw execution trace,
  - a user-facing editable/sliceable artifact,
  - material for future prompts,
  - a node in a fork/reconciliation graph,
  - and a stream of tool calls, approvals, costs, and proposed world changes?
```

## 0. Contract Spine

```text
                   USER / UI PROJECTION
                            |
                            | ActionRequest
                            v
                    *world-action-depot
                            |
                            | WorldTopology decides
                            v
        +---------------------------------------------+
        | World PStates                               |
        | $$world-threads                             |
        | $$world-turns                               |
        | $$context-bundles                           |
        | $$objects + $$artifact-graph                |
        | $$slices / $$overlays / $$derivatives       |
        +-------------------+-------------------------+
                            |
                            | foreign-append
                            | {:llm/turn-run-request ...}
                            v
                       *llm-depot
                            |
                            | LLMTopology decides
                            v
        +---------------------------------------------+
        | LLM PStates                                 |
        | $$llm-threads                               |
        | $$llm-turn-runs                             |
        | $$llm-pending-by-task                       |
        +-------------------+-------------------------+
                            |
                            | executor reads pending,
                            | claims, gets durable grant
                            v
                       Codex app-server
                            |
                            | streaming observations
                            v
                      *llm-obs-depot
                            |
                            | LLMTopology folds
                            v
        +---------------------------------------------+
        | LLM trace PStates                           |
        | $$llm-items-by-turn-run                     |
        | $$llm-tool-calls-by-run-id                  |
        | $$llm-token-usage-by-run-id                 |
        | $$llm-views                                 |
        +-------------------+-------------------------+
                            |
                            | foreign-append proposals /
                            | catalog progress
                            v
                    *world-action-depot
```

The loop is:

```text
Projection -> ActionRequest -> depot -> Rama decision
  -> accepted KernelEvent or rejected ActionDecision
  -> PState materialization -> Projection
```

No UI code, executor code, or helper service writes PStates. Topology is the
only PState writer.

## 1. Layer Boundaries

```text
LAYER                  OWNS
---------------------  -----------------------------------------------
World track            authored user meaning, WorldTurns, objects,
                       overlays, derivatives, slices, artifact graph,
                       ContextBundle freezing, accepted world facts

LLM track              model execution lifecycle, native Codex threads,
                       LLMTurnRuns, raw LLM items, tool traces, token
                       usage, approval waits, execution observations

ContextBundle          immutable bridge: exact input Softland chose to
                       show the model for one upcoming LLMTurnRun

Object catalog/graph   citeable identity and typed relations across
                       world artifacts and raw LLM items
```

Boundary rule:

```text
World owns meaning and acceptance.
LLM owns execution trace.
ContextBundle is authored by World/Context and consumed by LLM.
```

## 2. Identity Model

```text
ID                     MEANING
---------------------  -----------------------------------------------
world-thread/id        long-lived user-facing chat/canvas lane
world-turn/id          one user move inside a WorldThread
context-bundle/id      frozen model input for one LLM-triggering turn
llm-thread/id          Softland identity for native executor conversation
native/codex-thread-id Codex app-server thread id
llm-turn-run/id        one model execution attempt for one WorldTurn
llm-item/id            raw item emitted/observed inside an LLMTurnRun
object/id              citeable catalog identity
artifact-edge/id       typed relation in artifact graph
request/id             proposed change entering a depot
decision/id            durable accept/reject answer
event/id               accepted fact, produced only after decision
```

Cardinality:

```text
WorldThread 1 -> many WorldTurns
WorldTurn   1 -> 0 or 1 ContextBundle        (V0)
WorldTurn   1 -> 0 or 1 LLMTurnRun           (V0)
LLMThread   1 -> many LLMTurnRuns
LLMTurnRun  1 -> many raw LLM items

Many WorldTurns produce no LLMTurnRun:
  :slice-create
  :comment-create
  :derivative-create
  :patch-accept
  :cancel
  :abandon
```

Retry note:

```text
V0 records executor retries as attempts under the same llm-turn-run/id.
If later we need semantic re-execution, mint a new llm-turn-run/id and
link it with :retry-of. Do not overload one run id to mean multiple
independent model executions.
```

## 3. Depot Contracts

### 3.1 Depot List

```text
DEPOT                    WRITER                         ROUTING KEY
-----------------------  -----------------------------  ----------------------
*world-action-depot      UI, LLMTopology, Compute       :routing/key
                         topology/executors via
                         foreign-append

*llm-depot               WorldTopology only             [:llm-run run-id]

*llm-claim-depot         LLMExecutor                    [:llm-run run-id]

*llm-obs-depot           LLMExecutor                    [:llm-run run-id]

*llm-control-depot       WorldTopology                  [:llm-run run-id]
                         for cancel, approval resolve,
                         steer, compact
```

Working default:

```text
No separate *context-bundle-depot in V0.

ContextBundle is frozen by WorldTopology from a world ActionRequest and
materialized in $$context-bundles before the foreign-append to *llm-depot.

If Sid later chooses a dedicated context depot, the logical contract stays
the same:
  context bundle must be immutable,
  must exist before LLM execution,
  and must be referenced by llm-turn-run-request.
```

### 3.2 World ActionRequest Envelope

The first physical record Rama sees for user-authored LLM work is always an
ActionRequest in `*world-action-depot`.

### 3.2.0 Minimum-Viable Envelope

MVP scoping may omit nice-to-have context fields, but not the fields needed for
traceability, idempotency, routing, and policy.

```text
MUST HAVE
  :request/id
  :request/type
  :idempotency/key
  :routing/key
  :actor
  :target
  :action
  :payload

SHOULD HAVE
  :request/schema-version
  :request/time-ms
  :client/op-id
  :causal
  :provenance

OPTIONAL UNTIL WORLD BRANCHING EXISTS
  :branch
  :context
```

`:branch` means a Softland world-state branch, not the git branch of the local
workspace. If Rama-side world branching is not implemented in the slice, omit
`:branch` or set it to a single explicit default such as `world/main`. Git
branch / cwd belong in execution metadata, not in the authoritative world
branch field.

```clojure
{:request/id "req_..."
 :request/type :world-turn/compose-and-send
 :request/schema-version 1
 :request/time-ms 1778400000000

 :idempotency/key "actor:client-op:hash"
 :client/op-id "local-ui-op-..."

 :routing/key [:world-thread "chat-A"]

 :actor {:actor/id "sid"
         :actor/type :human}

 :branch {:branch/id "world/main"}

 :context {:projection/id "softland/chat-canvas"
           :selection/id nil}

 :target {:target/kind :world-thread
          :target/id "chat-A"}

 :action {:action/type :world-turn/compose-and-send
          :action/capability :llm/send-turn
          :action/params {:provider :codex
                          :model "gpt-5.2-codex"}}

 :payload {:world-thread/id "chat-A"
           :draft/id "draft-1"
           :prompt/text "..."
           :refs [{:object/id "note-17"}
                  {:slice/id "slice-3"}]
           :llm/options {:approval-policy :on-request
                         :sandbox :workspace-write
                         :cwd "/mnt/data/projects/Softland"}}

 :causal {:parents []
          :correlation/id "corr-..."
          :intent/id "intent-..."}

 :provenance {:source/type :projection
              :source/ref "softland/chat-canvas"}}
```

### 3.2.1 World ActionDecision And KernelEvent

Accepted decisions carry both the primary event and any sibling events created
by the same request. The primary event is the one that answers the request most
directly; sibling events are accepted facts produced as part of the same
transaction.

```clojure
{:decision/id "dec-req-..."
 :decision/status :accepted
 :request/id "req_..."
 :request/type :world-turn/compose-and-send
 :routing/key [:world-thread "chat-A"]
 :event/id "evt-world-turn-WT-1"
 :event/ids ["evt-world-turn-WT-1"
             "evt-context-bundle-B-1"
             "evt-llm-run-requested-run-1"]
 :decided-at-ms 1778400000050}
```

Rejected decisions keep the same trace surface and carry no event.

```clojure
{:decision/id "dec-req-..."
 :decision/status :rejected
 :request/id "req_..."
 :request/type :world-turn/compose-and-send
 :routing/key [:world-thread "chat-A"]
 :event/id nil
 :event/ids []
 :decision/reason :target-not-found
 :errors [{:error/code :world-thread/missing
           :target/id "chat-A"}]
 :decided-at-ms 1778400000050}
```

Example accepted facts:

```clojure
{:event/id "evt-world-turn-WT-1"
 :event/type :world-turn/created
 :request/id "req_..."
 :routing/key [:world-thread "chat-A"]
 :world-thread/id "chat-A"
 :world-turn/id "WT-1"
 :world-turn/kind :compose-and-send}

{:event/id "evt-context-bundle-B-1"
 :event/type :context-bundle/frozen
 :request/id "req_..."
 :routing/key [:world-thread "chat-A"]
 :world-thread/id "chat-A"
 :world-turn/id "WT-1"
 :context-bundle/id "B-1"
 :context-bundle/hash "sha256:..."}

{:event/id "evt-llm-run-requested-run-1"
 :event/type :llm-turn-run/requested
 :request/id "req_..."
 :routing/key [:world-thread "chat-A"]
 :world-thread/id "chat-A"
 :world-turn/id "WT-1"
 :context-bundle/id "B-1"
 :llm-turn-run/id "run-1"}
```

The foreign-append to `*llm-depot` is downstream of these accepted facts. It is
not where the user request first becomes true.

### 3.3 World ActionRequest Types

```text
REQUEST TYPE                         PRODUCES LLM RUN?
-----------------------------------  -----------------
:world-thread/create                 no
:world-turn/draft-save               no
:world-turn/compose-and-send         yes
:world-turn/slice-create             no
:world-turn/comment-create           no
:world-turn/derivative-create        no
:world-turn/fork-create              yes, for child thread first turn
:world-turn/reconcile-create         yes, for synthesis thread first turn
:world-turn/patch-accept             no
:world-turn/patch-reject             no
:world-turn/tool-approval-resolve    control only
:world-turn/cancel                   control only
:world-turn/compact-request          control only
:llm/proposal/patch-create           no, pending world decision
:llm/proposal/note-create            no, pending world decision
:llm/progress/catalog-update         no, status/catalog projection
```

### 3.4 LLM Turn Run Request

Only WorldTopology writes this record to `*llm-depot`.

```clojure
{:request/id "llm-req-..."
 :request/type :llm/turn-run-request
 :request/schema-version 1
 :request/time-ms 1778400000100
 :idempotency/key "world-event:evt-context-bundle-frozen"

 :routing/key [:llm-run "run-1"]

 :world-thread/id "chat-A"
 :world-turn/id "WT-1"
 :context-bundle/id "B-1"
 :llm-thread/id "llm-thread-A"       ; existing or newly minted
 :llm-turn-run/id "run-1"

 :executor {:agent/kind :codex
            :native/thread-id nil    ; nil for first run, set for follow-up
            :fork/from-native-thread-id nil}

 :payload {:context-bundle/id "B-1"
           :executor/pool :local-codex
           :executor/hints {:interactive? true}}}
```

Run execution knobs such as `:model`, `:approval-policy`, `:sandbox`, and `:cwd`
are sourced from the frozen ContextBundle. The LLM run request may carry
executor scheduling hints and native thread/fork refs, but it must not create a
second independent copy of bundle-owned options. If an option can affect what
Codex sees or how Codex is allowed to act, freeze it in the bundle.

LLMTopology records an LLM-side ActionDecision for this request too. That
decision answers "can this executor track run this bundle now?", not "is the
WorldTurn true?" The WorldTurn already became true in WorldTopology.

```clojure
{:decision/id "llm-dec-run-1"
 :decision/status :accepted
 :request/id "llm-req-..."
 :request/type :llm/turn-run-request
 :routing/key [:llm-run "run-1"]
 :event/id "llm-evt-run-1-requested"
 :decided-at-ms 1778400000110}
```

### 3.5 LLM Claim Record

Executor writes to `*llm-claim-depot`; LLMTopology grants only if the run is
still pending.

```clojure
{:claim/id "claim-..."
 :llm-turn-run/id "run-1"
 :llm-thread/id "llm-thread-A"
 :executor/id "softland-local-codex-1"
 :executor/task-id 12
 :claim/token "uuid-token"
 :claimed-at-ms 1778400000200
 :routing/key [:llm-run "run-1"]}
```

### 3.6 LLM Observation Record

Executor writes every Codex app-server event to `*llm-obs-depot`.

```clojure
{:observation/id "obs-run-1-000042"
 :observation/type :codex/item-completed
 :observation/schema-version 1

 :routing/key [:llm-run "run-1"]
 :llm-turn-run/id "run-1"
 :llm-thread/id "llm-thread-A"
 :native/codex-thread-id "01..."
 :native/codex-turn-id "turn_..."

 :sequence 42
 :received-at-ms 1778400001234

 :codex/event-method "item/completed"
 :codex/event-params {...}
 :raw/json {...}}
```

Observation branch must buffer by `:sequence`. Never drop out-of-order
observations.

Codex approval requests are id-bearing JSON-RPC server requests, not ordinary
notifications. They are still recorded as observations so the blocked state is
durable and visible to the UI.

```clojure
{:observation/id "obs-run-1-000057"
 :observation/type :codex/approval-request
 :observation/schema-version 1

 :routing/key [:llm-run "run-1"]
 :llm-turn-run/id "run-1"
 :llm-thread/id "llm-thread-A"
 :sequence 57
 :received-at-ms 1778400001600

 :approval/id "approval-7"
 :approval/type :exec
 :native/json-rpc-request-id 44
 :codex/event-method "item/cmdExec/requestApproval"
 :codex/event-params {...}
 :raw/json {...}}
```

`$$llm-approvals-pending[approval-id]` must retain the native JSON-RPC request
id while the Codex child is alive. The executor replies to Codex with the same
native id. If the executor/Codex process dies before resolution, the approval is
marked stale and the run is failed/restarted; do not pretend a new process can
answer the old request id.

### 3.7 LLM Control Record

WorldTopology writes to `*llm-control-depot` for run control. User-initiated
control decisions still enter through `*world-action-depot` first.

```clojure
{:control/id "ctrl-..."
 :control/type :approval/resolve
 :routing/key [:llm-run "run-1"]
 :llm-turn-run/id "run-1"
 :approval/id "approval-7"
 :native/json-rpc-request-id 44
 :decision :approved
 :actor {:actor/id "sid"}
 :time-ms 1778400002000}
```

`:approval/id` is Softland's durable id. `:native/json-rpc-request-id` is the
Codex app-server request id that must be used for the live JSON-RPC response.
WorldTopology copies it from the pending approval row into the control record
so the executor never has to infer the mapping from UI state.

Control types:

```text
:turn/cancel
:turn/steer
:approval/resolve
:elicitation/resolve
:compact/request
:terminal/cleanup
```

## 4. PState Contracts

### 4.1 World PStates

```text
PSTATE                         KEY                     PURPOSE
-----------------------------  ----------------------  ----------------------
$$requests-by-id               request/id              durable request trace
$$decisions-by-id              decision/id             accept/reject trace
$$events-by-id                 event/id                accepted facts only

$$world-threads                world-thread/id         user-facing containers
$$world-thread-graph           world-thread/id         parent/fork/merge DAG
$$world-turns                  world-turn/id           one user move
$$world-turns-by-thread        world-thread/id         ordered turn ids

$$context-bundles              context-bundle/id       immutable model inputs
$$context-bundles-by-turn      world-turn/id           turn -> bundle

$$objects                      object/id               citeable catalog
$$artifact-graph               object/id               typed outgoing edges
$$artifact-graph-in            object/id               typed incoming edges

$$slices                       slice/id                snapshot + source hash
$$overlays                     overlay/id              comments/bookmarks/etc.
$$world-derivatives            derivative/id           edited/curated versions

$$patch-proposals              patch/id                pending LLM patches
$$activity-timeline            world-thread/id         ordered user-visible
                                                       feed of WorldTurns,
                                                       run status markers,
                                                       proposals, accepts,
                                                       rejects, and forks
```

WorldTopology owns all of these writes.

### 4.2 LLM PStates

```text
PSTATE                         KEY                     PURPOSE
-----------------------------  ----------------------  ----------------------
$$llm-threads                  llm-thread/id           Softland LLMThread
$$llm-thread-by-world-thread   world-thread/id         current binding
$$llm-thread-graph             llm-thread/id           native fork lineage

$$llm-turn-runs                llm-turn-run/id         lifecycle truth
$$llm-turn-runs-by-thread      llm-thread/id           ordered runs
$$llm-turn-run-by-world-turn   world-turn/id           send turn -> run

$$llm-decisions-by-run-id      llm-turn-run/id         LLM accept/reject
$$llm-pending-by-task          executor/task-id        executor inbox

$$llm-items-by-turn-run        llm-turn-run/id         ordered raw items
$$llm-items-by-thread          llm-thread/id           full raw history index
$$llm-item-by-id               llm-item/id             item lookup
$$llm-raw-response-items       llm-turn-run/id         literal API output

$$llm-tool-calls-by-run-id     llm-turn-run/id         exec/patch/mcp/web/etc.
$$llm-approvals-pending        approval/id             blocked server requests
$$llm-approvals-by-run-id      llm-turn-run/id         approval trail

$$llm-token-usage-by-run-id    llm-turn-run/id         tokens/quota/cache
$$llm-cost-by-thread           llm-thread/id           rollups
$$llm-views                    llm-turn-run/id         bounded UI stream view
```

LLMTopology owns all of these writes.

### 4.3 Projection PStates

Projection PStates can be fed from world and LLM PStates, but remain derived.

```text
PSTATE                         KEY                     PURPOSE
-----------------------------  ----------------------  ----------------------
$$chat-canvas-view             world-thread/id         composed user view
$$run-detail-view              llm-turn-run/id         live trail view
$$object-detail-view           object/id               catalog detail
$$discourse-graph              object/id               typed projection of
                                                       artifact graph
```

Projection PStates must be rebuildable from depot history and canonical
PStates. They are never source truth.

## 5. ContextBundle Contract

ContextBundle is the bridge from user-side composition to model-side input.

```clojure
{:context-bundle/id "B-4"
 :context-bundle/schema-version 1
 :context-bundle/status :frozen
 :created-at-ms 1778400000000

 :world-thread/id "chat-A"
 :world-turn/id "WT-4"
 :llm-thread/id "llm-thread-A"

 :input/hash "sha256:..."
 :input/token-estimate 12654

 :execution/options
 {:agent/kind :codex
  :model "gpt-5.2-codex"
  :approval-policy :on-request
  :sandbox :workspace-write
  :cwd "/mnt/data/projects/Softland"
  :native/thread-action :resume-existing}

 :source/refs
 [{:kind :draft
   :draft/id "draft-4"
   :role :user-authored}

  {:kind :raw-llm-item-ref
   :llm-turn-run/id "run-1"
   :llm-item/id "item-I7"
   :content-hash "sha256:raw-item"
   :source-role :assistant}

  {:kind :slice
   :slice/id "S-1"
   :source {:llm-turn-run/id "run-1"
            :llm-item/id "item-I7"
            :span [142 281]
            :content-hash "sha256:raw-item"}
   :snapshot/hash "sha256:slice-text"
   :role :user-selected-material}

  {:kind :overlay
   :overlay/id "C-1"
   :role :user-comment}

  {:kind :derivative
   :derivative/id "D-1"
   :role :user-authored-replacement}]

 :rendered
 {:codex/user-input
  [{:type "text"
    :text "... exact text sent in turn/start ..."}]

  :system/instructions
  [{:source :softland
    :text "..."}]}

 :render/policy
 {:raw-items :quote-by-reference-with-hash
  :overlays :render-as-user-authored
  :derivatives :render-instead-of-source-when-selected
  :slices :render-snapshot-text
  :comments :never-render-as-assistant-authored}}
```

Rules:

```text
1. Immutable after :status :frozen.
2. Every LLM-triggering WorldTurn has exactly one frozen bundle in V0.
3. Every LLMTurnRun references exactly one frozen bundle.
4. User edits/comments are always marked user-authored.
5. Slice carries snapshot text/hash, not only span ref.
6. Raw item refs carry source content hash to detect drift.
7. The model sees rendered input, not Softland's internal artifact objects.
8. Execution options that affect Codex behavior are bundle-owned.
```

## 6. Artifact Contracts

### 6.1 Raw LLM Item

```clojure
{:llm-item/id "item-I7"
 :llm-turn-run/id "run-1"
 :llm-thread/id "llm-thread-A"
 :native/item-id "..."
 :item/type :assistant-message
 :item/order 7
 :content/text "..."
 :content/hash "sha256:..."
 :source :codex
 :created-at-ms 1778400009999}
```

Raw item is immutable. User edits never mutate it.

### 6.2 Slice

```clojure
{:slice/id "S-1"
 :source {:llm-turn-run/id "run-1"
          :llm-item/id "item-I7"
          :span [142 281]
          :content-hash "sha256:raw-item"}
 :snapshot/text "selected text at slice time"
 :snapshot/hash "sha256:slice-text"
 :created-by {:actor/id "sid"}
 :created-in {:world-thread/id "chat-A"
              :world-turn/id "WT-2"}}
```

Slice is a world artifact. It survives compaction and source-render changes.

### 6.3 Overlay

```clojure
{:overlay/id "C-1"
 :overlay/type :comment
 :role :user-comment
 :target {:object/id "obj-item-I7"
          :llm-item/id "item-I7"}
 :content/text "actually this is the interesting part"
 :created-in {:world-turn/id "WT-3"}}
```

Overlay travels alongside a source. It does not replace the source.

### 6.4 Derivative

```clojure
{:derivative/id "D-1"
 :derivative/type :edited-message
 :role :user-authored-replacement
 :derivative-of {:object/id "obj-item-I7"
                 :llm-item/id "item-I7"
                 :content-hash "sha256:raw-item"}
 :content/text "user-authored edited version"
 :created-in {:world-turn/id "WT-3b"}}
```

Derivative is an alternate artifact. A future ContextBundle may render it
instead of the raw source, but the raw source stays immutable.

### 6.5 Catalog Promotion Policy

Not every raw LLM item needs an eager `$$objects` row. The raw execution trace
lives in LLM PStates first. `$$objects` is the citeable world catalog.

Working default:

```text
EAGER CATALOG
  WorldThread
  WorldTurn
  ContextBundle
  LLMTurnRun summary object
  world artifacts: slice / overlay / derivative / patch proposal

LAZY CATALOG
  individual raw LLM items
  tool calls
  reasoning blocks
  stdout/stderr chunks
```

A raw LLM item is promoted into `$$objects` when it becomes public material:

```text
  user cites it
  user slices it
  user comments on it
  user creates a derivative from it
  another artifact graph edge targets it
  UI explicitly bookmarks/promotes it
```

Promotion is atomic with the world action that needs the object. For example,
`:world-turn/slice-create` promotes the source raw item if needed, creates the
slice object, and creates the graph edge in the same accepted decision.

This is a working default, not a deep ontology claim. See section 13 for the
remaining catalog policy decision.

## 7. Routing Keys

```text
RECORD FAMILY                       ROUTING KEY
----------------------------------  ------------------------------------------
world compose/send                  [:world-thread world-thread-id]
world slice/comment/derivative      [:world-thread owning-world-thread-id]
world patch accept/reject           [:world-thread owning-world-thread-id]
world approval/compact/cancel       [:world-thread owning-world-thread-id]
world fork-create                   [:world-thread parent-world-thread-id]
world reconcile-create              [:world-thread synthesis-world-thread-id]
world proposal from LLM             [:world-thread owning-world-thread-id]

llm turn-run request                [:llm-run llm-turn-run-id]
llm claim                           [:llm-run llm-turn-run-id]
llm observation                     [:llm-run llm-turn-run-id]
llm control                         [:llm-run llm-turn-run-id]
executor pending inbox              executor/task-id PState partition
```

Reasoning:

```text
World user moves need thread-local ordering.
LLM execution events need run-local ordering.
Executor pending inbox needs task-local discovery.
Request id is identity and idempotency, not locality.
```

Cross-thread reconciliation:

```text
The request routes to the new synthesis WorldThread. Parent content must be
captured as snapshots/refs in the ContextBundle. Do not require synchronous
multi-parent locking to build a bundle; preserve parent hashes and detect drift.
```

## 8. Topology Branches

### 8.1 WorldTopology

```text
BRANCH                  SOURCE                  WRITES
----------------------  ----------------------  -----------------------------
REQUEST                 *world-action-depot     requests/decisions/events
                                                world threads/turns
                                                slices/overlays/derivatives
                                                context bundles
                                                objects/artifact graph

LLM-PROPOSAL            *world-action-depot     patch proposals, note
                                                proposals, catalog updates

PROJECTION              world + llm PStates     chat-canvas view,
                                                object views, discourse graph
```

The REQUEST branch:

```text
1. Validate envelope shape and idempotency.
2. Read target-local PStates.
3. Accept/reject with ActionDecision.
4. If accepted, emit KernelEvent(s).
5. Materialize World PStates.
6. For LLM-triggering WorldTurns, freeze ContextBundle.
7. Foreign-append llm-turn-run-request to *llm-depot with append ack.
```

### 8.2 LLMTopology

```text
BRANCH                  SOURCE                  WRITES
----------------------  ----------------------  -----------------------------
REQUEST                 *llm-depot              llm decisions, threads,
                                                turn-runs, pending-by-task

CLAIM                   *llm-claim-depot        grant claim if pending;
                                                remove from pending inbox

OBS                     *llm-obs-depot          items, tool calls, approvals,
                                                token usage, live views,
                                                run lifecycle

CONTROL                 *llm-control-depot      cancel/approval/steer/
                                                compact state

WORLD-BRIDGE            derived from OBS        foreign-append proposals and
                                                catalog progress to world
```

The OBS branch must use retry/all-after style semantics and sequence buffering.
The stream is high-volume and append-heavy; correctness beats dropping deltas.

## 9. Lifecycle Contracts

### 9.1 Fresh Run

```text
UI
 | append ActionRequest :world-turn/compose-and-send
 v
*world-action-depot
 | WorldTopology accepts
 | creates chat-A, WT-1, B-1, pending $$objects entry
 | foreign-append {:llm/turn-run-request run-1 B-1}
 v
*llm-depot
 | LLMTopology accepts
 | creates llm-thread-A, run-1 :pending, pending-by-task
 v
LLMExecutor
 | claim -> durable grant
 | codex initialize/thread-start/turn-start with B-1 rendered input
 | stream observations
 v
*llm-obs-depot
 | LLMTopology folds raw items/tool calls/token usage
 | closes run-1
 | foreign-append world catalog/proposals
 v
WorldTopology
 | updates $$objects / $$artifact-graph / pending proposals
```

### 9.2 Follow-Up

```text
Existing: chat-A -> llm-thread-A -> native/codex-thread-id

UI sends :world-turn/compose-and-send
WorldTopology creates WT-N + B-N
WorldTopology foreign-appends run-N request
LLMTopology creates new LLMTurnRun run-N on existing LLMThread
Executor sends turn/start to existing Codex threadId
```

Not allowed:

```text
"append turn 2 to run 1"
```

Correct:

```text
"create run 2 in the same LLMThread"
```

### 9.3 Slice/Edit

```text
UI -> *world-action-depot
WorldTopology -> WorldTurn + slice/overlay/derivative + object/edge
No ContextBundle
No LLMTurnRun
Raw LLM item unchanged
```

### 9.4 Fork From Span

```text
UI selects span
WorldTopology creates or reuses Slice S-1 with snapshot/hash
WorldTopology creates child WorldThread chat-B + WT-1 + B-1
B-1 quotes S-1 snapshot as user-selected material
LLM request includes {:codex-fork {:from-thread native-thread-A}}
Executor sends thread/fork {threadId native-thread-A, ...config}
Executor appends fork-completed observation with the new native thread id
LLMTopology records native thread id on child LLMThread
Executor observes the append-ack / materialized PState binding
Executor sends turn/start with B-1 rendered input
```

Codex fork is thread-level only in `codex-cli 0.130.0` generated schema:

```text
thread/fork params = threadId + config overrides
no fromItemId
no excludeTurns
```

The ordering matters. The executor must not send `turn/start` on the new native
thread until the fork-completed observation has been durably appended and the
child LLMThread binding is visible in PState. Otherwise an executor crash
between fork and first turn can lose the mapping between Softland's child thread
and Codex's native thread.

### 9.5 Approval

```text
Codex emits approval server request
Executor appends observation to *llm-obs-depot
LLMTopology writes $$llm-approvals-pending
UI renders approval row from PState
User decides
UI appends *world-action-depot :world-turn/tool-approval-resolve
WorldTopology records WorldTurn + durable user decision
WorldTopology foreign-appends *llm-control-depot :approval/resolve
LLMTopology records run-control decision
Executor reads/resolves and replies to Codex JSON-RPC request id
```

Approval means:

```text
allowed agent to attempt tool action
```

It does not mean:

```text
accepted resulting patch/artifact into World truth
```

Patch acceptance is a later WorldTurn.

### 9.6 Cancel

```text
UI -> *world-action-depot :world-turn/cancel
WorldTopology records WorldTurn WT-cancel
WorldTopology foreign-appends *llm-control-depot :turn/cancel
LLMTopology marks run :cancel-requested
Executor sends turn/interrupt to Codex
OBS closes run :cancelled when Codex confirms
```

### 9.7 Compaction

```text
UI -> *world-action-depot :world-turn/compact-request
WorldTopology records WorldTurn WT-compact
WorldTopology foreign-appends *llm-control-depot :compact/request
LLMTopology records control decision on target LLMThread/run
Executor sends Op::Compact / thread compact request to Codex
Codex emits ContextCompacted
Executor appends compaction observation to *llm-obs-depot
LLMTopology records compaction marker on the thread/run
```

Raw Softland trail remains in depots/PStates. Future ContextBundles may cite
pre-compaction raw items because Softland retained them, even if the Codex
native thread only sees the summary.

### 9.8 Reconciliation

```text
UI selects parent WorldThreads chat-B and chat-C
UI appends *world-action-depot :world-turn/reconcile-create
WorldTopology creates synthesis WorldThread chat-S
WorldTopology records parent edges chat-B -> chat-S and chat-C -> chat-S
WorldTopology creates WorldTurn WT-1 in chat-S
WorldTopology freezes ContextBundle B-1 aggregating parent snapshots/refs
WorldTopology foreign-appends *llm-depot {:llm/turn-run-request run-S-1 B-1}
LLMTopology creates a fresh LLMThread for chat-S
Executor sends thread/start + turn/start with B-1 rendered input
OBS records raw synthesis output
```

Codex has no multi-parent native fork. Reconciliation is a new Softland
WorldThread with multi-parent world edges and a fresh native LLMThread.
Parents stay intact; synthesis is a new artifact, not destructive merge.

### 9.9 Patch Acceptance

```text
LLMTurnRun produces TurnDiff
LLMTopology records raw diff under the run
LLMTopology foreign-appends *world-action-depot :llm/proposal/patch-create
WorldTopology creates pending patch proposal object
UI renders proposal in review queue
User clicks Accept
UI appends *world-action-depot :world-turn/patch-accept
WorldTopology validates proposal/current workspace state
WorldTopology accepts or rejects with ActionDecision
If accepted, WorldTopology records patch-applied event + object/graph edges
```

Tool approval and patch acceptance are separate:

```text
approval lets Codex attempt a file-change tool
patch-accept makes the produced patch part of World truth
```

## 10. Cost And Cache Contract

Store token/quota facts as observations, not as estimates hidden in UI code.

```clojure
{:llm-turn-run/id "run-1"
 :tokens/input-total 50000
 :tokens/cached-input 45000
 :tokens/output 2000
 :tokens/reasoning-output 8000
 :model/context-window 1050000
 :subscription/messages-used 1
 :billing/mode :chatgpt-subscription}
```

Current external constraints checked 2026-05-10:

```text
previous_response_id:
  client-side state convenience; official docs say previous input tokens
  in the chain are still billed as input tokens in the API.

prompt caching:
  automatic for eligible matching prefixes; cached tokens reduce price and
  latency but still count as input tokens/rate-limit usage.
```

Therefore:

```text
Do not store "inherited context is free" anywhere.
Track full input, cached input, output, reasoning output, and subscription
message quota as separate fields.
```

## 11. Invariants

These are the lines the implementation must not cross.

```text
I1.  UI never appends directly to *llm-depot for user sends.
I1a. UI never appends directly to *llm-control-depot for user decisions.
I2.  Every LLM-triggering run has a prior accepted WorldTurn.
I3.  Every LLMTurnRun references exactly one frozen ContextBundle.
I4.  ContextBundle exists before *llm-depot receives the run request.
I5.  Raw LLM items are immutable.
I6.  User edits/comments never mutate raw LLM items.
I7.  Slice stores snapshot text/hash plus source content hash.
I8.  Overlays/derivatives render as user-authored, never assistant-authored.
I9.  World-only WorldTurns do not create ContextBundles or LLMTurnRuns.
I10. WorldTopology is the only writer of $$objects and $$artifact-graph.
I11. LLMTopology is the only writer of LLM PStates.
I12. Executor never writes PStates; it reads PStates and writes depots.
I13. LLM observations are folded in sequence; out-of-order obs are buffered.
I14. Tool approval does not equal world acceptance of produced artifacts.
I15. Codex native fork semantics are treated as thread-level only unless the
     installed schema proves otherwise.
I16. Rejected ActionRequests remain queryable as ActionDecisions.
I17. Idempotency key replay returns the same decision/result shape.
I18. Projection views are rebuildable from depot/PState truth.
```

## 12. Contract Tests

Tests should be named so misunderstanding the contract causes a visible
failure.

```text
world-first-send-test
  sending a prompt appends to *world-action first; *llm-depot receives only
  a WorldTopology-derived request.

context-bundle-before-run-test
  $$context-bundles[B] and $$objects[pending-run] exist before run status
  becomes :pending in $$llm-turn-runs.

one-bundle-per-run-test
  every LLMTurnRun references exactly one frozen ContextBundle, and no
  LLM-triggering WorldTurn has multiple frozen bundles in V0.

accepted-decision-fields-test
  accepted world decisions include request id/type, routing key, primary
  event id, sibling event ids, and decided-at timestamp.

follow-up-new-run-same-thread-test
  follow-up creates a new llm-turn-run/id and reuses the same LLMThread /
  native Codex thread id.

slice-raw-immutability-test
  slice creates $$slices + graph edge; $$llm-item-by-id source content is
  unchanged.

slice-source-hash-test
  slice records snapshot text/hash and the source raw item's content hash;
  later source drift is detectable without losing the slice.

derivative-renders-as-user-authored-test
  edited derivative included in a later ContextBundle is tagged as
  user-authored replacement, not assistant history.

world-only-turn-no-llm-side-effect-test
  slice/comment/derivative/patch-accept/cancel WorldTurns do not create
  ContextBundles or LLMTurnRuns unless explicitly defined as LLM-triggering.

fork-span-softland-anchor-test
  fork from a span creates a slice snapshot and child WorldThread; Codex
  fork request contains threadId only, not fromItemId.

fork-binding-before-turn-start-test
  executor waits for durable fork-completed append/PState binding before
  sending turn/start on the new native Codex thread.

approval-world-first-test
  approving apply_patch creates a world-side approval WorldTurn, then an
  LLM control record. UI does not write *llm-control-depot directly.

approval-is-not-patch-acceptance-test
  approving apply_patch lets Codex continue but does not mark the resulting
  patch accepted in World PStates.

obs-sequence-buffer-test
  out-of-order observations are buffered and later drained, not dropped.

cancel-world-first-test
  cancel creates a WorldTurn and then LLM control; executor interrupt is
  downstream of durable state.

compaction-world-first-test
  compaction creates a world-side compact-request WorldTurn before LLM
  control; UI does not write compact control directly.

idempotency-test
  replaying the same request/idempotency key does not mint duplicate
  WorldTurns, ContextBundles, or LLMTurnRuns.

rejected-request-trace-test
  invalid target produces ActionDecision :rejected and no KernelEvent.

cost-cache-fields-test
  token usage stores total input and cached input separately.

pstate-writer-asymmetry-property-test
  replaying depot history produces the same PStates as live processing, and
  no UI/executor path can mutate PStates outside topology code.

projection-rebuildability-test
  chat canvas, run detail, object detail, and discourse graph views can be
  rebuilt from canonical PStates/depot history.
```

## 13. Remaining Decisions

These remain Sid-level choices, not hidden implementation defaults.

```text
1. ContextBundle physical home
   Working default: WorldTopology freezes and stores $$context-bundles.
   Alternative: dedicated *context-bundle-depot.

2. World derivatives organization
   Working default: one $$world-derivatives PState with typed rows.
   Alternative: separate PStates by derivative kind.

3. Derivative versioning
   Working default: append-only versions; latest pointer is a projection.
   Alternative: single mutable derivative artifact.

4. LLMThread cardinality
   Working default: one active Codex LLMThread per WorldThread per agent kind.
   Alternative: multiple comparable LLMThreads under one WorldThread for
   model comparison/reruns.

5. Raw item catalog promotion policy
   Working default: lazy promotion when a raw item is cited/sliced/commented/
   derived/bookmarked.
   Alternative: eager $$objects row for every raw item and tool call.
```

## 14. What This Doc Is Not

```text
Not a UI spec.
Not a full Codex protocol mirror.
Not a schema generator.
Not a replacement for llm-track-canonical.md.
Not a claim that every PState listed must ship in Slice 1.
```

This doc is the contract layer the MVP can slice from. A smaller slice may
implement fewer request types and PStates, but it must not violate the
invariants above.
