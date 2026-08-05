# Vision: The Prompt-Driven UI (PDUI)

> **Strategy Origin:** Suggested by Gemini (Feb 18, 2026)
> **Solves Problem:** [Manual Orchestration & Context Thrashing](problem-manual-orchestration-gemini.md)

> "The UI is just a macro system for the CLI."

## The Core Concept
We are not building a complex multi-agent orchestration system from scratch. We are building a **Remote Control** for the existing `claude` CLI.

Every meaningful action in the UI corresponds 1:1 to a natural language prompt sent to the active CLI session.

## The Architecture: UI-to-Prompt

Instead of:
`User clicks button` → `Code calls rigid API` → `Database updates`

We do:
`User clicks button` → `Code constructs prompt` → `Agent runs prompt` → `Agent updates state`

### Examples from the Sketch

| UI Action | Generated Prompt | Desired Effect |
|-----------|------------------|----------------|
| **App Load / Refresh** | "Check Linear for my active tickets. Return them as a JSON list." | Populates the "Backlog" lane. |
| **Drag Ticket to 'Parallel'** | "Create a new git worktree for ticket PROJ-123. Run the build. If it fails, try to fix it." | Spawns background worker. |
| **Click 'Review' on Ticket** | "Show me the `git diff` for PROJ-123 and summarize the changes vs main." | Populates the Review Pack UI. |
| **Click 'Fix Tests'** | "Run the tests for this module. If any fail, analyze the error and propose a fix." | Active agent loop. |

## Why this is powerful
1.  **Zero API Integration Cost:** We don't need to build a "Linear API Client" or a "Git Worktree Manager" in Clojure. We just tell Claude to do it. Claude already has the tools (MCP, Shell).
2.  **Infinite Extensibility:** Adding a new feature doesn't mean writing new backend logic. It means writing a new *prompt template*.
3.  **Human-in-the-loop by default:** Because the "backend" is just a chat session, the user can always intervene, clarify, or cancel.

## Phase 2 Implementation Plan

### 1. The Prompt Registry (`prompts.cljc`)
A central registry of prompt templates parameterized by UI data.

```clojure
(def prompts
  {:fetch-tickets "Use the Linear MCP tool to find my active tickets..."
   :spawn-worktree "Create a git worktree for branch {branch}..."
   :review-diff   "Diff branch {branch} against main..."})
```

### 2. The Auto-Prompter
Extend `loop.cljs` to trigger agent runs programmatically, not just from the command panel.

```clojure
;; CURRENT: Only triggered by user typing in Command Panel
(defn submit-command! [text] ...)

;; NEW: Triggered by UI events
(defn trigger-auto-prompt! [prompt-key params]
  (let [prompt (format-prompt (get prompts prompt-key) params)]
    (submit-agent-run! prompt)))
```

### 3. The State parser
The agent needs to return data the UI can render. We continue the pattern from Phase 1:
- Agent outputs JSON (implicitly or requested via prompt).
- UI parses JSON stream.
- UI updates Reagent/Electric atoms (`!tickets`, `!worktrees`).

## The User Workflow (Target State)
1.  **Open App:** "Loading tickets..." (Agent runs in background).
2.  **See Dashboard:** List of tickets appears.
3.  **Action:** Drag ticket to "Doing".
4.  **System:** "Setting up workspace..." (Agent creates worktree).
5.  **Result:** Worktree ready. "I'm ready to code. What's first?"

## Relationship to Phase 1 (Reasoning Trails)
Phase 1 builds the *Unit of Work* (the trail).
Phase 2 builds the *Manager of Work* (the dashboard that orchestrates multiple trails).

The "Prompt-Driven UI" is the glue that binds them.
