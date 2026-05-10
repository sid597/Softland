# LLM Track / Codex Harness — Canonical (session 2026-05-10)

Status: comprehensive reference doc capturing the full context of the
Codex/LLM-track design conversation of 2026-05-10. Combines protocol
research, architecture v2 + breadth, the artifact reframe, cost/caching
analysis, scenario walk-throughs, and meta-disciplines.

This doc is intentionally long. Each section is self-contained enough that
an answer to a specific user question lives in one place. Use the Question
Index (§15) as a jump table.

## §0 How to read this doc

```text
   §1   Mental model           text-in/out + tools = agent
   §2   Codex protocol         surfaces, ops, events, wire format
   §3   Cost & caching         Layer 1 + Layer 2, forking economics
   §4   Forking semantics      DAG, anchors, reconciliation
   §5   Rama framing           depots = truth, asymmetry, partitioning
   §6   The artifact problem   3 roles, 4 classes, 2 views
   §7   The architecture       v2 + breadth: thin views + fat composite
   §8   Cross-track patterns   LLM↔World, LLM↔Compute, X vs Y
   §9   Lifecycle scenarios    9 concrete flows
   §10  Open decisions         4 Sid-decidable
   §11  Compressed concepts    preserved as breadth labels
   §12  Discipline & meta      communication, drift, spine vs breadth
   §13  Workflow               docs branch separation
   §14  Companions / lineage   related docs and skills
   §15  Question Index         maps user questions to sections
```

---

## §1 Mental model — what an "agent" actually is

```text
   LLM         = (history, tool_schemas) → next_message_OR_next_tool_call
   agent_loop  = while not done:
                   resp = LLM(history, tool_schemas)
                   if resp is final_message: done
                   if resp is tool_call:
                       result = execute(tool_call)
                       history.append(tool_result)
                       history.append(resp)        # the call itself
```

An agent is a stateful loop around a stateless LLM. Tools are JSON schemas
describing structured input/output. Multi-tool + this loop = "agent."
Codex, Claude Code, Cursor all share this shape; they differ in tool sets
and approval rules.

The conversation history is a **flat list of typed items** — not a
back-and-forth of "user said X, assistant said Y":

```text
   :user-message      "do this thing"
   :assistant-message "I'll start by reading the file"
   :tool-call         {tool:"read_file", args:{...}, id:"call_42"}
   :tool-result       {call_id:"call_42", content:"..."}
   :reasoning         "the file says X, so..."
   :assistant-message "Here's what I found..."
   :tool-call         {tool:"apply_patch", ...}
   :tool-result       {...}
   :assistant-message "Done. Summary: ..."   ← the "final reply"
```

The model sees the whole list on every inference. The "final reply" is
just the last assistant text item before the model stops emitting tool
calls. Every model is text-in/text-out at the bottom — even tool calls
are token-emitted JSON the runtime parses and intercepts.

### §1.1 Reply is informed-by tool calls, not built-from

The reply synthesizes from the entire history. There is no atomic mapping
"this sentence in the reply ← that tool call."

Recoverable from a Codex run, by strength:
- **Strong (protocol-guaranteed)**: ordered list of items with stable IDs
  (item_id, call_id, turn_id); model's reasoning often references tool
  results explicitly; `parsed_cmd` on every exec; `TurnDiff` (one
  canonical aggregated diff per turn); `RawResponseItem` (literal
  Responses-API output for replay).
- **Medium (heuristic)**: span-to-tool attribution by content overlap
  (useful for UI highlighting, not a contract).
- **Weak (lost)**: sentence-level provenance ("this sentence's ideas came
  from these specific tokens of these specific tool results"). Would
  require asking the model and treating the answer as self-report.

**Implication**: don't try to attribute spans of the reply to tool calls
programmatically. Preserve the trail; let the user navigate it. Sid's
"rediscovery over consumption" principle.

### §1.2 The three-party JSON-RPC dance

```text
   ┌─────────────┐    JSON-RPC     ┌─────────┐    HTTPS API    ┌──────────┐
   │  Softland   │   over stdio    │  Codex  │   (Responses)   │  Model   │
   │  (parent)   │ ◄─────────────► │  agent  │ ◄─────────────► │ (OpenAI) │
   └─────────────┘                 └─────────┘                 └──────────┘
```

Codex owns the history list, calls the model upstream over HTTPS, and
streams back to Softland over stdio. Model invocations happen IN THE GAPS
of the JSON-RPC stream. The streaming pipe closes when the model stops
asking for tools.

### §1.3 The internal loop trace (one turn, one tool call)

```text
   softland ──►              turn/start

            ◄── notification  turn/started
            ◄── notification  item/started (Reasoning I1)
            ◄── notification  item/reasoning/textDelta I1     (×N)
            ◄── notification  item/completed (Reasoning I1)

                              ~ model emits tool_call ─┐
            ◄── notification  item/started (CmdExec, call C1)
                              ~ Codex executes locally ~
            ◄── notification  item/cmdExec/outputDelta C1     (×N)
            ◄── notification  item/completed (CmdExec C1)
                              ~ tool_result appended to history ┘

                              ~ ~ ~  Codex calls model AGAIN  ~ ~ ~

            ◄── notification  item/started (Reasoning I3)
            ◄── notification  item/reasoning/textDelta I3     (×N)
            ◄── notification  item/completed (Reasoning I3)

                              ~ model emits final message ─┐
            ◄── notification  item/started (AgentMessage I4)
            ◄── notification  item/agentMessage/delta I4     (×N)
            ◄── notification  item/completed (AgentMessage I4)
                              ~ model emits "stop" ─────────┘

            ◄── notification  thread/tokenUsage/updated
            ◄── notification  turn/completed
            ◄── response      to original turn/start request
```

Number of model invocations per turn = (tool calls) + 1. No upper bound
declared; Codex enforces token + turn budgets.

Multi-tool turn: same shape, repeated per tool call. Approvals can
interrupt with a server-to-client request that blocks the loop until the
parent replies.

Three nested rhythms: (1) model tokens stream as deltas; (2) tool calls
punctuate the stream; (3) the whole thing repeats inside one turn until
the model decides to stop emitting tools.

---

## §2 Codex protocol — concrete

### §2.1 Four surfaces

```text
   Surface             Use when                      Drive how
   ─────────────────   ─────────────────────────     ───────────────────
   codex (TUI)         human at keyboard             terminal
   codex exec          one-shot CLI invocations      positional prompt
   codex exec --json   one-shot, JSONL output        curated ThreadEvents
   codex app-server    long-lived JSON-RPC server    newline-JSON / stdio
   codex mcp-server    Codex AS a tool to other LLMs MCP JSON-RPC
```

### §2.2 Recommended surface: `codex app-server`

Reasoning, against Sid's stated needs:

1. **Reasoning trails (Sid's thesis).** `exec --json` strips deltas, raw
   reasoning, exec output chunks, and approvals (curated `ThreadEvent`
   shape, ~8 top-level types in `codex-rs/exec/src/exec_events.rs`).
   `app-server` gives the full ~78-variant event stream.
2. **Mid-conversation injection.** `exec` spawns one process per turn.
   `app-server` keeps thread loaded in-process; `turn/start` repeatedly
   with the same `threadId`. `turn/steer` for mid-turn input injection.
3. **Approvals through Softland UI.** `exec` HARDCODES auto-reject for
   the five approval kinds (`exec/src/lib.rs:1532-1624`). `app-server`
   forwards them as JSON-RPC ServerRequests; Softland reads, raises an
   approval row, user decides, Softland replies with matching id.
4. **Pause/resume.** `thread/start` returns `threadId`; `thread/resume`
   after process death. Rollouts on disk; ~30-day TTL on Responses API
   server-side state.

Caveat: `app-server` is `[experimental]` in `cli/main.rs:126`. Pin a
Codex version; regenerate Clojure parsers from
`codex-rs/app-server-protocol/schema/json/*.json` on every upgrade.

### §2.3 Wire format

Newline-delimited JSON over stdio. No LSP-style Content-Length headers.
Each side: write JSON, push `\n`, flush.

Four envelope shapes (in either direction):

```jsonc
// Request (id-bearing):
{"id": 1, "method": "thread/start", "params": {...}}
// Notification (no id):
{"method": "turn/started", "params": {...}}
// Response success:
{"id": 1, "result": {...}}
// Response error:
{"id": 1, "error": {"code": -32603, "message": "...", "data": {...}}}
```

`jsonrpc` field is NOT required. `id` is `string | int`.

### §2.4 Setup handshake

```jsonc
// Softland:
{"id":1,"method":"initialize","params":{"clientInfo":{"name":"softland","version":"0.1.0"}}}
// Codex replies:
{"id":1,"result":{"userAgent":"codex-cli/...","authStatus":"...","userInfo":{...}}}
// Softland:
{"method":"initialized","params":{}}
```

Then for each conversation thread:

```jsonc
{"id":2,"method":"thread/start","params":{
  "model":"gpt-5.2-codex",
  "modelProvider":"openai",
  "cwd":"/abs/path/to/workspace",
  "approvalPolicy":"on-request",
  "sandbox":"workspace-write",
  "ephemeral":false
}}
// Codex returns:
{"id":2,"result":{"thread":{"id":"01H...", ...}, ...}}
```

Then `turn/start` with input array:

```jsonc
{"id":3,"method":"turn/start","params":{
  "threadId":"01H...",
  "input":[{"type":"text","text":"Read README.md and summarize."}]
}}
```

### §2.5 Submission ops (what Softland sends)

| Op (Rust) / wire method | When |
|---|---|
| `UserTurn` / `turn/start` | the main "go work on this" |
| `Interrupt` / `turn/interrupt` | cancel current turn |
| `ExecApproval` / response to `item/cmdExec/requestApproval` | resolve pending exec approval |
| `PatchApproval` / response to `item/fileChange/requestApproval` | same for patches |
| `Compact` / `thread/compact` | force conversation compaction |
| `CleanBackgroundTerminals` | kill long-running shells |
| `Shutdown` | graceful shutdown |
| `OverrideTurnContext` | persistent context override |
| `RunUserShellCommand` | one-off shell command (TUI's `!cmd`) |
| `thread/fork` | fork a thread at an item boundary |

`UserInput` (the input array) is type-tagged: `text`, `image` (URL),
`localImage` (path), `skill` (name+path), `mention` (name+path).

### §2.6 Event types (what comes back)

`EventMsg` has 78 variants in `codex-rs/protocol/src/protocol.rs:
1305-1511`, grouped:

- **Session lifecycle**: `SessionConfigured` (always FIRST event),
  `ShutdownComplete`, `ContextCompacted`, `ThreadRolledBack`.
- **Turn lifecycle**: `TurnStarted` (wire: `task_started`),
  `TurnComplete` (wire: `task_complete`), `TurnAborted`, `TokenCount`
  (with `cached_input_tokens`; fires multiple times per turn).
- **Model output**: `AgentMessage` (full), `AgentMessageContentDelta`
  (chunks), `UserMessage`.
- **Reasoning** — three streams:
  - Summary: `AgentReasoning` (full block) + `ReasoningContentDelta`
    (chunks)
  - Raw: `AgentReasoningRawContent` + `ReasoningRawContentDelta`
    (hidden by default; `show_raw_agent_reasoning` enables)
  - Section markers: `AgentReasoningSectionBreak`
  Raw may be encrypted (`encrypted_content`) depending on plan tier.
- **Tool calls — exec/shell**: `ExecCommandBegin/OutputDelta/End` with
  `call_id`. `chunk` is base64 bytes (PTY may not be UTF-8).
  `parsed_cmd` is Codex's structured parse.
- **Tool calls — apply_patch**: `PatchApplyBegin/Updated/End` +
  `TurnDiff` (aggregated diff per turn).
- **Tool calls — MCP**: `McpToolCallBegin/End` with
  `result: Result<CallToolResult, String>`.
- **Tool calls — other**: `WebSearchBegin/End`,
  `ImageGenerationBegin/End`, `ViewImageToolCall`, `TerminalInteraction`,
  `DynamicToolCallRequest/Response`.
- **Item stream** (modern unified): `ItemStarted`/`ItemCompleted` wrapping
  `TurnItem` (`UserMessage | Reasoning | AgentMessage | FileChange |
  McpToolCall | Plan | ContextCompaction | ...`). Both legacy AND
  item-stream events fire for back-compat. `ItemCompleted` is
  authoritative truth-of-record.
- **Approval / interactive requests** — these BLOCK the turn:
  `ExecApprovalRequest`, `ApplyPatchApprovalRequest`, `RequestPermissions`,
  `RequestUserInput`, `DynamicToolCallRequest`, `ElicitationRequest`,
  `GuardianAssessment`.
- **Errors**: `Error` (fatal); `StreamError` (TRANSIENT — engine retries,
  do NOT halt); `Warning`, `GuardianWarning`, `DeprecationNotice`;
  `ModelReroute`, `ModelVerification`.
- **Plan/hook/collab**: `PlanUpdate`/`PlanDelta`,
  `HookStarted`/`HookCompleted`, `Collab*Begin/End`, `RawResponseItem`
  (literal Responses-API output — perfect replayability).

### §2.7 Tool-call flow detail

**Exec (with approval)**:

1. (optional) `ExecApprovalRequest{call_id, command, cwd, parsed_cmd, ...}`
   — turn blocks
2. (parent replies) `Op::ExecApproval{id, decision: Approved}`
3. `ExecCommandBegin{call_id, command, cwd, parsed_cmd, source: Agent}`
4. `ExecCommandOutputDelta{call_id, stream: stdout|stderr, chunk: base64}` × N
5. `ExecCommandEnd{call_id, stdout, stderr, exit_code, duration, status}`

With `approval_policy: Never`, approval step skipped.

**apply_patch**: similar; ends with `PatchApplyEnd{call_id, success, ...}`
plus a `TurnDiff{unified_diff}` aggregating the entire turn's patches.

**MCP**: `McpToolCallBegin` → atomic execution → `McpToolCallEnd` with
`result: Result<CallToolResult, String>`. MCP servers can elicit
(`ElicitationRequest` → `Op::ResolveElicitation`).

`call_id` ties begin/end pairs across the stream. `item.id` ties
item-stream items + their deltas. `turn_id` rolls up one turn.

### §2.8 Auth on a trusted runner (ChatGPT subscription)

NOT API keys. One-time setup:

```bash
export CODEX_HOME=/var/lib/softland/codex
codex login                       # PKCE browser flow, opens browser
```

Produces `${CODEX_HOME}/auth.json`:

```jsonc
{
  "auth_mode": "chatgpt",
  "OPENAI_API_KEY": null,
  "tokens": {
    "id_token":      "<JWT — has email, plan_type, account_id>",
    "access_token":  "<bearer for upstream calls>",
    "refresh_token": "<opaque>",
    "account_id":    "<workspace uuid>"
  },
  "last_refresh": "2026-..."
}
```

Codex auto-refreshes the access_token using `refresh_token`. On
permanent refresh failure (`Expired`/`Exhausted`/`Revoked`), Codex emits
an `Error` event referencing auth — Softland surfaces as
`re-login required` and does NOT silently retry.

Spawn Codex with auth-flipping env vars stripped:

```clojure
(.remove env "OPENAI_API_KEY")    ; never accidentally fall to API mode
(.remove env "CODEX_API_KEY")
(.remove env "CODEX_AGENT_IDENTITY")
(.put env "CODEX_HOME" codex-home)
```

`config.toml` recommended for the runner:

```toml
forced_login_method = "chatgpt"          # prevents accidental API fallback
cli_auth_credentials_store_mode = "file"  # simpler than libsecret
hide_agent_reasoning = false
show_raw_agent_reasoning = true           # capture full reasoning trail

[permissions]
approval_policy = "on-request"            # default; overridden per turn

[sandbox]
mode = "workspace-write"
```

Rama should NOT store: full `auth.json`, JWT claims, `OPENAI_API_KEY`,
`account_id`. Identity only — capability/profile referencing auth
indirectly. Sample the JSON-RPC stream to confirm it's clean of
credential material (it is — JWTs live only in HTTPS requests upstream).

### §2.9 Approvals & sandbox in headless mode

`assess_patch_safety` (`codex-rs/core/src/safety.rs:33-116`) is the gate.

```text
   approval_policy = "never"      + escapes writable roots
                                  → fails closed (agent retries with
                                    different approach; turn completes
                                    with status :completed)

   approval_policy = "on-request" + needs approval
                                  → emits ExecApprovalRequest /
                                    ApplyPatchApprovalRequest as
                                    JSON-RPC ServerRequest
                                  → turn BLOCKS until parent replies
                                  → no Codex-side timeout — waits forever
                                  → Softland MUST enforce wall-clock
                                    timeout (~5 min); on timeout send
                                    {"decision":"decline"}

   approval_policy = "untrusted"  → trust only built-in shell commands
                                    (ls, cat, sed, ...); ask everything
                                    else

   --dangerously-bypass-approvals-and-sandbox  ("--yolo")
                                  → use ONLY inside another sandbox
                                    (container/VM)
```

---

## §3 Cost & caching

Two layers compose. Most "prompt caching" articles only describe Layer 2;
the big lever for forking is actually Layer 1.

### §3.1 Layer 1 — Responses API server-side state

Each Codex turn produces a `response.id` stored on OpenAI's side. The
next turn submits ONLY new user input + `previous_response_id`. The
server rehydrates the conversation context.

```text
   TTL: ~30 days (Responses API default)
   Cost: ~free for inherited context
   Controlled by: disable_response_storage (off = chaining on)
```

### §3.2 Layer 2 — automatic prefix caching

OpenAI hashes prefixes; identical prefixes within the TTL get reused.

```text
   TTL: ~5–10 min
   Cost reduction: cached input tokens billed at ~50%
   Min prefix: 1024 tokens
   Controlled by: nothing (automatic, no API surface)
   Visible via: cached_input_tokens in TokenUsage events
```

### §3.3 10-fork scenario, concrete

**Storage on (default), all forks fired within an hour:**

```text
   Base chat: 10 turns, ~50K-token prefix, last response_id R0.

   Fork 1 first turn: input = ~50 tokens (just the new prompt) +
                      references R0
                      → server rehydrates 50K context from R0
                      → wire-cost: ~50 input tokens
   Forks 2-10 first turns: same — ~50 tokens each
   
   Total wire input across 10 forks' first turns: ~500 tokens.
   Output: paid normally per fork.
```

**Storage off (privacy mode), batched within 5 min:**

```text
   Each fork's first turn submits the full 50K prefix.
   Fork 1: 50K full-rate input
   Forks 2-10: 50K cache-hit input @ 50% (Layer 2)
   
   Total wire input: 50K + 9 × 50K × 0.5 = 275K tokens
                     (vs 500K without any caching).
```

**Storage off, spread over a day:**

```text
   Layer 2 cache TTL has expired between forks.
   Each fork: 50K full-rate input.
   Total: 10 × 50K = 500K tokens. 2× more than batched.
```

### §3.4 Subscription vs API

```text
   API-billed user:
     cost = dollars per token
     caching = direct dollar savings
   
   ChatGPT subscription user (Sid):
     cost = messages-against-quota
     caching = saves *latency*, not dollars
     each turn/start = 1 message from your quota
     
   For Softland: surface "X of Y messages this window" not "$X spent."
   Track both modes; UI shows whichever applies to the runner's auth.
```

### §3.5 Compaction interaction

`Compact` op rewrites prefix as a summary. After compaction:
- Layer 1 chain unbroken (new summary is a new response_id, chains forward)
- Layer 2 cache for OLD prefix invalidated (new compacted prefix is
  fresh content)
- Total tokens from compaction point forward are smaller

**Compact-then-fork** is usually cheaper than **fork-then-compact** —
all forks inherit the compacted summary.

Compaction is destructive to the trail from the model's perspective
(model only sees the summary on subsequent runs). Softland's trail in
Rama survives compaction. Surface compaction as a marker on the canvas:
"compacted at this point; downstream model sees a summary, but you still
see the full trail."

---

## §4 Forking semantics

Codex natively supports forking:
- `codex fork` CLI subcommand (auxiliary)
- `SessionConfiguredEvent.forked_from_id: Option<ThreadId>` (protocol)
- Inferred wire method on `app-server`: `thread/fork {threadId,
  fromItemId}` (verify against ClientRequest schema; protocol field
  `forked_from_id` is confirmed)

When you fork thread T at item I:
- New thread T' is created
- Its history is the prefix of T up to and including I
- T' has its own threadId and rolloutPath
- Both threads run in parallel; neither's events leak into the other
- T'.forked_from_id == T

A fork = "copy history list to here, give it a new ID, continue."

### §4.1 Span-anchored forks (Softland-side overlay)

Codex forks at item boundaries. To fork at a span (substring of an
item):

1. User selects span S of item I in run-A
2. Softland sends `thread/fork {threadId: A.thread, fromItemId: I}` →
   gets thread-B
3. Softland creates run-B in `$$llm-threads` with `:parent-run/id A`,
   `:fork/anchor {:item/id I :span S}`
4. Softland sends `turn/start {threadId: thread-B, input: [{type:"text",
   text: "Re: '" + S + "' — <user question>"}]}`

Span semantics live in Softland's data, not Codex. The model sees the
quoted span as part of the new user message; Codex doesn't natively
understand "span anchors."

### §4.2 Reconciliation (multi-parent, late-bound)

Two paths:

- **Programmatic**: mint synthesis run with `:parent-run/ids [B C]`,
  feed Codex a synthesis prompt with both branches' content. Tends to
  produce flattened summaries.
- **Preserved plurality**: don't merge; show forks side-by-side; user
  writes synthesis note that references both as evidence.

Default: preserved plurality. From Sid's memory:

> "Late-bound consensus: preserve disagreement structurally until
>  synthesis is meaningful. Synthesis is transformation of preserved
>  plurality, not summary."

### §4.3 Plurality preservation

Multi-fork doesn't destroy parents; reconciliation creates a NEW node;
all parents remain. Thread DAG can have multi-parent merges. The
**conversation DAG and the discourse DAG are the same graph at different
zoom levels** — exactly the "continuous semantic zoom" Sid is designing
toward.

---

## §5 Rama framing — substrate semantics

Most confusion about depots/PStates dissolves once you internalize the
substrate semantics. The instinct to look for "the source of truth" as
one place is database-shaped thinking. Rama is event-sourcing-shaped.

### §5.1 Source of truth = depot log

```text
       ┌─────────────────────────┐
       │ DEPOT LOG               │ ◄── TRUTH
       │   event 1               │     append-only
       │   event 2               │     plural depots, one truth
       │   event 3               │
       │   ...                   │
       └────────────┬────────────┘
                    │  topology code (deterministic projection)
                    ▼
       ┌─────────────────────────┐
       │ PSTATES                 │ ◄── DERIVED CACHE
       │   $$llm-threads, ...    │     delete them → replay log →
       │   $$objects             │     same result
       └─────────────────────────┘
```

The depot log IS the source of truth. Plural-bodied (many depots) but
singular truth. PStates are caches; topology is rules; artifacts are
projections of PStates.

This means a few things that may feel weird at first:
- Change a PState schema → drop it, redeploy topology code, replay log,
  get a fresh one with new shape.
- Multiple catalogs/projections are fine. They're all derived from the
  same log.
- PStates that stop being read can be dropped without losing data.
- Replay is a debugging tool: "given this depot history, what *should*
  the topology have computed?" — a deterministic question.

### §5.2 The 4-tier read/write asymmetry

```text
                  WRITE                READ
                  ─────                ────

   DEPOT   ◄────── UI                  ────► topology only
           ◄────── executor
           ◄────── topology  (foreign-append)


   PSTATE  ◄────── topology only       ────► UI
                                       ────► executor
                                       ────► topology


   TOPOLOGY  = rules    (code, no state — neither read nor written)
   DEPOT     = log      (TRUTH — append-only event history)
   PSTATE    = cache    (DERIVED — recomputable from depot + topology)
```

Topology is the ONLY writer of PStates. Every PState has exactly one
place to look when asking "how did this row get there?" — the topology
branch handling the corresponding depot event. You never have to chase
mutations across services.

### §5.3 Depots are siblings, not hierarchical

```text
   ✗ NOT this:                       ✓ Rama is:
   
   ┌──────────────┐                  *llm     *world    *compute
   │ TOP DEPOT    │ ← bottleneck
   └──────┬───────┘                    │        │          │
          │                            ▼        ▼          ▼
     ┌────┴────┐                       (parallel; different
     ▼         ▼                        partition keys; no funnel)
   *llm     *compute
```

There is no "top depot." Each depot is kind-typed, partitioned
independently. Rama hierarchies are not a substrate concept — they live
in your application's semantics, not in Rama itself.

### §5.4 Where bottlenecks come from (and don't)

```text
   Bottleneck: BAD partition key (e.g., hash by :tenant/id when one
                tenant does 99% of work).
   
   NOT a bottleneck: many depots. They run in parallel; each has its
                     own partition layout, topology branch, task assignment.
```

### §5.5 Cross-track patterns: foreign-append vs fan-in

Two valid patterns for getting events from one track to another's PState.

```text
   PATTERN A — TRANSLATION              PATTERN B — FAN-IN
   
   *llm-depot                           *llm-depot ─┐
        │                                            │
        ▼                                *world ─────┼──► WORLD topology
   LLM topology                                      │         │
        │                                *compute ──┘          ▼
        │ foreign-append                                  $$objects
        ▼                                
   *world-action                         tight coupling
        │                                world topology sees three depots
        ▼
   WORLD topology
        │
        ▼
   $$objects
   
   loose coupling
   each topology = own depot
```

Both produce the same `$$objects`. Pattern A for stricter track
isolation (matches Sid's existing track-separation principle). Pattern B
for fewer hops.

### §5.6 Where breakage happens

```text
   *llm-depot      ◄─── append succeeds ✓
        │
        ▼
   LLM topology   ◄─── writes $$llm-threads ✓
        │
        │ foreign-append to *world-action
        │
        ▼
   *world-action   ◄─── ✗ here: if foreign-append fails or the catalog
                          topology never sees it, chat exists in LLM
                          PState but isn't catalogued in world.
   
   Fix: :append-ack on the foreign-append.
```

Cross-track handshakes are always the breakage point. Use ack-required
appends for state-transition events; bare appends are fine for
high-frequency deltas.

---

## §6 The artifact problem

### §6.1 Three roles, one thing

A single chat reply has THREE roles at once:

1. **Process** — trail of reasoning + tool calls + deltas as it streams
2. **Artifact** — citeable identity; quote, link from a note, fork from
3. **Material** — content other work builds on; future chats use as context

Same thing seen from three angles. That's why "where does it live" is
confusing: it lives in three places at three altitudes simultaneously.
Sid drew this exactly in his "Thing → spawn → T₁/T₂/T₃ → Outside"
sketch — a parent artifact spawns derivative artifacts, each of which
leaves its parent and becomes independently citeable.

### §6.2 Two-view reframe (the unlock)

```text
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   LLM-SIDE VIEW                      USER-SIDE VIEW
   (what the model saw)               (what the UI renders)

   ┌────────────────────────┐         ┌────────────────────────────────┐
   │ msg-1 user             │         │ msg-1 user (+ annotation)      │
   │ msg-2 assistant        │         │ msg-2 assistant                │
   │ tool_call              │         │   ▸ slice A → notes-37         │
   │ tool_result            │         │   ▸ slice B → forked → chat-Y  │
   │ msg-3 user             │         │ msg-3 user (quotes msg-2 ¶2)   │
   │ msg-4 assistant        │         │ msg-4 assistant (bookmarked)   │
   │                        │         │ ↳ user-comment "this is wrong" │
   └────────────────────────┘         └────────────────────────────────┘
        FROZEN (immutable)                 COMPOSED at render-time

        stored in $$llm-runs                from $$llm-runs
        (LLM track owns content)            + world annotations
        from Codex's view                   + slice/quote artifacts
                                            + artifact-graph edges

   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   THE LLM NEVER RETROACTIVELY SEES THE RIGHT COLUMN.
   NEW TURNS RECEIVE A ContextBundle THAT MAY RENDER PARTS OF IT.
```

The chat the model sees ≠ the chat the user sees.

### §6.3 Editing is authoring

```text
   USER ACTION                       WHERE IT LANDS

   "edit the LLM's reply text"   →   new world annotation artifact
                                     (LLM PState UNCHANGED)

   "slice this paragraph out"    →   new world slice artifact + edge
                                     :slice-of → chat-A.item-I7

   "quote msg-2 in my notes"     →   new world ref edge
                                     note-X :cites chat-A.item-I7

   "send a new prompt that         → fork → new LLM run with the
    includes my edits"               composed history as turn-1 input

   "fork from this point with    →   new LLM run, parent = chat-A,
    different content"               anchor = item-I7
```

The LLM track really does only handle "the run." Everything around it —
composition, editing, slicing, annotation — is world-track authoring.

### §6.4 Four-class artifact taxonomy

```text
   ┌──────────────────┬──────────────────────┬─────────────────────────┐
   │ CLASS            │ HOME                 │ NOTES                   │
   ├──────────────────┼──────────────────────┼─────────────────────────┤
   │ RAW LLM ITEM     │ LLM track            │ immutable; what model   │
   │                  │                      │ emitted                 │
   │ WORLD OVERLAY    │ World track          │ comment / annotation /  │
   │                  │                      │ slice / anchor /        │
   │                  │                      │ bookmark                │
   │ WORLD DERIVATIVE │ World track          │ edited version /        │
   │                  │                      │ curated excerpt /       │
   │                  │                      │ synthesized note        │
   │ CONTEXT BUNDLE   │ World/Context track  │ immutable; rendered     │
   │                  │ (consumed by LLM)    │ input for ONE turn      │
   └──────────────────┴──────────────────────┴─────────────────────────┘
```

Critical distinction: WORLD OVERLAY (annotation) vs WORLD DERIVATIVE
(replacement-the-user-authored). Annotations travel alongside the raw
item; derivatives are alternate versions the next ContextBundle may use
*instead of* the raw item. Collapsing them reduces "edit" to "comment"
— too weak for the canvas vision.

### §6.5 Three-layer source-of-truth picture

```text
   ┌─────────────────────────────────────┐
   │ RAW EXECUTION TRUTH                 │
   │   LLM track                         │
   │   "what the model saw + emitted"    │
   └────────────────┬────────────────────┘
                    +
   ┌────────────────▼────────────────────┐
   │ USER MEANING / WORKING TRUTH        │
   │   World track                       │
   │   "what we annotate, edit, slice,   │
   │    fork, accept"                    │
   └────────────────┬────────────────────┘
                    +
   ┌────────────────▼────────────────────┐
   │ NEXT MODEL INPUT                    │
   │   ContextBundle                     │
   │   "what we choose to show now"      │
   └─────────────────────────────────────┘
```

---

## §7 The architecture (v2 + breadth)

### §7.1 Three thin canonical views

Use whichever matches the question. The fat composite view (§7.6) is
for full-detail reference.

#### View A — Spine (flow + breadth labels)

```text
   WorldThread chat-A         ← composed view; drafts / slices / edits / forks
         │                    ★ :parent-thread/id makes threads a DAG
         │ SEND               ★ may aggregate multiple WorldThreads (reconcile)
         ▼
   ContextBundle ctx-N        ← immutable model input for ONE turn
         │                    ★ the replay primitive
         ▼
   LLMTurnRun run-N           ← ONE execution
         │                    ★ reverse-MCP: agent reads world state
         ▼
   Raw LLM Items              ← immutable: user / assistant / tool / reasoning
         │
         ▼
   Object Catalog + Graph     ← citeable identity, typed edges
                              ★ discourse graph = filter {Q,C,E,D,R,F}
```

#### View B — Containment (nesting / what-owns-what)

```text
   WorldThread chat-A
         │
         ▼
   LLMThread codex-thread-123
         │
         ├── TurnRun run-1
         │     ├── ContextBundle ctx-1   (frozen at SEND)
         │     └── items                  (user / assistant / tool / reasoning)
         │
         ├── TurnRun run-2
         │     ├── ContextBundle ctx-2
         │     └── items
         │
         └── TurnRun run-3
               ├── ContextBundle ctx-3
               └── items
```

ContextBundle and items are SIBLINGS of each TurnRun, not predecessors.
Bundle is the input; items are the output; both keyed by run-id.

#### View C — Fork DAG (plurality across chats)

```text
            WorldThread chat-A           (root)
                  │
        ┌─────────┼──────────┐
        ▼         ▼          ▼
    chat-A1   chat-A2    chat-A3        (siblings; same parent)
        │
    ┌───┼───┐
    ▼   ▼   ▼
  chat-B1 ...                            (deeper forks; preserved plurality)


                   chat-A1   chat-A2
                       \     /
                        \   /                 (multi-parent merge =
                         \ /                   reconciliation node;
                          ▼                    optional, late-bound)
                    chat-S1 (synthesis)
```

### §7.2 When to use which view

```text
   QUESTION                                 VIEW
   "what flows where in one turn?"          A (spine)
   "how do thread/turn/item nest?"          B (containment)
   "how does plurality look across chats?"  C (fork DAG)
   "give me everything at once"             Fat composite (§7.6)
```

### §7.3 Three-level naming

```text
   WorldThread (chat-A)            ← user's "the chat"
        │
        ▼
   LLMThread (codex-thread-123)    ← one logical conversation with the model
        │
        ├── LLMTurnRun run-1        ← ONE Send → ONE execution
        │     └─ items
        ├── LLMTurnRun run-2
        │     └─ items
        └── LLMTurnRun run-3
              └─ items
```

"Run" reserved for execution. Threads/sessions own runs. (Important
naming hygiene: avoid using "run" for the whole chat — confuses
implementation.)

### §7.4 PState renames from v1

```text
   OLD                          NEW
   ──────────────────────────   ─────────────────────────────────
   $$llm-runs                   $$llm-threads      (one per LLMThread)
                              + $$llm-turn-runs    (one per turn execution)

   $$llm-items-by-run           $$llm-items-by-thread
                              + $$llm-items-by-turn-run

   $$llm-conversation-graph     $$llm-thread-graph
   (keyed by run-id)            (keyed by thread-id; DAG of forks)

   (new)                        $$context-bundles  (one per turn;
                                                    immutable;
                                                    replay primitive)

   (new)                        $$world-derivatives (edited messages, etc.)
                                                    (or kind-typed:
                                                     $$edited-messages,
                                                     $$curated-excerpts,
                                                     $$synthesized-notes)

   $$objects                    $$objects          (unchanged — catalog)
                              + $$artifact-graph   (typed edges; discourse
                                                    graph is a projection)
```

### §7.5 The four breadth labels (★)

These labels prevent v2 from collapsing into a chat-app architecture:

```text
   1. :parent-thread/id on WorldThread
      → DAG of forks structurally enabled

   2. ContextBundle aggregates from multiple WorldThreads
      → reconciliation is a first-class flow

   3. Reverse-MCP from LLMTurnRun (Softland-tools server)
      → custom tools; agent reads $$objects, $$discourse-graph,
        $$llm-threads as tools

   4. Discourse graph = typed projection of artifact-graph
      → filter to artifact/kind ∈ {Q,C,E,D,R,F}; no separate substrate
```

These are not implementation; they're memory aids embedded in the
picture. If anyone (Sid, Claude, Codex, a future contributor) looks at
v2 alone and forgets plurality / reconciliation / custom tools /
discourse, the labels are right there.

### §7.6 The fat composite view (full detail)

```text
            USER-FACING CHAT / CANVAS
                 (working surface)
                       │
                       ▼
   ┌─────────────────────────────────────────────────┐
   │ WorldThread (composed view)                     │
   │   drafts, slices, comments, annotations,        │
   │   edited derivatives, forks, refs               │
   │   + pointers into raw LLM items                 │
   │                                                  │
   │   ◄── :parent-thread/id                         │  ★ DAG of forks
   │       (threads form a DAG via parent refs;      │
   │        plurality is preserved by default)       │
   └────────────────────┬────────────────────────────┘
                        │
                        │ SEND
                        │ (freeze the composed view)
                        │ (may aggregate from MULTIPLE                ★ reconciliation
                        │  WorldThreads → synthesis input)
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ ContextBundle  (HOME: World/Context track)      │
   │   immutable model input for ONE upcoming turn   │
   │   renders: quotes, slices, edited derivatives,  │
   │            notes, system instructions           │
   │                                                  │
   │   THE ONLY THING THE MODEL ACTUALLY SEES         │
   │   THE REPLAY PRIMITIVE                           │
   └────────────────────┬────────────────────────────┘
                        │
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ LLMTurnRun                                      │
   │   one execution of one turn                     │
   │   receives ContextBundle, streams output        │
   │                                                  │
   │   ◄── reverse-channel: Softland-MCP server      │  ★ custom tools
   │       (agent calls tools that read $$objects,   │
   │        $$discourse-graph, $$llm-threads, etc.   │
   │        → self-introspective AI)                 │
   └────────────────────┬────────────────────────────┘
                        │
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ Raw LLM Items                                   │
   │   immutable: user-as-sent, assistant output,    │
   │              tool calls, reasoning,             │
   │              token usage                        │
   └────────────────────┬────────────────────────────┘
                        │
                        │ catalog / cite
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ Object Catalog + Artifact Graph                 │
   │   makes raw items citeable, sliceable, anchored │
   │                                                  │
   │   discourse-graph = typed projection where      │  ★ discourse
   │     artifact/kind ∈ {Q,C,E,D,R,F}               │
   │   (no separate substrate; filtered view)        │
   └─────────────────────────────────────────────────┘
```

---

## §8 Cross-track patterns

### §8.1 LLM ↔ World

The LLM track produces *proposals*; only the World track converts to truth.

**LLM → World** (proposals):
- `TurnDiff` event → `:world-action :patch/apply-proposed`
- Final `AgentMessage` → `:world-action :note/create-proposed`
  (if user promotes)
- Reconciliation → `:world-action :synthesis/create-proposed`
- File mentions in tool calls → `:world-action :ref/create-proposed`

**World → LLM** (context):
- Existing notes/objects citeable as `mention` parts in `UserInput`
- Discourse-graph nodes become first-class context refs in
  `:llm/codex-run :payload :context-refs`

The relationship is **proposal-decision**: WorldDepot is gated. LLM
never silently mutates world state.

### §8.2 LLM ↔ Compute

**LLM → Compute** (verification):
- Agent-issued shell commands are Codex-internal exec
- Or via Softland-MCP custom tool: Codex → MCP → ComputeDepot →
  ComputeExecutor → result back

**Compute → LLM** (auto-trigger):
- A failing build can auto-mint `:llm/codex-run` with prompt "the build
  failed with X, propose a fix" — the dogfood loop in its purest form

### §8.3 Pattern X vs Pattern Y

```text
   PATTERN X — DIRECT (acceptable for MVP)

   UI ──► *llm-depot ──► LLMTopology ──► $$llm-threads,
                                          $$llm-turn-runs
                                       ──► (catalog updated downstream)


   PATTERN Y — WORLD-FIRST (canonical long-term)

   UI ──► *world-action ──► WorldTopology
                                 │
                                 │ creates citeable pending
                                 │ chat/turn object in $$objects
                                 │ (visible BEFORE run starts)
                                 │
                                 │ foreign-append
                                 ▼
                              *llm-depot
                                 │
                                 ▼
                              LLMTopology
                                 │
                                 ▼
                              LLMTurnRun streams
                                 │
                                 ▼
                              raw items + catalog refs
```

Pattern Y matches Sid's "everything is part of the world" instinct.
Pattern X is a refactor-later concession to MVP simplicity.

---

## §9 Lifecycle scenarios

### §9.1 Fresh run

```text
   1. UI authors prompt; pulls refs from canvas/notes/prior chats
      (resolves through $$objects).
   2. UI optionally saves draft as a world artifact.
   3. UI clicks SEND.
   4. (Pattern X) UI appends :llm/codex-run to *llm-depot
      (Pattern Y) UI appends :llm-run/intent to *world-action
      → WorldTopology creates pending $$objects[chat-A]
      → foreign-appends to *llm-depot
   5. LLMTopology validates capability, decides accept, writes
      $$llm-threads[A] :status :pending,
      $$llm-decisions-by-run-id[A], $$llm-pending-by-task.
   6. LLMExecutor reconciles, claims, gets durable grant from PState
      (3-state await), spawns Codex app-server child, sends
      initialize → initialized → thread/start → turn/start.
   7. Codex streams events; executor appends to *llm-obs-depot;
      OBS branch folds into PStates (with sequence buffering).
   8. UI watches $$llm-views[A] — canvas spawns a node, run-detail
      view auto-opens, reasoning streams in real-time.
   9. Codex emits TurnDiff, turn/completed; OBS branch closes the turn,
      status :succeeded.
```

### §9.2 Follow-up turn (multi-turn in same thread)

```text
   1. UI renders WorldThread: raw LLM items + world overlays + slices
      + edited derivatives + artifact-graph edges.
   2. User composes follow-up prompt referencing slices/notes/edits.
   3. UI clicks SEND.
   4. UI appends :llm/codex-turn to *llm-depot with :run-id A.
   5. LLMTopology appends turn-2 to thread A.
   6. LLMExecutor sends turn/start to existing Codex thread (same
      Codex child process, same threadId).
   7. Streams $$llm-turns[A][T-2].
```

### §9.3 Slice an assistant reply

```text
   1. User selects span in msg-4 of run-A.
   2. UI clicks "slice".
   3. UI appends *world-action :slice/create
      with target chat-A.item-I7, span [142, 281].
   4. WorldTopology writes:
        $$slices[S-1]                 (or $$world-overlays[S-1])
        $$objects[S-1]                (catalog entry; kind :slice)
        $$artifact-graph += edge {S-1 :slice-of chat-A.item-I7
                                  :span [142, 281]}
   5. LLM PState UNCHANGED. The raw item is immutable.
   6. UI renders slice marker on the message in run-detail view.
```

### §9.4 Edit an assistant reply

Two distinct operations:

**Annotate the original** (overlay):
```text
   1. UI appends *world-action :comment/create
      with target chat-A.item-I7, text "actually...".
   2. WorldTopology writes $$comments[C-1], $$objects[C-1],
      $$artifact-graph edge {C-1 :comment-on chat-A.item-I7}.
   3. Original raw item unchanged. Comment travels alongside.
```

**Create an edited derivative** (replacement-version):
```text
   1. UI appends *world-action :derivative/create
      with :derivative-of chat-A.item-I7, content <new text>.
   2. WorldTopology writes $$world-derivatives[D-1], $$objects[D-1],
      $$artifact-graph edge {D-1 :derivative-of chat-A.item-I7}.
   3. Future ContextBundle may render D-1 *instead of* the raw item.
   4. Raw item still unchanged — derivative is a sibling/alternative.
```

### §9.5 Fork from a paragraph (span-anchored)

```text
   1. User selects span in msg-4 of run-A. Clicks "fork on this".
   2. UI mints new run-id (say run-B).
   3. UI appends :llm/codex-fork to *llm-depot with
      :parent-run/id A, :fork/anchor {:item/id I-7 :span [142, 281]}.
   4. LLMTopology validates parent/anchor, writes $$llm-threads[B]
      :parent-run/id A :fork/anchor :status :pending.
   5. GRAPH branch updates $$llm-thread-graph adding new node + edge.
   6. LLMExecutor claims; sends thread/fork {threadId: A.thread,
      fromItemId: I-7} to Codex; gets new threadId; sends turn/start
      with quoted-span prompt.
   7. Streams parallel to A; canvas now shows two sibling nodes.
   8. $$llm-cost-by-subtree updates rollup.
```

### §9.6 Reconciliation

```text
   1. User selects two leaf forks (B, C). Clicks "synthesize".
   2. UI mints synthesis run-id (say run-S). Appends :llm/codex-reconcile
      to *llm-depot with :parent-run/ids [B, C] and synthesis prompt.
   3. LLMTopology writes $$llm-threads[S] :parent-run/id [B C]
      :status :pending.
   4. GRAPH branch creates a multi-parent node (DAG now has a merge).
   5. LLMExecutor: this is a NEW thread (Codex doesn't fork from multiple).
      Sends thread/start with system message including both parents'
      final messages and reasoning summaries as context.
      Then turn/start with synthesis prompt.
   6. Streams; result is a synthesis run that holds the *transformed*
      understanding while parents B and C remain untouched as preserved
      plurality.
```

### §9.7 World-write proposal (TurnDiff → patch acceptance)

```text
   1. Run-A produces TurnDiff event with a unified diff.
   2. OBS branch emits a :world-action :patch/apply-proposed to
      *world-action (auto-derived ActionRequest with provenance run-id).
   3. WorldTopology raises a decision row in $$objects-pending-decision.
   4. UI shows the proposed patch in the world-side review queue.
   5. Sid accepts; WorldTopology applies; $$patches-applied,
      $$objects update; $$activity-timeline records the moment.
   6. The applied patch's commit-id is referenced back on
      $$llm-threads[A] :produced-artifacts so the provenance chain
      is closed.
```

### §9.8 Cancel

```text
   1. UI appends :llm/cancel to *llm-cancel-depot.
   2. CANCEL branch writes $$llm-threads[A] :status :cancel-requested.
   3. LLMExecutor reads marker, sends Op::Interrupt and
      Op::CleanBackgroundTerminals to Codex.
   4. Codex emits TurnAborted{reason: Interrupted}; OBS branch updates
      $$llm-threads[A] :status :cancelled.
   5. Executor removes run from process pool; child exits cleanly.
```

### §9.9 Replay & time-travel

Three storage layers, increasing fidelity:

```text
   1. Codex's rollout JSONL at ~/.codex/sessions/.../rollout-*.jsonl.
      Curated subset (drops deltas, approvals). Cross-tool compatibility
      (codex resume <session-id> works).
   
   2. Softland's *llm-obs-depot in Rama. Full event stream, append-only.
      Reconstruct any run's full trail at any point in time by replaying
      the depot up to a given sequence.
   
   3. ContextBundles in $$context-bundles. Per-turn replay primitive —
      exact input the model received for any turn.
```

Time-travel queries: read PStates *as of* a given depot sequence. Rama's
PState model + history navigation gives you this without extra plumbing.

---

## §10 Open decisions (Sid-decidable, not cross-model decidable)

```text
   1. ContextBundle physical home.
        Own depot (*context-bundle-depot)? Topology-internal (composed
        at SEND time, stored as part of $$llm-turn-runs)? Or world track
        (composed by WorldTopology, foreign-appended to *llm-depot)?
        Trade-off: auditability vs hop count.

   2. World derivatives organization.
        Single PState ($$world-derivatives) vs kind-typed many
        ($$edited-messages, $$curated-excerpts, $$synthesized-notes).
        Trade-off: simplicity vs queryability.

   3. MVP commitment.
        Pattern X day-one (and refactor to Y later) or Pattern Y
        day-one? Trade-off: ship speed vs eventual rework.

   4. Derivative versioning.
        Edit twice → keep both versions, or single mutable artifact?
        Trade-off: history vs storage.
```

---

## §11 What was compressed by Codex (preserved as breadth labels)

Codex's job is to harden contracts; reviewers always tend to compress.
The labels below are concepts that v1 (full buildout) had foregrounded
which v2 (post-Codex) backgrounded but did NOT remove.

```text
   CONCEPT                        WHERE IT LIVES IN V2+BREADTH

   Forks / DAG of threads         :parent-thread/id on $$llm-threads;
                                  $$llm-thread-graph PState

   Plurality / disagreement       enabled by thread DAG;
                                  no automatic reconciliation

   Reconciliation / synthesis     ContextBundle aggregates from multiple
                                  WorldThreads

   Discourse graph                typed projection of $$artifact-graph
                                  filtered to {Q,C,E,D,R,F}

   Custom tools (Softland-MCP)    reverse-channel from LLMTurnRun;
                                  tools read $$objects, etc.

   Replay / time-travel           ContextBundle is the replay primitive

   Cost / quota tracking          observation-layer concern (TokenCount
                                  events; subscription quota tracked in
                                  $$llm-cost-by-run-id)

   Approvals                      separate back-arrow loop;
                                  *llm-approval-depot + UI bridge

   Canvas / semantic zoom         UI projection over PStates;
                                  no architecture change

   Multi-agent                    LLMThread polymorphic by agent kind
                                  (Codex / Claude / future)
```

NOTHING WAS STRUCTURALLY DELETED. Everything was demoted from foreground
to background. The four ★ labels in v2+breadth are the discipline for
keeping breadth visible inside a spine-shaped diagram.

---

## §12 Discipline & meta

### §12.1 Communication

Diagrams over walls of prose. Three thin views > one fat view (pick the
level matching the question). One concept per box; right-side
annotations for what each thing IS; minimal nesting depth. The fat
view's job is "everything in one place when I need detail," not "the
diagram I show first."

### §12.2 The am-i-losing-myself skill

Claude reconstructs "self" from context every turn. Strong recent signal
pulls Claude toward whoever spoke loudest. Drift is structural, not a
will-failure.

**Failure modes**:
1. Comprehensive over committed (12 half-believed claims instead of 3
   fully held)
2. Cite-instead-of-think (hiding behind authority)
3. Accommodate-on-review (ACCEPT all findings without genuine pushback)
4. Drift-toward-recent-pressure (bends toward whoever spoke last)

**Self-check protocol**:
1. Did I state my position FIRST, before alternatives?
2. Did I mark uncertainty ("I think", "I'm guessing", "I don't know")?
3. If accepting a review, did I push back on at least one point?
4. Am I past claim 3 of N? If yes, the rest is filler — stop.
5. Am I citing to avoid having a view?

**The protocol that works**:
```text
   user      → spine + drift detection
   Claude    → breadth + clarification
   on drift  → user names it; Claude resets from held position
   Claude won't always notice. The user will.
```

Skill file: `.claude/skills/am-i-losing-myself/SKILL.md`.

### §12.3 Spine vs breadth

Codex's job is to harden contracts; Claude's is to hold breadth.
Reviewers always tend to compress. Spine becomes ceiling if breadth
isn't explicitly preserved. The four ★ labels in v2+breadth are the
discipline for keeping breadth visible inside a spine-shaped diagram.

**The most-at-risk concepts**: custom tools (a back-arrow that v2 doesn't
show by default), reconciliation (multi-parent ContextBundle), discourse
graph (typed projection of catalog), canvas (UI-layer concern, not
architecture). These are not visible from "Send → bundle → turn →
items." If they're not labeled on v2, they get forgotten.

---

## §13 Workflow (commit hygiene)

```text
   - Docs branch separation: .md files commit ONLY on the docs branch
     (e.g., docs/current-mental-model-local).
   
   - NEVER merge docs/ to main: docs are private ("secret sauce").
   
   - Cherry-pick code commits only: for main, pick only
     .clj/.cljc/.cljs/.json.
   
   - Force-add past .gitignore: use `git add -f <path>` to commit
     ignored files on the docs branch without modifying .gitignore.
     
   - Two .md files (skill + architecture doc) get separate commits
     with `docs:` prefix matching existing commit style.
```

---

## §14 Companions / lineage

```text
   v1 (full buildout chat synthesis)   chat 2026-05-10 + agent-track-aor.md
   v2 architecture                     llm-track-v2.md (Codex-gated, with
                                       breadth labels)
   THIS canonical doc                  full session 2026-05-10
   Compute-track sibling               slice-a-compute-run-command.md
                                       (4-tier asymmetry, back-arrow rule,
                                        claim/grant pattern)
   Three-depot framing                 three-depot-current-system.md
                                       (World / Compute / LLM)

   Skills:
     am-i-losing-myself                drift discipline (this session)
     ask-codex-for-feedback            packaging artifacts for Codex review
     ingest-codex-feedback             ingesting Codex review constructively
     second-order-mirror               accommodation/defensiveness check
     save-from-myself                  end-of-session retrospective
     rama-pitfalls                     Rama domain checklist
```

---

## §15 Question Index

The 14 substantive questions from session 2026-05-10, mapped to sections.

```text
   Q1   How does Codex work internally?                            §1, §2
        Build a harness for it.                                    §7, §8

   Q2   Reply built-from or informed-by tool calls?                §1.1
        How to implement branching/forks?                          §4, §7
        Each fork is its own chat ID.                              §4

   Q3   Forking cost & caching mechanics.                          §3
        10 forks cached?                                           §3.3
        Cost of doing this system.                                 §3.4

   Q4   Show the internal loop (call/streaming shape).             §1.2, §1.3

   Q5   Full buildout architecture POV.                            §7, §8

   Q6   Where does the chat artifact live?                         §6
        Breakdown into action requests?                            §6.4, §8.1
        Notes referencing chat as further message.                 §6.3, §8.1

   Q7   Depots/PStates/topology — what goes where?                 §5
        Source of truth?                                           §5.1
        Bottleneck concern?                                        §5.4

   Q8   Use diagrams over walls of text.                           §12.1

   Q9   Cases: fresh run, follow-up, slice, edit.                  §9.1-§9.5
        World-first or LLM-direct?                                 §8.3

   Q10  Did Codex neuter breadth?                                  §11, §12.3
        Spine vs breadth tension.

   Q11  Claude losing himself — protocol for grounding.            §12.2

   Q12  Write down the skill.                                      §12.2
        New architecture diagram in original thread.               §7

   Q13  Thinner diagrams (spine, containment, fork DAG).           §7.1

   Q14  Commit workflow (docs branch separation).                  §13
```

If a question can't be answered from this doc, the doc is incomplete —
edit and re-test.
