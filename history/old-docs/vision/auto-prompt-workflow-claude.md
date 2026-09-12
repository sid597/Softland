# Strategy: Plugin Workflow for the `discourse-graph` Codebase

> **Strategy Origin:** Suggested by Claude (Feb 18, 2026) — from handwritten notes session
> **Solves Problem:** [Manual Orchestration & Context Thrashing](problem-manual-orchestration-gemini.md)
> **Related Strategy:** [Prompt-Driven UI (Gemini)](prompt-driven-ui-gemini.md)
> **Consensus:** [Commission Consensus (Codex + Claude)](../plans/commission-consensus.md) — source of truth for implementation decisions

## The Paradigm

A website loads → sends `fetch()` to its backend → renders JSON into components.

Our editor loads → sends **prompts** to Claude Code → renders **structured responses** into WebGPU UI.

Prompts are API calls. MCP tools are the integration layer. The Claude CLI is the backend.

## Scope: Plugin Workflow Only (Generalize Later)

This is NOT a general-purpose system. It automates ONE specific workflow: the developer's daily process on the `projects/discourse-graph` codebase. On project load, read `README.md` from `projects/discourse-graph` — that file drives the automatic steps. Platform generalization comes later.

## The Use Case (What Happens Today, Manually)

1. Open project, switch to Linear, find active tickets
2. Create git worktrees for each ticket
3. Start background terminal runs (turbo dev) per worktree
4. Switch between terminals to give commands/chat
5. View diffs, comment, apply quick actions, test each ticket
6. Assemble PRs and submit

Every step above is something Claude Code + MCP already does — the developer just types each prompt manually.

## The Workflow (States of One Local World)

The older "5 screens" came from the sketches. The current V0 translation is one local world whose state changes over time. There are no page navigations. The WebGPU canvas re-renders based on structured responses and the current workflow node.

### Intake
- **Trigger:** User bootstraps the workflow (`/bootstrap` or `/mock`)
- **UI state:** grouped ticket list on the left, empty state / detail / execution stack on the right
- **Key move:** select 2+ tickets and they become an ordered execution stack in the right pane

### Run
- **Trigger:** User presses `Enter` or runs `/run`
- **UI state:** left pane becomes a batch map with lane statuses; right pane becomes a live trail stream
- **Key artifact:** the trail is the first usable review artifact, not just an implementation log

### Review
- **Trigger:** run completes
- **UI state:** same master-detail shell, but the right pane becomes the artifact workspace
- **V0 shape:** live trail first; richer `Summary | Trail | Diff | Tests` tabs are the next pass

### Rework
- **Trigger:** user sends feedback via `/rework <comment>`
- **UI state:** the same local world stays open; feedback becomes the input to another run
- **Constraint:** V0 still uses a single live run buffer; preserved run history is a V1 upgrade

### Finalize
- **Trigger:** user runs `/finalize`
- **UI state:** finalize summary, then return to intake with ticket list preserved
- **Outcome:** produce a handoffable artifact instead of a bare diff

## Execution Modes

- **V0:** ordered batch only. Selected tickets become a stack; order matters; one run walks that stack.
- **Future:** parallel lanes remain in the conceptual model, but they are not part of the current dogfooding shell.
- **Session continuity:** one Claude session, `--resume` chaining across bootstrap, run, review, rework, and finalize.

## What's Different from Gemini's Strategy

Both strategies converge on "prompts as API calls" and "the UI is a remote control for the CLI." The difference:

| | Gemini (Prompt-Driven UI) | Claude (Auto-Prompt Workflow) |
|--|--------------------------|-------------------------------|
| **Scope** | General architecture for any project | Scoped to the discourse-graph plugin workflow, generalize later |
| **Entry point** | Prompt registry (`prompts.cljc`) | `README.md` from the target project |
| **Focus** | The abstraction layer (how prompts map to UI) | The concrete workflow and its local-world shape |
| **Workflow shape** | Implied via examples table | Intake → Run → Review ↔ Rework → Finalize |
| **Testing** | Not addressed | Screen 4 with Playwright MCP + screenshot overlay |

They are complementary: Gemini defines the architecture, Claude defines the first concrete instance of that architecture.

## Session Continuity

All workflow phases operate within the SAME Claude Code `--resume` session. Claude remembers ticket context from intake through run/review/rework. No fresh sessions per phase.

## Relationship to Reasoning Trails

The reasoning trail spike builds the **artifact slot** for run/review. The plugin workflow supplies the container that generates, revises, and hands off those artifacts.
