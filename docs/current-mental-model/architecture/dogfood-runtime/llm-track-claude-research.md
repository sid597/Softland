# LLM Track — Claude side: surfaces, policy, architecture
*Research notes, 2026-05-10. Mirrors the §2 shape of `llm-track-canonical.md` (Codex). To be reviewed by Codex, then merged into the canonical doc as §2′ once verified.*
*Revision 2 (2026-05-11): incorporated Codex review findings (fatal 1–4, soft 1–8). Verified `claude mcp serve` exists (`main.tsx:524-526`, `entrypoints/mcp.ts:35`) and `ask() → QueryEngine.submitMessage` factoring (`QueryEngine.ts:1186/1249/1288`). Split policy boundary from Softland invariant in §3.5.*
*Revision 3 (2026-05-11): post-gate cleanup. Stale text fixes: §0 provenance row, §1 section title and intro, §7.1 instance/schema notation, §7.4 path. Substantive additions: §3.1 strip-or-fail-closed rule for `CLAUDE_CODE_OAUTH_TOKEN`, §4.7 audit/ingestion ledger guardrail, §7.3 reframed as reconciliation (not rename — canonical already uses `:llm/turn-run-request`, verified at `llm-track-canonical.md:1372, 1416, 1503`). §8.1 path question narrowed to per-project mapping.*

---

## §0 What this document is

A consolidated record of research into how Softland can use Claude programmatically. It maps the Claude side onto the Codex framing already established in `llm-track-canonical.md` (§2 Codex protocol) and adds the parts that don't transfer cleanly — the OAuth/subscription policy boundary, the multi-user question, and a passive-observer pattern that doesn't have a Codex analog.

**Provenance of findings:**

| Source | Used for |
|---|---|
| `code.claude.com/docs/en/headless` | CLI flags, output formats, bare mode |
| `code.claude.com/docs/en/cli-reference` | Exhaustive flag list (verified line-by-line) |
| `code.claude.com/docs/en/agent-sdk/streaming-vs-single-mode` | Streaming-input AsyncIterable model |
| `code.claude.com/docs/en/mcp` | MCP integration, including `claude mcp serve` |
| Leaked Claude Code source at `github.com/tanbiralam/claude-code` (npm leak, 2026-03-31, ~512K LOC) | Verifying actual implementation. Architecture only — no code reuse |
| TechCrunch / The Register / VentureBeat / paddo.dev / Hacker News | OpenClaw policy timeline and ToS interpretation |
| Check Point Research CVE-2025-59536 / CVE-2026-21852 | Adjacent security context |
| `github.com/NousResearch/hermes-agent` | One reference implementation that uses Claude correctly |

**Files read directly to verify claims:**
- `src/QueryEngine.ts` (1,295 lines) — core conversation lifecycle
- `src/query.ts` (1,729 lines) — agent loop body, model invocation, tool dispatch
- `src/cli/print.ts` (5,594 lines) — print-mode entry, bidirectional protocol
- `src/main.tsx` (4,683 lines) — CLI flag definitions, routing
- Directory listings for `entrypoints/sdk/`, `coordinator/`, `services/api/`

---

## §1 The six Claude surfaces

The same kind of table the Codex canonical doc has at §2.1. All six rows are real surfaces; the last one (`claude mcp serve`) is the Claude-as-tool surface for other LLM hosts, not the Softland executor surface.

| # | Surface | Use when | Drive how | Closest Codex equivalent |
|---|---|---|---|---|
| 1 | `claude` (interactive TUI) | Human at keyboard | Terminal, React/Ink render | `codex` (TUI) |
| 2 | `claude -p "..."` | One-shot, plain stdout | Subprocess, parse text | `codex exec` |
| 3 | `claude -p --output-format json` | One-shot, structured + metadata | Subprocess, parse JSON: `result`, `session_id`, `num_turns`, `total_cost_usd`, optional `structured_output` | `codex exec --json` |
| 4 | `claude -p --input-format stream-json --output-format stream-json` (with `--include-partial-messages`, `--include-hook-events`, `--replay-user-messages`, `--permission-prompt-tool`) | Long-lived bidirectional stdio, mid-session control, host-mediated approvals | Subprocess; newline-JSON in / newline-JSON out; `control_request`/`control_response` envelopes for non-conversational ops | **`codex app-server`** |
| 5 | Claude Agent SDK (Python / TypeScript) | In-process agent loop, library-style | `query()` async generator; `canUseTool` callback; AsyncIterable input | Library wrapper around #4 — no direct Codex equivalent (Codex doesn't ship an in-process SDK; the equivalent is "write your own client against `app-server`") |
| 6 | `claude mcp serve` | Expose Claude Code as an MCP server/tool to another host LLM | stdio MCP server; lists Claude Code's tools (Read, Write, Edit, Bash, etc.) and handles `CallToolRequest` by invoking them | `codex mcp-server` |

Row 6 verified at `main.tsx:524-526` (the early subcommand check: `if (mcpIndex !== -1 && cliArgs[mcpIndex + 1] === 'serve')`) and `entrypoints/mcp.ts:35` (`startMCPServer()` registers `ListToolsRequestSchema` and `CallToolRequestSchema` over `StdioServerTransport`). Not the recommended Softland executor surface — use surface #4 (stream-json) for the Rama LLM-track control plane. `mcp serve` is the Claude-as-tool surface, intended for other LLM hosts (e.g., another agent) to call into Claude Code as a tool.

**Recommended surface for Softland's LLM-track executor: #4.** Same wire-format shape as Codex's app-server, same long-lived-stdio pattern. The Agent SDK is a thin wrapper around this and adds no protocol capability the CLI doesn't already expose.

---

## §2 Wire format and protocol (verified against `print.ts`)

### §2.1 Format

Newline-delimited JSON over stdio, both directions. **No JSON-RPC envelope, no Content-Length headers.** Each side writes a JSON object, then `\n`, then flushes. `installStreamJsonStdoutGuard()` (`print.ts:594`) diverts stray non-JSON writes (debug prints, library banners) to stderr to keep the stream parseable.

### §2.2 Inbound envelopes (host → Claude on stdin)

The control_request subtypes listed below are the **Softland-required core subset** verified at `print.ts:2830` for the protocol shape Softland's executor needs. The actual protocol is broader — `print.ts` also handles `set_max_thinking_tokens`, `mcp_status`, `mcp_message`, `mcp_set_servers`, additional auth flows, `remote_control`, `side_question`, `stop_task`, settings updates, and others. Treat the list below as the minimum executor surface to implement, not the complete protocol specification. Regenerate from the leaked schema files (`entrypoints/sdk/coreSchemas.ts`, `controlSchemas.ts`) per Claude Code version upgrade.


```jsonc
// Conversational user message:
{"type":"user",
 "message":{"role":"user","content":"Read README.md and summarize"}}

// Multimodal user message:
{"type":"user",
 "message":{"role":"user",
            "content":[{"type":"text","text":"..."},
                       {"type":"image","source":{...}}]}}

// Control request (verified at print.ts:2830):
{"type":"control_request",
 "request_id":"<uuid>",
 "request":{"subtype":"interrupt"}}              // abort current turn
{"type":"control_request",
 "request_id":"<uuid>",
 "request":{"subtype":"end_session"}}            // graceful shutdown
{"type":"control_request",
 "request_id":"<uuid>",
 "request":{"subtype":"initialize",
            "systemPrompt":"...",
            "appendSystemPrompt":"...",
            "agents":{...},
            "hooks":{...},
            "jsonSchema":{...},
            "promptSuggestions":true,
            "sdkMcpServers":[...]}}              // first-message setup
{"type":"control_request",
 "request_id":"<uuid>",
 "request":{"subtype":"set_permission_mode",
            "mode":"default|acceptEdits|bypassPermissions|plan|dontAsk",
            "ultraplan":bool}}
{"type":"control_request",
 "request_id":"<uuid>",
 "request":{"subtype":"set_model",
            "model":"<model>|default"}}
```

### §2.3 Outbound envelopes (Claude → host on stdout)

```jsonc
// 1. Init / system metadata (always first unless plugin_install precedes):
{"type":"system","subtype":"init","session_id":"<uuid>",
 "model":"<model>","tools":[...],"mcp_servers":[...],"plugins":[...]}

// 2. Raw API stream events (with --include-partial-messages):
{"type":"stream_event","session_id":"...","uuid":"...",
 "event":{"type":"content_block_delta","index":0,
          "delta":{"type":"text_delta|input_json_delta|thinking_delta",
                   "text":"..."}}}

// 3. Hook lifecycle events (with --include-hook-events):
//    Verified at print.ts:628-674
{"type":"system","subtype":"hook_started",
 "hook_id":"...","hook_name":"...","hook_event":"PreToolUse|PostToolUse|...",
 "uuid":"...","session_id":"..."}
{"type":"system","subtype":"hook_progress",
 "stdout":"...","stderr":"...","output":"..."}
{"type":"system","subtype":"hook_response",
 "exit_code":0,"outcome":"...","output":"..."}

// 4. Assistant message (one per content block; usage/stop_reason mutated on message_delta):
{"type":"assistant","message":{...standard Anthropic message shape...}}

// 5. Tool result (assembled from tool execution):
{"type":"user","message":{"role":"user",
                          "content":[{"type":"tool_result",...}]}}

// 6. Control response (matched to inbound request_id):
{"type":"control_response",
 "response":{"subtype":"success|error","request_id":"<uuid>",...}}

// 7. Status / metadata events:
{"type":"system","subtype":"status",
 "permissionMode":"...","status":null,"uuid":"...","session_id":"..."}
{"type":"rate_limit_event","rate_limit_info":{...}}
{"type":"auth_status","isAuthenticating":bool,"output":"...","error":"..."}

// 8. API retry signal:
//    Verified at QueryEngine.ts:944-955
{"type":"system","subtype":"api_retry",
 "attempt":1,"max_retries":5,"retry_delay_ms":2000,
 "error_status":429,"error":"rate_limit","uuid":"...","session_id":"..."}

// 9. Compact boundary (when context-budget pipeline fires):
{"type":"system","subtype":"compact_boundary",
 "compact_metadata":{...},"uuid":"...","session_id":"..."}

// 10. Result (terminal):
{"type":"result",
 "subtype":"success|error_max_turns|error_max_budget_usd|
            error_max_structured_output_retries|error_during_execution",
 "is_error":bool,"duration_ms":N,"duration_api_ms":N,
 "num_turns":N,"stop_reason":"...","session_id":"...",
 "result":"...","total_cost_usd":N.NN,
 "usage":{...},"modelUsage":{...},
 "permission_denials":[{tool_name,tool_use_id,tool_input}],
 "fast_mode_state":{...},"uuid":"..."}
```

`--replay-user-messages` echoes accepted stdin envelopes back on stdout — functions as ACK.

### §2.4 Setup handshake

No `initialize`/`initialized` two-step like Codex. The first `system/init` event Claude emits IS the handshake; it carries model, tool list, MCP servers, loaded plugins. The host can also send a `control_request {subtype: "initialize", ...}` to override systemPrompt, register hooks, declare SDK MCP servers, etc. — verified at `print.ts:2863-2917` and `print.ts:4336` (`handleInitializeRequest`).

### §2.5 Submission ops mapping

| Op | Claude mechanism | Codex equivalent |
|---|---|---|
| Start a new conversation | Open subprocess + first `{"type":"user",...}` envelope | `thread/start` |
| Add user input mid-session | Another `{"type":"user",...}` on stdin | `turn/start` |
| Mid-turn steering | **No direct equivalent.** Supported approximation: `control_request {subtype:"interrupt"}` to abort the current turn, followed by a new `user` envelope to re-prompt with the steering content. Streaming-input mode also supports queued user messages and tool-request denial/guidance via `--permission-prompt-tool` MCP roundtrip | `turn/steer` (no direct analog; injects steering into the still-running turn while preserving execution) |
| Interrupt | `control_request {subtype:"interrupt"}` or SIGINT | `Interrupt` / `turn/interrupt` |
| Approve/deny tool | Host MCP tool returns allow/deny/modify (via `--permission-prompt-tool`) | `ExecApproval` / `PatchApproval` |
| Set permission mode mid-session | `control_request {subtype:"set_permission_mode", ...}` | (no direct equivalent — Codex uses sandbox config at start) |
| Switch model mid-session | `control_request {subtype:"set_model", ...}` | (no direct equivalent) |
| Resume after process death | New `claude -p ... --resume <session-id>` | `thread/resume` |
| Fork at a checkpoint | `--resume <id> --fork-session` | `thread/fork` |
| Compact | **No host-driven programmatic compact.** Claude does auto/micro/reactive compaction internally and emits `compact_boundary` events when it fires (verified at `query.ts:413-426` microcompact, `query.ts:454-505` autocompact). The host can observe boundaries but cannot trigger compaction. `/compact` slash command is interactive-only | `thread/compact` (Codex has host-driven; Claude has only internal) |
| Shutdown | `control_request {subtype:"end_session"}` or close stdin | `Shutdown` |

### §2.6 Event types

Smaller and less curated than Codex's 78 variants. Verified categories:
- **System**: `init`, `api_retry`, `plugin_install`, `hook_started/progress/response`, `compact_boundary`, `status`, `local_command`
- **Stream events** (raw Anthropic SSE): `message_start`, `content_block_start`, `content_block_delta` (text_delta / input_json_delta / thinking_delta), `content_block_stop`, `message_delta` (carries final stop_reason and usage), `message_stop`, `ping`
- **Assistant** / **User** / **Attachment** (with subtypes `structured_output`, `max_turns_reached`, `queued_command`)
- **`tool_use_summary`**, **`tombstone`** (control signal to remove a previously-yielded message)
- **`stream_request_start`**, **`auth_status`**, **`rate_limit_event`**
- **Result** (terminal — five subtypes listed in §2.3.10)

The protocol shapes are documented in prose, not in a published JSON schema like Codex's `app-server-protocol/schema/json/*.json`. Treat as a soft contract; pin the Claude Code version; regenerate parsers per upgrade. The schema files exist in the leaked tree at `src/entrypoints/sdk/coreSchemas.ts` (56KB) and `controlSchemas.ts` (19KB) but aren't published independently.

---

## §3 The auth boundary — `--bare`, OAuth, API key

This is the part that has no Codex analog and is the single most important thing to get right.

### §3.1 The two paths Softland uses

Claude Code has more auth paths than the two Softland uses. The full set includes (in approximate priority order, from official auth docs): cloud provider credentials (Bedrock/Vertex/Foundry), `ANTHROPIC_AUTH_TOKEN`, `ANTHROPIC_API_KEY`, `apiKeyHelper`, `CLAUDE_CODE_OAUTH_TOKEN` (a long-lived OAuth token generated via `claude setup-token`, designed for CI), and finally subscription OAuth read from the keychain after `claude /login`.

**Softland deliberately recognizes only a narrow subset:**

1. **Subscription mode**: Softland spawns local `claude` without `--bare`. Claude Code reads its own local keychain/login state from a **sanitized child env**. If `CLAUDE_CODE_OAUTH_TOKEN` is present in Softland's parent env (a documented Anthropic variable that some other tool might have set), **strip it from the child env or fail closed** — do not pass it through. Softland's process never touches the token; the binary handles its own auth from local state only.
2. **API-key mode**: Softland spawns local `claude --bare` with `ANTHROPIC_API_KEY` injected at spawn time. The `--bare` flag (`main.tsx:976`) skips OAuth/keychain reads entirely, so it's structurally impossible for the subprocess to drift onto a subscription credential.
3. **Passive-observe mode**: no spawn at all (see §4.7).

We deliberately do not use the `CLAUDE_CODE_OAUTH_TOKEN` env-var path even though it is a documented Anthropic feature. Injecting an OAuth token into an env var that Softland's process owns *would* place Softland in the OAuth-handling business — even if doing so is permitted under the documented Anthropic policy for that variable. The strip-or-fail-closed rule in mode 1 makes this structural rather than aspirational. See §3.5 for the policy/invariant distinction.

### §3.2 The structural property `--bare` enforces

`--bare` is the **policy boundary marker**. From the leaked `main.tsx:976` flag definition:

> "Minimal mode: skip hooks, LSP, plugin sync, attribution, auto-memory, background prefetches, **keychain reads**, and CLAUDE.md auto-discovery. Sets CLAUDE_CODE_SIMPLE=1. **Anthropic auth is strictly ANTHROPIC_API_KEY or apiKeyHelper via --settings (OAuth and keychain are never read).**"

This is structurally important: bare-mode invocations are *incapable* of doing the OpenClaw thing. The supported programmatic path is on the side of the fence where the prohibited path can't even be expressed.

### §3.3 The OpenClaw timeline (why this matters)

| Date | Event |
|---|---|
| Nov 2025 | OpenClaw originally released as "Clawdbot" by Peter Steinberger |
| Early Jan 2026 | Anthropic begins technical crackdown |
| Jan 9, 2026 | Server-side fingerprinting deployed; spoofed clients rejected |
| Feb 19, 2026 | Consumer ToS updated with explicit "Authentication and credential use" clause: *"OAuth authentication is only intended for Claude Code and Claude.ai, and using OAuth tokens in any other product, tool, or service constitutes a violation."* |
| Apr 4, 2026 | Subscription-tier ban for OpenClaw — Pro/Max accounts can no longer drive third-party harnesses |
| Apr 10, 2026 | Steinberger temporarily banned from Claude entirely (TechCrunch) |

### §3.4 Three layered violation mechanisms (what got OpenClaw banned)

1. **OAuth-token misuse (legal layer).** Tokens scoped for Claude Code were used by a third-party client.
2. **Client spoofing (technical layer).** Headers impersonated `claude-code/...` to access subscription endpoints. Detected since Jan 2026.
3. **Subscription arbitrage (economic layer).** $200/month subscription consuming $1,000-5,000/day API-equivalent traffic via autonomous loops.

### §3.5 Two tests — the policy boundary and the Softland invariant

These are different and the doc previously conflated them. Use both.

**Test A — Anthropic policy boundary** (what crosses Anthropic's actual line):

> Is Softland *offering Claude.ai login*, *storing/routing Free/Pro/Max consumer credentials*, or *using subscription OAuth to serve a third-party product/workload on behalf of users*?

If yes → high-risk OpenClaw shape, likely violation under current Consumer Terms ("Authentication and credential use", Feb 2026). If no → likely permitted under documented narrower readings (e.g., `CLAUDE_CODE_OAUTH_TOKEN` in CI env is allowed; a developer running their own scripts with their own OAuth is allowed).

**Test B — Softland safety invariant** (stricter than policy, on purpose):

> Does Softland's own process read, store, log, forward, or env-inject any OAuth token, ever?

If yes → reject the design even if it would pass Test A. Softland deliberately stays inside a narrower box than Anthropic's policy actually requires, because:

1. The narrow box is structurally enforceable (the executor's three modes from §3.1 cannot express OAuth handling).
2. It eliminates an entire category of policy drift — if policy interpretation shifts, Softland is still safe.
3. It removes a private-data attack surface (OAuth tokens are powerful secrets).

The combined effect of both tests:

| Pattern | Test A (policy) | Test B (Softland invariant) | Decision |
|---|---|---|---|
| Spawn local `claude -p`, no token handling | ✅ | ✅ | Use |
| `claude --bare -p` with `ANTHROPIC_API_KEY` | ✅ | ✅ | Use |
| Read user's local session JSONL (passive observer) | ✅ | ✅ | Use |
| Inject `CLAUDE_CODE_OAUTH_TOKEN` env var into spawn | ✅ | ❌ | **Reject** (Softland-stricter) |
| Sid's central server holds users' OAuth, spawns Claude Code per user | ❌ | ❌ | Reject (both) |
| Softland reads OAuth from disk and presents on the wire | ❌ | ❌ | Reject (both) |

The "Softland's process never holds OAuth" rule survives as §7.1 #3 — it's the operational form of Test B.

### §3.6 Adjacent security context (CVEs)

- **CVE-2025-59536** and **CVE-2026-21852** — RCE and API-key exfiltration via Claude Code's hooks, MCP servers, and env vars. Untrusted repos could exfiltrate `ANTHROPIC_API_KEY` when cloned and opened.
- Mitigation: never auto-load hooks or MCP from untrusted repos. `--bare` mode helps because it skips auto-discovery.
- Relevant to Softland because we use hooks and `.mcp.json`.

---

## §4 Multi-user / multi-subscription patterns

Conversation explored several patterns. Verdicts:

### §4.1 Pattern A — Pooling one subscription across many humans

```
Many users → Softland → uses Sid's OAuth → Claude
```

❌ **OpenClaw shape; explicitly banned.** Sid's subscription serving multiple humans' requests is per-person-license violation regardless of where the work is stored or how the requests are framed.

### §4.2 Pattern B — Each user provides OAuth to a central Softland

```
User A logs into Claude via Softland → Softland stores A's OAuth →
                                       Softland uses A's OAuth → Claude
(Repeat per user)
```

❌ **Also OpenClaw shape.** Doesn't matter that each user has their own subscription; Softland-server holding any user's OAuth token is the violation. This is literally what OpenClaw did per-user.

### §4.3 Pattern C — Per-user containers on Sid's central server

```
Sid's server hosts container-A (User A's keychain) → spawns claude in container
                                                    → outbound from Sid's IP
```

❌ for Sid-hosted multi-user containers. The strongest enforcement frame isn't "same egress IP" (that's incidental); it's that Sid would be **operating a third-party developer product that routes requests through user-plan credentials on behalf of those users**. Container isolation doesn't change Softland's relationship to those credentials — Softland is still the orchestration layer that knows which user, when, and which token.

**Allowed-adjacent carve-out**: a user's own remote dev container, or an organization-managed dev environment that the user personally administers, where the user has logged Claude Code in directly and Softland is not the credential-routing product. That's just Pattern D (BYO compute) running on a remote machine the user owns — fine. The distinction is *who operates the credential-handling layer*, not where the bytes physically live.

### §4.4 Pattern D — Per-user local Softland-client (BYO compute)

```
User A's machine: Softland-client (local) → spawns local claude → A's local OAuth
                                            ↓ events (no auth)
                              Sid's central infra: Rama (shared truth)
```

✅ **Allowed.** Each user runs Softland-client locally. Their local Claude Code uses their local OAuth on their machine. Only events flow to central Rama; auth never crosses machines. This preserves the "personal harness" structural property by replication.

### §4.5 Pattern E — Per-user API key (BYO key)

```
User A pastes ANTHROPIC_API_KEY into Softland → Softland uses A's key
```

✅ **Allowed.** API keys are explicitly designed for third-party use. Pay-per-token. Trivial multi-tenant model.

### §4.6 Pattern F — Softland-as-customer (consolidated SaaS)

```
Softland has one Anthropic API account → charges users per usage
```

✅ **Allowed.** Standard B2B/B2C SaaS pattern. API key billing, internal accounting.

### §4.7 Pattern G — Passive observer (read user's local session JSONL)

```
User runs claude normally on their machine → transcripts persist locally
Softland-client (on user's machine) → tails ~/.claude/projects/*.jsonl → Rama
```

✅ **Allowed and the cleanest of the multi-user patterns from an auth/policy standpoint.** Softland never makes a Claude API call; it reads the user's own data files (their property under Anthropic's Consumer Terms) and indexes them. No OAuth handling, no initiation of Claude work, indifferent to subscription/API distinction.

**But not without responsibilities.** Once Softland-client ships transcripts to central Rama, it becomes a privacy and data-processing surface even though it's not an Anthropic auth violation. The canonical version of this pattern must include:

| Guardrail | Why |
|---|---|
| **Explicit per-user opt-in**, with revocation | Transcripts contain everything the user told Claude — secrets, credentials, private code, API keys, drafts. Default-off is mandatory |
| **Workspace scoping** | Only ingest transcripts from explicitly-allowlisted project directories |
| **Secret/token redaction at ingest** | Pattern-match for common secrets before they hit Rama; redact before persistence |
| **Retention and deletion controls** | Match or exceed Claude Code's local 30-day retention; provide user-driven deletion that propagates to Rama |
| **Enterprise/team policy awareness** | In team contexts (Claude for Work, etc.), respect any org-level policy that constrains transcript sharing |
| **Audit / ingestion ledger** | A Rama PState that records, per ingested file/range: when it was read, what was redacted, what was skipped, what was shipped to which depot, and any subsequent deletions. Makes transcript ingestion accountable and reversible. Not policy-critical, but necessary for a Rama-backed system to make the "world records what inhabitants do" claim auditable rather than aspirational |

**Path correction**: transcripts live at `~/.claude/projects/` (verified) — *not* `~/.claude/sessions/` as the earlier draft speculated. Plaintext JSONL, 30-day default retention.

This pattern aligns with Softland's "world records what inhabitants do" thesis. It's compatible with any auth method the user used with Claude (subscription, API key, Bedrock, etc.) because Softland just reads what's already on disk.

### §4.8 The synthesis

| Workload type | Recommended pattern |
|---|---|
| Sid's interactive coding/exploration | D (local Softland-client + subscription) |
| Sid's batch synthesis runs across the world | D + subscription |
| Other users contributing knowledge | G (passive observer) — captures their existing Claude usage |
| Other users actively triggering LLM features in Softland UI | E (BYO API key) — explicitly fine, pay-per-token |
| Scheduled jobs / autonomous loops | E or F — never subscription |
| Softland-as-product later | F |

**Key rule for the LLM-track executor architecture:**

> Softland's process never holds an OAuth token. The executor knows three modes only:
> - `:subscription` — spawns local `claude` non-bare, OAuth handled by the binary
> - `:api-key` — sets `ANTHROPIC_API_KEY` from a per-call secret, spawns with `--bare`
> - `:passive-observe` — reads user's local session JSONL, no spawn at all

Make this a structural property of the executor. Reject any code path that tries to read or store an OAuth string.

---

## §5 Architecture findings from reading the leaked source

### §5.1 The two-layer agent loop

| Layer | File | Role |
|---|---|---|
| Outer (`QueryEngine.submitMessage`) | `QueryEngine.ts:209` | Per-conversation lifecycle: messages, transcripts, permission denials, cost, terminal envelope, max-turns / max-budget enforcement |
| Inner (`queryLoop`) | `query.ts:241` | Per-turn body: context-budget pipeline, model invocation, tool dispatch, fallback retries, stop hooks |

Both yield SDK messages; the outer filters/normalizes/transforms what the inner emits.

### §5.2 The actual API call

`query.ts:659`:
```typescript
for await (const message of deps.callModel({
  messages: prependUserContext(messagesForQuery, userContext),
  systemPrompt: fullSystemPrompt,
  thinkingConfig, tools, signal,
  options: { model, fallbackModel, mcpTools, taskBudget, ... }
}))
```

`deps.callModel` is dependency-injected (`productionDeps()`); production lives in `services/api/claude.ts` (3,419 lines). Uses `@anthropic-ai/sdk/resources/messages.mjs` — the **official SDK** does the HTTP. The agent loop is auth-agnostic; auth is determined at SDK-init time by the env (`ANTHROPIC_API_KEY` or OAuth keychain).

### §5.3 Tool dispatch is concurrent and streaming

Surprising finding: tools dispatch as `tool_use` blocks arrive, **not after the assistant message completes**.

`query.ts:561-568`:
```typescript
const useStreamingToolExecution = config.gates.streamingToolExecution
let streamingToolExecutor = useStreamingToolExecution
  ? new StreamingToolExecutor(toolUseContext.options.tools, canUseTool, toolUseContext)
  : null
```

`query.ts:826-844` — tools added to executor as they arrive; results consumed while model still streams. Permission checks happen inside the executor via `canUseTool`. On streaming fallback or abort, executor is `discard()`ed and orphan partial messages get **tombstoned** (sent as `tombstone` envelopes to remove them from the host's view).

### §5.4 The context-budget pipeline (runs before every API call)

```
each iteration:
  applyToolResultBudget(...)              # cap tool-result aggregate size
  if HISTORY_SNIP:    snipCompactIfNeeded(...)   # remove zombies
  microcompact(...)                       # cache-aware mini-compaction
  if CONTEXT_COLLAPSE: applyCollapsesIfNeeded(...)
  autocompact(...)                        # full summary if over threshold
  → call model
```

Each step yields a boundary message if it fires. `taskBudget.remaining` is recomputed across compaction boundaries (`query.ts:283-291`) because the server-side prompt renderer can't see history that got summarized away. This is more compaction infrastructure than I'd naively assume.

### §5.5 Print mode vs TUI

The fork happens at `main.tsx:602` (and again at 786, 800, 2826):
```typescript
if (process.argv.includes('-p') || process.argv.includes('--print')) {
  // ... print mode setup ...
}
```

| Aspect | TUI (`claude`) | Print (`claude -p`) |
|---|---|---|
| Entry | React/Ink render tree | `runHeadless()` → `runHeadlessStreaming()` (`print.ts:455, 976`) |
| Loop driver | `ask()` wrapper (`QueryEngine.ts:1186`) which instantiates `new QueryEngine(...)` (line 1249) and delegates via `yield* engine.submitMessage(...)` (line 1288) | Same path: `runHeadlessStreaming → ask() → QueryEngine.submitMessage`. Both modes go through the unified ask-wrapper now; the factoring described at `QueryEngine.ts:175-183` has already happened |
| Model loop | Same `query()` in `query.ts` | Same `query()` in `query.ts` |
| Input | Keyboard via Ink `<TextInput>`, vim, voice | `string` argv OR `AsyncIterable<string>` from stdin |
| Output | Ink renders messages | `output.enqueue(msg)` → JSONL/text/JSON on stdout |
| Slash commands | Same `processUserInput()`; some open interactive dialogs | Same; interactive ones (`/compact`) unavailable in `-p` |
| Permission prompts | Modal Ink dialog | `--allowedTools` baseline + `--permission-prompt-tool` MCP roundtrip |
| Mid-session control | Keyboard shortcuts | `control_request` envelopes |
| Settings reactivity | `useSettingsChange` hook | Direct `settingsChangeDetector.subscribe` |
| Memory | React unmount | `setInterval(Bun.gc, 1000)` |
| Stdout safety | Ink renders | `installStreamJsonStdoutGuard()` |
| Sandbox approval | Modal | Forward to host via `can_use_tool` control_request |
| Resume | Interactive picker | `--resume <id>` flag |
| Hook events | UI status indicators | Streamed as JSONL when `--include-hook-events` |
| Trust dialog | Shown on first run | Skipped (`main.tsx:363`) |
| Auto-resume | Manual session pick | `turnInterruptionState` continues mid-turn after restart |
| Plugin loading | Full discovery | Cache-only (`loadAllPluginsCacheOnly`) |

The `QueryEngine.ts:175` comment is explicit: *"It extracts the core logic from ask() into a standalone class that can be used by both the headless/SDK path and (in a future phase) the REPL."* So the unification is in progress — print is the new path, TUI is being migrated.

### §5.6 Architectural lesson for Softland

Anthropic factored the loop OUT of the TUI before factoring it INTO the TUI again. The print mode was built first as a clean abstraction, the TUI is being migrated onto it. Softland's LLM track should adopt the same inversion: build the headless event-stream first, treat any UI as another consumer of the same stream.

---

## §6 hermes-agent — one verified reference implementation

Verified against `skills/autonomous-ai-agents/claude-code/SKILL.md` in the repo. Hermes uses Claude via three patterns:

```bash
# Print mode (one-shot tasks):
claude -p 'Task description' --allowedTools 'Read,Edit' --max-turns 10

# tmux orchestration (multi-turn interactive):
tmux new-session -d -s claude-work -x 140 -y 40
tmux send-keys -t claude-work 'claude' Enter
tmux capture-pane -t claude-work -p -S -50

# Bare mode (CI / scripted):
claude --bare -p 'task'   # requires ANTHROPIC_API_KEY
```

Hermes parses the JSON output's `result`, `session_id`, `num_turns`, `total_cost_usd`. **Hermes does NOT use the bidirectional stream-json protocol** — it's the equivalent of `codex exec`, not `codex app-server`. Useful as a reference for the simplest case but not the right shape for a Rama-driven runtime.

---

## §7 Recommendations for Softland's LLM-track architecture

### §7.1 Executor structural rules (mandate at design level)

1. **Always spawn `claude -p --input-format stream-json --output-format stream-json`** with the streaming flags (`--include-partial-messages`, `--include-hook-events`, `--replay-user-messages`). This is the app-server analog and the only mode that supports the back-arrow cleanly.

2. **Auth-mode is per-request, not per-deployment.** The LLM-run-request envelope carries:
   ```clojure
   ;; instance:
   {:llm/request-id "..."
    :llm/auth-mode  :subscription      ;; scalar; one of #{:subscription :api-key :passive-observe}
    :llm/triggered-by {:agent :sid}    ;; or {:agent :user-N} / {:agent :scheduled} / {:agent :compute-feedback}
    ...}
   ```
   Allowed `:llm/auth-mode` values: `#{:subscription :api-key :passive-observe}`. Default to `:api-key` for any `:triggered-by` other than `:sid` with an interactive session id.

3. **Softland's process never holds an OAuth token.** Hard rule. The executor only knows how to:
   - Set/unset `ANTHROPIC_API_KEY` env at spawn time
   - Spawn the `claude` subprocess
   - Pipe stdio
   No OAuth code, no keychain reads, no token storage. If a future contributor PRs token-handling code, reject it on policy grounds, not just style.

4. **Mandate `--bare` for `:api-key` mode.** Structural guarantee that the subprocess can't accidentally drift onto OAuth.

5. **Validate auth mode against trigger.** If `:triggered-by :sid + :auth-mode :subscription` — fine. If `:triggered-by :user-other + :auth-mode :subscription` — reject at the depot boundary, not at runtime.

### §7.2 Three deployment shapes for Softland

| Shape | Deployment | LLM workloads |
|---|---|---|
| **Personal (now)** | Softland on Sid's laptop | All `:subscription` via local subprocess |
| **Multi-user federated (future)** | Softland-client per user + central Rama for shared truth | Each user's own subscription via their own local Softland-client; passive-observer for additional data |
| **Multi-user dev/preview** | Softland-server + BYO API key per user (transitional) | `:api-key` per user; **requires**: secret vaulting (never write keys to disk plaintext, never log), per-tenant accounting (track tokens/cost per user), explicit user consent for key storage, abuse controls (rate limits per user, anomaly detection), and a documented migration path off this pattern toward Softland-as-customer once usage justifies it. Not a destination — a stepping stone |
| **Multi-user hosted (later, if becomes a product)** | Softland as a service | Softland-as-customer: one or more Anthropic API accounts owned by Softland; users pay Softland; Softland accounts for usage internally; standard B2B/B2C billing |

### §7.3 Capability discriminator across LLM track

This is an evolution of the canonical `:llm/codex-run` shape from `llm-track-canonical.md`, not a rename by fiat. The proposed evolved request shape:

```clojure
{:request/type :llm/turn-run-request   ;; canonical already uses this in lifecycle flows
 :llm/backend  :claude                  ;; scalar instance: :codex | :claude
 :llm/auth-mode :subscription           ;; Claude-only field; one of #{:subscription :api-key :passive-observe}
 :llm/triggered-by {:agent :sid}        ;; routing/policy discriminator
 :context-bundle {...}                  ;; preserved from canonical
 :world-thread "..."                    ;; preserved from canonical
 :world-turn "..."                      ;; preserved from canonical
 :prompt "..."
 :sandbox {...}}
```

Schema notation: `#{:codex :claude}` describes the *type* of `:llm/backend`; an actual instance carries the scalar `:llm/backend :claude`. The earlier draft mistakenly wrote the schema form where an instance value belonged.

**Reconciliation rationale (not a rename)**: the canonical doc already uses `:llm/turn-run-request` in its lifecycle flows (verified at `llm-track-canonical.md:1372, 1416, 1503`) — `:llm/codex-run` is *residual prose* in earlier sections (`llm-track-canonical.md:1267, 1280`). The cleanup is to reconcile that residual prose into the existing `:llm/turn-run-request` shape and add `:llm/backend` as a discriminator. No new request type is being invented; one half of the canonical doc just needs to catch up to the other half.

WorldTurn, ContextBundle, LLMTurnRun, cataloging, and observation-folding semantics are shared between Codex and Claude tracks; the discriminator belongs at the executor capability layer, not at the depot schema layer. That's why one unified request type with a backend tag beats two sibling request types.

**When to fork into sibling request types instead**: if backend-specific lifecycle semantics escape the executor — e.g., if Claude's `compact_boundary` events need different downstream folding than any Codex equivalent — promote that backend's slice into a sibling type. Until that escape happens, one unified type is correct.

Above the executor (request / decision / observation): depot schema stays unified. Inside the executor: wire format diverges (Codex JSON-RPC vs Claude stream-json `control_request`). Executor reads `:llm/backend`, picks the wire-format adapter, spawns the right subprocess.

### §7.4 The passive-observer slice

Worth building early as a separate slice:

```
docs/current-mental-model/architecture/dogfood-runtime/llm-track-passive-observer.md
```

Contents:
- Watch `~/.claude/projects/*.jsonl`
- Parse new lines, link to provenance (which user, which machine, which workspace)
- Ship to Rama as `:llm/observed-conversation` events
- Render in the Softland world as trails

Doesn't require any new auth model; doesn't require coordination with the active LLM track; is the cleanest demo of "world records what inhabitants do."

---

## §8 Honest gaps and open questions

### §8.1 Things to verify before relying on

1. **Schema and per-project path mapping for session JSONL files.** Base path verified as `~/.claude/projects/` (see §4.7), but the per-project subdirectory mapping (how workspace paths are encoded into directory names, whether multiple workspaces share files, how `--resume`/`--continue` discriminate) still needs verification by running `claude` once and inspecting. Schema lives at `entrypoints/sdk/coreSchemas.ts` (56KB) in the leaked tree but treat as soft contract.

2. **Whether `--input-format stream-json` rejects non-`user` envelopes.** Verified that `control_request` works (`print.ts:2830`); haven't verified what happens to malformed envelopes.

3. **Rate-limit headers and how to detect subscription-vs-API limits.** `rate_limit_event` envelope exists; need to test what it carries on each auth path.

4. **Whether tool-use during `permission-prompt-tool` round-trips can be distinguished from normal tool-use in the JSONL.** The host MCP server should receive a structured request; need to verify exact shape.

5. **Behavior on `--bare` if `ANTHROPIC_API_KEY` is unset.** Probably exits with error; need to confirm.

### §8.2 Things Codex has that Claude doesn't (no equivalents)

- Mid-turn steering (`turn/steer`)
- Programmatic `thread/compact`
- Domain-specific approval types (`ExecApproval` vs `PatchApproval` named separately)
- Schema-versioned wire format (`app-server-protocol/schema/json/`)

### §8.3 Things Claude has that Codex doesn't (no equivalents)

- Extended thinking with `thinking_delta` events
- Hook lifecycle events streamed as JSONL
- `--json-schema` final-output validation
- The passive-observer pattern (because Claude Code persists transcripts; Codex's relationship with persistence is different)

---

## §9 Sources

### Public docs
- [Headless mode](https://code.claude.com/docs/en/headless)
- [CLI reference](https://code.claude.com/docs/en/cli-reference)
- [Streaming vs single mode](https://code.claude.com/docs/en/agent-sdk/streaming-vs-single-mode)
- [Claude Code MCP](https://code.claude.com/docs/en/mcp)
- [Tool use overview](https://docs.claude.com/en/docs/agents-and-tools/tool-use/overview)

### Policy / OpenClaw
- [TechCrunch — Anthropic temporarily banned OpenClaw's creator from accessing Claude](https://techcrunch.com/2026/04/10/anthropic-temporarily-banned-openclaws-creator-from-accessing-claude/)
- [The Register — Anthropic closes door on subscription use of OpenClaw](https://www.theregister.com/2026/04/06/anthropic_closes_door_on_subscription/)
- [The Register — Anthropic clarifies ban on third-party tool access to Claude](https://www.theregister.com/2026/02/20/anthropic_clarifies_ban_third_party_claude_access/)
- [VentureBeat — Anthropic cracks down on unauthorized Claude usage by third-party harnesses and rivals](https://venturebeat.com/technology/anthropic-cracks-down-on-unauthorized-claude-usage-by-third-party-harnesses)
- [MindStudio — What Is the OpenClaw Ban?](https://www.mindstudio.ai/blog/anthropic-openclaw-ban-oauth-authentication)
- [paddo.dev — Anthropic's Walled Garden: The Claude Code Crackdown](https://paddo.dev/blog/anthropic-walled-garden-crackdown/)
- [Hacker News — Anthropic Explicitly Blocking OpenCode](https://news.ycombinator.com/item?id=46625918)
- [Hacker News — Anthropic no longer allowing Claude Code subscriptions to use OpenClaw](https://news.ycombinator.com/item?id=47633396)
- [GitHub — Using OpenCode with Anthropic OAuth violates ToS](https://github.com/anomalyco/opencode/issues/6930)

### Security context
- [Check Point Research — RCE and API Token Exfiltration Through Claude Code Project Files](https://research.checkpoint.com/2026/rce-and-api-token-exfiltration-through-claude-code-project-files-cve-2025-59536/)

### Code references (architectural understanding only)
- `github.com/tanbiralam/claude-code` — Claude Code source leaked from npm 2026-03-31 (~512K LOC TypeScript on Bun, ~1,900 files). All code is Anthropic IP; do not copy, only learn architecture
- `github.com/NousResearch/hermes-agent/blob/main/skills/autonomous-ai-agents/claude-code/SKILL.md` — verified reference implementation

---

## §10 Conversation trail (research-notes appendix — do not propagate to normative canonical body)

For the trail-as-knowledge thesis: the questions that produced this doc, in sequence. **This section is provenance, not contract.** Keep it in the research-notes file to preserve the origin-prompt context (aligns with "papers should be logs"); strip it when this doc is merged into the normative §2′ canonical body so the architecture reads as a contract, not a chat transcript.

1. *"What are the equivalent surfaces to Codex's TUI/exec/exec-json/app-server/mcp-server for Claude?"* → §1, §2
2. *"What about hermes-agent — how does it use Claude?"* → §6
3. *"What got OpenClaw banned and what should I avoid?"* → §3
4. *"How does the leaked claude-code repo's loop actually work?"* → §5
5. *"How is `claude -p` implemented vs the TUI?"* → §5.5
6. *"What's the closest SDK-equivalent using my subscription?"* → §3, §4.1-§4.3
7. *"Why does my multi-user concern apply to Softland if it's stateless and storage-only?"* → §4 (refined the "multi-user" framing)
8. *"What if users have their own subscriptions through Softland?"* → §4.2, §3.5 (the OpenClaw-shape result)
9. *"Why provide `claude -p` for personal harnesses if I can't use it that way?"* → §4 ("personal" is structural)
10. *"What if it's a per-user container?"* → §4.3 (depends on where it runs)
11. *"What if Softland reads the user's local Claude session JSONL?"* → §4.7 (the passive observer — best pattern)

---

## §11 Next steps

- [ ] Send this to Codex for review (use `ask-codex-for-feedback` skill); incorporate into canonical as §2′
- [ ] Verify §8.1 open questions — especially the session JSONL path and schema
- [ ] Draft `llm-track-passive-observer.md` as a sister slice
- [ ] Update `slice-a-compute-run-command.md` template to include the auth-mode / triggered-by discriminator pattern (§7.3) — same fields apply to compute track even if the auth concern is different
- [ ] Add the §3.5 diagnostic test to `rama-pitfalls` skill so future Softland design reviews catch any drift toward token-handling code
