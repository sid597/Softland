 # Softland Strange-Loop Runtime v1 Plan (Decision-Complete)

  ## Summary

  Build a Rama-native, persistent multi-agent runtime that treats each interaction as typed reasoning artifacts
  (not chat), rendered in a new Inquiry Arena in WebGPU.
  This plan is aligned to your chosen decisions:

  - Runtime: Rama-native core
  - Execution: persistent sessions for all providers if possible
  - First UI: Inquiry Arena
  - Acceptance bar: typed loop end-to-end
  - Command scope: raw command passthrough
  - Workspace scope: any local path
  - Safety controls: no enforced controls
  - Reasoning schema: hybrid mapping (Softland typed moves + QUE/CLM/EVD/SRC index)

  ———

  ## Scope (In/Out)

  ### In scope (v1)

  - Enter in command panel creates typed inquiry/run events.
  - Persistent provider session management (best-effort per provider).
  - Raw command passthrough execution via provider adapters.
  - Typed move extraction + persistence + graph linkage.
  - Inquiry Arena rendering (3 lanes + contradiction panel + decision controls).
  - End-to-end async flow with non-blocking editing.

  ### Out of scope (v1)

  - Strong runtime sandboxing/policy enforcement.
  - Full category-theoretic operators UI.
  - External cloud relay orchestration.
  - Automatic formal proof/witness generation (manual/heuristic witness allowed).

  ———

  ## Architecture

  ## 1) Core Model (authoritative schema)

  ### 1.1 Primary entities

  - Inquiry: problem container (goal, context, open tensions, status)
  - Run: one execution against a provider/session
  - Move: typed reasoning unit emitted by human/agent/system
  - Decision: commit/fork/defer/reject on inquiry branch
  - Tension: explicit contradiction/uncertainty relation between moves

  ### 1.2 Typed move taxonomy (Softland primary)

  - :inquiry
  - :claim
  - :evidence
  - :counterexample
  - :plan
  - :patch
  - :test-result
  - :perf-result
  - :decision
  - :unknown

  ### 1.3 DiscourseGraph mirror taxonomy (secondary index)

  - QUE, CLM, EVD, SRC
  - Mapping:
      - :inquiry -> QUE
      - :claim/:counterexample -> CLM
      - :evidence/:test-result/:perf-result -> EVD
      - file/commit/url/session refs -> SRC

  ———

  ## 2) Rama changes (exact)

  ### 2.1 New depot

  - *agent-events-depot (hash-by :run-id)
    Rationale: isolate agent event throughput from *node-events-depot.

  ### 2.2 New PStates

  - $$inquiries-pstate
      - {inquiry-id -> {:title :status :created-at :updated-at :origin :branch-id :tags}}
  - $$runs-pstate
      - {run-id -> {:inquiry-id :provider :status :cwd :argv :started-at :ended-at :exit-code :session-id}}
  - $$provider-sessions-pstate
      - {provider -> {session-key -> {:session-id :mode :last-active :metadata}}}
  - $$moves-pstate
      - {inquiry-id -> [move-id ...]} + $$move-by-id-pstate
  - $$tensions-pstate
      - {inquiry-id -> [{:a-move-id :b-move-id :kind :status :created-at} ...]}
  - $$decisions-pstate
      - {inquiry-id -> [{:decision :by :at :rationale :links} ...]}
  - $$dg-index-pstate
      - {move-id -> {:dg-node-id :dg-type}} (hybrid mapping bridge)

  ### 2.3 New event actions

  - :inquiry/open
  - :run/requested
  - :run/started
  - :run/chunk
  - :run/completed
  - :run/failed
  - :run/cancelled
  - :move/add
  - :tension/add
  - :decision/add
  - :dg/index-upsert

  ### 2.4 Topology behavior

  - On :run/requested:
      - persist run row
      - resolve provider session strategy
      - append :run/started
      - spawn async execution
  - On stream output:
      - append :run/chunk events
      - parse chunks -> candidate typed moves
      - append :move/add events
  - On terminal state:
      - append :run/completed or :run/failed
      - update session registry
  - On move add:
      - mirror to DG index (QUE/CLM/EVD/SRC)
      - optionally add DG node/edge events to existing graph states

  ———

  ## 3) Provider runtime (persistent if possible)

  ## 3.1 Adapter interface (new namespace)

  src/app/server/agent/providers.clj

  Contract:

  - resolve-provider(provider-key) -> adapter
  - adapter methods:
      - supports-persistent?
      - start-or-resume!(session, request)
      - send!(session, request)
      - read-chunks!(session, on-chunk, on-end, on-error)
      - close!(session)

  ## 3.2 Execution mode policy

  - Global policy: persistent for all providers if capability exists
  - If provider cannot satisfy persistent mode at runtime:
      - fallback to one-shot for that run
      - persist fallback reason in run metadata
  - Raw passthrough:
      - UI sends argv and cwd directly
      - provider adapter validates only minimal shape (non-empty argv, string cwd)

  ## 3.3 Session key strategy

  session-key = sha256(provider + cwd + inquiry-id + optional branch-id)

  ———

  ## 4) HTTP + Electric interfaces

  ## 4.1 HTTP routes (extend src/app/server_jetty.clj)

  - POST /api/agent/inquiry/open
  - POST /api/agent/run
  - POST /api/agent/decision
  - POST /api/agent/cancel
  - GET /api/agent/inquiry/:id/snapshot (optional debug/bootstrap)

  Payload policy:

  - EDN body
  - raw argv allowed
  - raw cwd allowed

  ## 4.2 Electric bridge

  src/app/electric_flow.cljc

  - Add server-side subscriptions for:
      - inquiry snapshot
      - run stream
      - move/tension/decision updates
  - Feed a shared client atom injected into start-loop!:
      - !agent-state (single source for arena rendering)
  - Do not create independent AI atoms outside loop ownership.

  ———

  ## 5) Client/UI changes (Inquiry Arena first)

  ## 5.1 Command panel behavior (`src/app/client/workspace/runtime.cljs` + `src/app/client/workspace/cmd_panel.cljs`)

  - Replace Enter stub with intent emission:
      - If no active inquiry: create :inquiry/open
      - Trigger 3 parallel runs by default (Claude/Codex/Gemini)
      - Request payload includes raw argv, cwd, and context snapshot

  ### Context snapshot fields

  - :file-path
  - :cursor ({:line :col})
  - :selection
  - :visible-range
  - :fold-state
  - :timestamp

  ## 5.2 New local state atoms

  - !active-inquiry-id
  - !agent-state (mirrors Electric/Rama stream)
  - !arena-layout
  - !provider-run-status
  - !selected-move-id
  - !decision-draft

  ## 5.3 Arena rendering

  Add token generation layer into <combined-text-ops:

  - Lane A/B/C: provider outputs
  - Contradiction panel: active tensions
  - Decision rail: commit | fork | defer | reject
  - Move badges: type (claim, evidence, etc.)
  - DG mapping indicators (QUE/CLM/EVD/SRC chip)

  ———

  ## 6) Typed extraction pipeline

  ## 6.1 Parser stages

  1. Chunk collector per run
  2. Segmenter (newline + markdown/code-fence aware)
  3. Move classifier (heuristic rules first)
  4. Linker (attach move to sources/context)
  5. Tension detector (claim vs counterexample/evidence mismatch)

  ## 6.2 Storage rules

  - Keep raw chunk log always.
  - Store parsed moves as separate append-only records.
  - Never overwrite parsed moves; corrections are new moves.

  ———

  ## 7) File-by-file implementation plan

  1. src/app/server/rama/objects.cljc

  - Add CliProcessTaskGlobal + process lifecycle helpers
  - Add stream reader utilities (stdout/stderr -> chunk callbacks)

  2. src/app/server/rama/core.clj

  - Declare *agent-events-depot
  - Declare new PStates
  - Add full :run/*, :move/*, :tension/*, :decision/* cases
  - Wire DG hybrid index updates

  3. src/app/server/rama/util_fns.cljc

  - Foreign bindings for new pstates/depot
  - Public append helpers:
      - open-inquiry!
      - request-run!
      - add-decision!
      - cancel-run!
  - Subscription helpers for inquiry/run/moves/tensions

  4. src/app/server/agent/providers.clj (new)

  - Adapter registry and capability discovery
  - Provider-specific arg/session handling
  - Persistent fallback semantics

  5. src/app/server_jetty.clj

  - Add /api/agent/* routes
  - EDN body parsing + response envelopes
  - Keep existing file APIs unchanged

  6. src/app/electric_flow.cljc

  - Create shared !agent-state
  - Server subscriptions -> client state propagation
  - Pass !agent-state into start-loop!

  7. src/app/client/workspace/runtime.cljs

  - Replace Enter stub flow
  - Add Inquiry Arena interaction handlers
  - Add token generation for lanes/tensions/decisions

  8. src/app/client/substrate/webgpu/renderer.cljs

  - Reuse existing pipeline; only add style/theming primitives for arena elements if needed

  ———

  ## 8) Public/API contracts

  ## 8.1 POST /api/agent/run request

  {:run-id "uuid"
   :inquiry-id "uuid"
   :provider :claude|:codex|:gemini
   :cwd "/any/path"
   :argv ["claude" "-p" "..." "--output-format" "stream-json"]
   :context {:file-path "...", :cursor {:line 10 :col 4}, :selection nil, :visible-range [0 1200], :fold-state
#{}, :timestamp 1738800000000}}

  ## 8.2 Run event envelope

  {:action-type :run/chunk|:run/completed|:run/failed
   :run-id "uuid"
   :inquiry-id "uuid"
   :provider :claude
   :ts 1738800000000
   :payload {...}}

  ## 8.3 Move record

  {:move-id "uuid"
   :inquiry-id "uuid"
   :run-id "uuid|nil"
   :type :claim|:evidence|:counterexample|...
   :content "..."
   :links {:sources [...], :files [...], :runs [...]}
   :dg {:type :CLM|:EVD|:QUE|:SRC}
   :created-at 1738800000000}

  ———

  ## 9) Testing plan

  ## 9.1 Unit tests

  - Provider adapter capability resolution.
  - Session-key generation determinism.
  - Chunk parser -> move classifier mapping.
  - DG hybrid mapping correctness.

  ## 9.2 Rama topology tests

  - :run/requested creates run row + started state.
  - streamed chunks append and emit moves.
  - completion/failure transition correctness.
  - tension insertion on conflicting moves.
  - decision append immutability.

  ## 9.3 Integration tests

  - Enter -> inquiry open -> 3 provider runs started.
  - non-blocking editor interaction during run.
  - persistent session resume on second run same session-key.
  - fallback to one-shot when persistent unsupported.

  ## 9.4 UI behavior tests

  - Arena renders three lanes and statuses.
  - contradiction panel updates live.
  - decision controls append correct events.
  - move type badges and DG chips visible.

  ## 9.5 Acceptance scenarios (must pass for v1)

  1. User opens inquiry, keeps editing, receives async responses in arena.
  2. At least one claim and one evidence move persisted and linked.
  3. At least one contradiction shown in tension panel.
  4. User makes a decision (commit/fork/defer/reject) and it is persisted/replayable.
  5. Inquiry snapshot reload reconstructs arena state deterministically.

  ———

  ## 10) Rollout plan

  1. Phase A: Backend kernel

  - depot/pstates/actions/adapters + tests

  2. Phase B: Wiring

  - routes + Electric subscriptions + shared state path

  3. Phase C: Arena UI

  - lane rendering + tension/decision controls

  4. Phase D: Typed extraction

  - heuristic parser + DG indexing

  5. Phase E: Stability

  - replay checks + perf checks + large-output behavior

  ———

  ## 11) Assumptions and defaults (locked)

  - Raw command passthrough is allowed in v1.
  - Any local cwd is allowed in v1.
  - No mandatory runtime safety controls are enforced in v1.
  - Persistent sessions are attempted for all providers; fallback is allowed when unsupported.
  - Hybrid reasoning schema is authoritative (Softland moves + DG mirror index).
  - Existing editor render pipeline remains the only rendering backend (no DOM transcript fallback).

  ———

  ## 12) Implementation reality update (what changed + what we learned)

  ## 12.1 What changed from the initial breakdown

  - The execution path moved from overloading `*node-events-depot` to a dedicated `*agent-events-depot` keyed by `:run-id`.
  - The first concrete runtime state landed as `$$agent-runs-pstate` keyed by run-id with request/result lifecycle data.
  - The client/server bridge currently uses explicit HTTP endpoints (`/api/agent/run`, `/api/agent/run-status`) instead of an Electric push bridge.
  - Command-panel semantics were expanded into a small control grammar: `/provider ...`, `/run ...`, and plain prompt mode.
  - Provider execution now supports two modes: raw argv passthrough and provider-default argv synthesis.

  ## 12.2 Reactive architecture lessons captured during implementation

  - Polling cannot be modeled as recursive imperative control flow; it behaves better as a Missionary flow (`make-interval-flow` + `<agent-poll-requests`) consumed in the existing DAG.
  - Side effects should stay at the perimeter (fetch/post helpers), with state transitions represented explicitly in atoms and event streams.
  - Reusing the existing text GPU pipeline was the correct choice: agent output becomes just another token source in `<combined-text-ops`.
  - Backpressure/race control is required even in polling mode; an explicit in-flight gate (`!agent-poll-inflight`) prevented overlapping status fetches and state flapping.

  ## 12.3 Architecture principles now locked for this track

  - Separate event throughput domains by concern (graph/node vs agent runtime) to preserve composability and future scaling.
  - Use immutable run identity (`run-id`) as the primary join key across depot events, pstate rows, and UI status.
  - Keep run lifecycle explicit and monotonic (`:submitting` -> `:running` -> `:complete|:failed`) so UI and topology remain replayable.
  - Treat provider capability differences as adapter behavior, not editor behavior.

  ## 12.4 Gaps identified (still open)

  - No server-push path yet from Rama PState to client; status is currently polled.
  - Persistent provider session registry/resume policy is only partially represented in runtime state.
  - Typed move extraction, tension creation, decision persistence, and DG indexing were not implemented in this slice.
  - Streaming chunk-level updates and cancellation semantics are not implemented yet.

  ## 12.5 Implementation-phase insight that changed prioritization

  - The most valuable immediate hardening is not more command syntax; it is replacing polling with a true reactive server->client stream from run state updates.
  - The second priority is chunked/streamed run outputs so the inquiry surface can reason over intermediate states, not just terminal output.
  - The third priority is formalizing provider capability negotiation (supports resume? supports raw argv? expected output mode?) as first-class adapter metadata.

  ———

  ## 13) Tech Lead Review (claude-3)

  ### 13.1 Codex's 6 Points — Scored

  **1. Separate `*agent-events-depot` — KEEP** ✅
  Correct architectural separation. Agent events have different throughput patterns than graph CRUD. Hash-by :run-id is right for agent runs. Prevents contention.

  **2. Don't slurp in one shot — CORRECT ADVICE, NOT YET IMPLEMENTED**
  `run-cli-process` still does `(slurp (.getInputStream proc))`. Fine for v1. Streaming chunks is a known gap the architecture supports (add :run/chunk events later).

  **3. Fix context fields — KEEP** ✅
  `(:cursor doc)` captures the full cursor map (correct). `(:height viewport)` not `:h` (correct). Original plan had bugs here.

  **4. Polling instead of Electric subscription — REVERT** ❌
  The most consequential architectural decision, and the wrong one.

  What polling added (~100 lines):
  - `make-interval-flow` (6 lines)
  - `<agent-poll-requests` (12 lines)
  - `poll-agent-run!` (28 lines)
  - `!agent-poll-inflight` gate atom
  - `!active-agent-run-id` tracking atom
  - Polling consumer in `m/join vector` (7 lines)
  - `/api/agent/run-status` GET endpoint (7 lines)

  Problems:
  - 350ms latency vs near-instant with Rama subscription
  - Continuous CPU load when idle (violates Session 16b optimization: 76% → 6%)
  - Codex itself said in 12.5: "The most valuable immediate hardening is replacing polling with a true reactive server→client stream"

  The concern about `e/Task` boundary is valid but the solution is wrong.
  Correct fix: create `!agent-output` in `electric_flow.cljc`, pass it to `start-loop!` as keyword arg (same pattern as `!sidebar-visible` and `!file-load-request`), feed it from Electric subscription.

  **Remove:** `make-interval-flow`, `<agent-poll-requests`, `poll-agent-run!`, `!agent-poll-inflight`, `!active-agent-run-id`, polling consumer, `/api/agent/run-status`.
  **Replace with:** ~15 lines of Electric subscription.

  **5. Route method + body guard — KEEP** ✅
  `parse-edn-body`, POST check, try/catch. Straightforwardly better engineering.

  **6. Typed moves from day 1 — PARTIALLY REVERT** ⚠️
  The plan exploded into 7 PStates, 12 event types, 5-stage extraction pipeline, and a multi-lane "Inquiry Arena." This is weeks of work. For first contact: Enter → Rama → CLI → response → screen.
  KEEP: `/provider` command, `[CLAUDE]>` prompt display. Cost nothing, nice UX.
  DEFER: typed moves, tensions, decisions, DG indexing, arena lanes.

  ### 13.2 Good Code to Keep

  | Change | File | Verdict |
  |---|---|---|
  | `run-cli-process` separate from `cli-exec-future` | objects.cljc | ✅ Cleaner separation |
  | `provider-default-argv` with session-id | objects.cljc | ✅ Correct shape |
  | Error handling in `cli-exec-future` (try/catch) | objects.cljc | ✅ Robust |
  | Separate `*agent-events-depot` + `$$agent-runs-pstate` | core.clj | ✅ Good separation |
  | `submit-agent-run` generating run-id | util_fns.cljc | ✅ Clean |
  | `parse-edn-body` + POST guard | server_jetty.clj | ✅ Better error handling |
  | `post-edn!` helper | loop.cljs | ✅ Needed |
  | `parse-agent-command` (/provider, /run, plain) | loop.cljs | ✅ Nice UX |
  | `[PROVIDER]>` in prompt | loop.cljs | ✅ Visual clarity |
  | Agent output panel with status colors | loop.cljs | ✅ Good rendering |

  ### 13.3 Architecture Issues to Fix

  **1. Polling → Electric subscription**
  Remove ~100 lines of polling infra. Wire Electric `e/server` subscription to Rama PState. Pass `!agent-output` atom into `start-loop!` from `electric_flow.cljc`.

  **2. Add session continuity PState for --resume**
  `$$agent-runs-pstate` is keyed by run-id (good for lifecycle). But can't query "what's the session for this file+provider?" without scanning all runs.
  Add: `$$cli-sessions-pstate {file-path → {provider → {:session-id :last-active}}}`.
  Two indexes into the same data: runs for status, sessions for continuity.

  **3. Wire --resume into the flow**
  On CLI completion: extract session-id from output, store in `$$cli-sessions-pstate`.
  On next command for same file+provider: look up session-id, pass `--resume`.
  This is THE killer feature — AI remembers previous conversations per file.

  **4. PState schema typing**
  `$$agent-runs-pstate {String (map-schema Keyword Object)}` is untyped. Should be `fixed-keys-schema` with explicit fields. Prevents silent corruption.

  **5. Remove max-output-lines 10 hardcoded truncation**
  Real CLI responses can be hundreds of lines. Use scrollable output panel or fold long responses.

  ### 13.4 Execution Plan

  1. Fix PState schema (add fixed-keys, add $$cli-sessions-pstate)
  2. Add --resume session-id storage in topology
  3. Rip out polling infra from loop.cljs
  4. Add Electric subscription bridge in electric_flow.cljc
  5. Pass !agent-output into start-loop! from electric_flow.cljc
  6. Reconnect rendering (keep Codex's output panel, feed from subscription atom)
  7. Test end-to-end: Enter → Rama → CLI → PState → Electric → WebGPU

  ### 13.5 What Gets Deferred

  - Typed move extraction pipeline (v2)
  - Multi-lane Inquiry Arena (v2)
  - Tension/decision persistence (v2)
  - Streaming chunk events (v2 — slurp is fine for now)
  - Provider capability negotiation (v2)
  - Category theory operators UI (v3+)
