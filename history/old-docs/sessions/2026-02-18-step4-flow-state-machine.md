# Session: 2026-02-18 — Step 4: Wire Prompt Templates to UI Actions

## What was done
Implemented the V0 flow state machine from commission-consensus.md Section 4. This is the "prompts as API calls" paradigm — each UI action composes a templated prompt and sends it through the existing SSE streaming infrastructure.

## Implementation (6 Stages)

### Stage 1: Flow state machine (pure fns)
- `flow-transitions` — directed graph encoding Section 4 state graph
- `valid-transition?` — checks graph + human override (any → `:intake` or `:arrange`)
- `initial-flow-state` — `{:node :idle :tickets [] :selected [] :arrangement nil :session-id nil :history []}`
- `transition-flow-state` — pure fn, returns new state or nil, appends history

### Stage 2: Prompt templates
- `flow-prompt` — takes `[action flow-state]`, returns `{:prompt "..." :system-instruction nil}`
- Actions: `:bootstrap`, `:run-sequential`, `:run-parallel`, `:rework`, `:finalize`
- Bootstrap explicitly requests JSON code block for structured parsing

### Stage 3: Response parser
- `parse-tickets-from-output` — extracts JSON array from agent output text
- Strategy 1: ````json ... ```` code block regex
- Strategy 2: bracket-matching fallback (`[` depth tracking)
- Returns `[{:id :title :status :assignee :priority}]` or nil

### Stage 4: Extended `parse-agent-command`
- 9 new slash commands: `/bootstrap`, `/select`, `/arrange`, `/run-flow`, `/review`, `/rework`, `/finalize`, `/status`, `/reset`
- `/select 1 2 3` converts 1-based user input to 0-based indices

### Stage 5: Event handler extraction + flow commands
- `auto-scroll-agent!` — extracted auto-scroll logic
- `make-event-handler` — DRY: single fn handles all 8 event kinds, takes `run-id` + `on-done-fn`
- `fire-flow-run!` — composes prompt via `flow-prompt`, streams via SSE, passes session-id for `--resume`
- All `:flow-*` handlers in `submit-agent-run!` — validate state, transition, fire prompt or show info
- Refactored existing `:run` case to use `make-event-handler`

### Stage 6: Auto-bootstrap on load
- `setTimeout` at 1.5s, double-checked guard (idle + no session-id)
- Three-way semantics: fresh load → bootstrap, resume with state → skip, resume without → bootstrap

## Additional fixes during session
1. **Bootstrap JSON schema** — aligned to `{id, title, status, assignee, priority}` per Codex review
2. **`:idle` documentation** — documented as pre-graph state in `flow-transitions` docstring
3. **Hardcoded `flow-cwd`** — `/home/sid/projects/discourse-graph` (V0 scope per consensus doc)
4. **`--allowedTools` support** — read-only Linear MCP tools passed via CLI args (not settings file) for security
   - Modified `objects.cljc`: `provider-default-argv` accepts `:allowed-tools` kwarg
   - Modified `server_jetty.clj`: wires `:allowed-tools` from request body to argv builder
   - Client sends tool list in `fire-flow-run!` request body

## Codex review status
- Codex reviewed plan, found 6 items (2 High, 3 Medium, 1 Low)
- 2 High items were stale reads (Codex reviewed before Stages 5D/5E/6 landed)
- 2 Medium items fixed (JSON schema + `:idle` documentation)
- 1 Medium item fixed (resume/bootstrap semantics)
- 1 Low item addressed inline (5E refactor kept in same slice, minimal risk)
- **Awaiting Codex re-review at current file state**

## End-to-end validation
- Auto-bootstrap fired on load
- Claude CLI spawned with `--allowedTools` for Linear MCP
- `mcp__linear-server__list_issues` called successfully (both started + unstarted states)
- 21 real Linear tickets parsed and loaded into `!flow-state :tickets`
- `/select`, `/arrange`, `/status` all working — state machine transitions correctly
- Session-id captured for `--resume` continuity

## Files changed
- `src/app/client/webgpu/loop.cljs` (~430 lines added)
- `src/app/server/rama/objects.cljc` (3 lines — `--allowedTools` kwarg)
- `src/app/server_jetty.clj` (1 line — wire `allowed-tools` through)

## Next steps
1. Codex re-review of Step 4
2. Step 5: End-to-end validation with `/run-flow` on a safe ticket
3. Visual UI layer (ticket cards, canvas, drag-to-arrange) — Tier 2c
