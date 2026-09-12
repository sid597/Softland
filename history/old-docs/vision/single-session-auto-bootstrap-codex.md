# Strategy: Single-Session Auto-Bootstrap Workflow (Codex)

> **Strategy Origin:** Codex (Feb 18, 2026) — based on this chat session
> **Solves Problem:** [Manual Orchestration & Context Thrashing](problem-manual-orchestration-gemini.md)
> **Related Strategies:** [Prompt-Driven UI (Gemini)](prompt-driven-ui-gemini.md), [Auto-Prompt Workflow (Claude)](auto-prompt-workflow-claude.md)

## What This Chat Clarified (Non-Negotiables)

1. There is **one Claude Code session** (`--resume` continuity), not a multi-agent system.
2. We are **not defining custom agents**; we are driving what Claude already has (MCP + CLI tools).
3. On project/app load, we should **auto-send a bootstrap prompt** instead of manually typing it.
4. "Screens" are **user-flow states** (what happens after user action X/Y/Z), not hard page boundaries.
5. The first target is the current real workflow, not a generic platform.

## Core Model

The UI is an event machine over one chat session:

`UI event -> prompt template -> Claude response -> parsed state -> next UI state`

This is the same shape as a web app calling backend APIs on load and on user actions, except prompts are the API surface.

## User Flow as State Transitions

### State 1: `:bootstrapping`
- **Trigger:** app/project load
- **System action:** auto-prompt Claude to fetch active Linear issues via MCP
- **Output:** structured issue list for UI

### State 2: `:intake`
- **Trigger:** issues returned
- **User action:** select which issues to include now
- **System action:** capture selection + intent fields

### State 3: `:arrange`
- **User action:** arrange selected tickets as sequential or parallel
- **Meaning:** this sets execution semantics for the same session

### State 4: `:run`
- **Trigger:** user clicks run/start on selected plan
- **System action:** send run prompts ticket-by-ticket according to arrangement
- **Constraint:** still one Claude session, no extra agent abstraction

### State 5: `:review`
- **Trigger:** run output/diff available
- **User action:** comment, approve, request rework
- **System action:** convert review decisions into actionable follow-up prompt payload

### State 6: `:rework`
- **Trigger:** review has actionable comments
- **System action:** auto-prompt Claude with condensed, ticket-scoped rework instructions
- **Return:** back to `:review`

### State 7: `:finalize`
- **Trigger:** all selected tickets approved or deferred
- **System action:** generate final summary artifact and reset to next intake batch

## Sequential vs Parallel (Important Clarification)

- **Sequential:** T1 output informs T2, then T3, in one narrative chain.
- **Parallel:** T1/T2/T3 are independent work tracks represented in UI as separate lanes.
- This is a **workflow/planning mode**, not proof we need separate "agents."

## V0 Contract Requirements

1. Bootstrap prompt is automatic on load.
2. Responses must include a parseable structured block (JSON preferred) for deterministic UI state updates.
3. Every user action maps to a named prompt template.
4. Session continuity is explicit and visible in state.
5. Review feedback loops back into prompts without manual copy/paste.

## V0 Out of Scope

1. New integration platforms beyond existing Claude + MCP setup.
2. Rich media automation (video/screenshot pipelines) as a blocker for first release.
3. Generalized multi-project orchestration framework.

## Acceptance Criteria

1. On load, user sees issues without typing an initial prompt.
2. User can select and arrange issues, then run the plan in the same session.
3. User can review results, request rework, and iterate without context-copy overhead.
4. State transitions are explicit and reliable (`bootstrapping -> intake -> arrange -> run -> review -> rework/finalize`).
5. Manual orchestration steps are reduced to exceptions, not the default path.

## Notes on Alignment

- Gemini's strategy supplies architecture framing ("prompt-driven UI").
- Claude's strategy supplies concrete discourse-graph flow and screen narrative.
- This Codex strategy locks the operational constraint from this chat: **single-session, event-driven user flow with auto-bootstrap**.
