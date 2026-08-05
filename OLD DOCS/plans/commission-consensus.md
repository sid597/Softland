# Commission Consensus (Codex + Claude)

> Date: 2026-02-18
> Status: APPROVED
> Rule: full product context + implementation decisions (no speculative expansion beyond stated scope)

## 0. Full Product Context (From User's 6-Page Handwritten Sketches + Chat Clarifications)

This project automates a real manual workflow the user runs daily inside one Claude Code session on their `discourse-graph` project. Not generic orchestration software — a practical in-editor control plane for ticket execution and review.

### The Manual Process Today (What We're Replacing)
1. Open project, manually type "get my Linear tickets" into Claude Code
2. Read ticket descriptions in CLI text output, mentally plan which to work on
3. Manually create git worktrees per ticket, copy necessary files
4. Start background terminal runs (turbo dev) for each worktree
5. Alt-tab between terminals to give commands, check output, review diffs
6. Manually assemble PR descriptions, comment, submit

### The Core Paradigm: "Prompts as API Calls"
Just like a website sends `fetch()` on page load and renders JSON into components, the editor sends **prompts** to Claude Code on load and renders **structured responses** into WebGPU UI. The Claude CLI is the backend. MCP tools (Linear, Playwright, GitHub) are the integration layer. No custom API clients needed.

### The Principle: "UI Under UI" (from Page 1 of sketches)
For each MCP interaction, there should be **corresponding UI**. When the system fetches Linear tickets, it doesn't show text — it renders ticket cards. When it creates a worktree, it shows the workspace state. Every data operation has a visual reflection. It's a stack of UI produced by a stack of prompts.

### Screen-by-Screen Detail (from sketches; not the final V0 state names)

**Screen 1 — Task Ingestion (Page 6, bottom sketch)**
- Editor loads the `discourse-graph` project
- Auto-prompt fires (no typing): "Get my active Linear tickets"
- Tickets appear as cards on a blank canvas
- A chat panel sits alongside for ad-hoc commands
- This is the starting state — a blank canvas that populates itself

**Screen 2 — Task Arrangement (Page 5, Page 6 top)**
- Task cards (T1, T2, T3, T4) appear in a sidebar
- User drags a task into the main panel to work on it
- Arrangement determines execution mode:
  - **Stack vertically = sequential**: T1 output feeds T2's context, T2 feeds T3. One narrative chain.
  - **Place side-by-side = parallel**: T1/T2/T3 run independently, disjointed but sharing common mainpath (main branch). Each gets its own worktree.
- Per-task detail is visible: description, parameters, metadata
- Tasks can have pre-filled descriptions/context from Linear. If the intent is clear to Claude, it can auto-populate fields. If not, it asks specific questions — "I will fill that out, echo anything filled out gets sent to Linear."
- After selection, tasks are "ready to be worked on instead of me prompting again"

**Screen 3 — Per-Task Management (Page 4)**
- Composite panel per task: T1 Description + Checkbox/OK button, T2 Description + Checkbox/OK button
- A shared chat panel at the bottom
- The checkbox/OK means: "build or simulate → auto-output in Claude Code client"
- **"Computable docs"** concept: the task description + checkboxes + diffs are not just display — they are interactive and actionable
- Expanded per-task view shows (Linear-style layout):
  - Diff file 1, Diff file 2 (with commit info)
  - Output panels per file
  - Comment thread (inline and overall)
- "This order and the splice of Diff UI and we achieve Comment style... synced to git, doesn't matter if it goes up to 20 commits"
- **Two chat levels**: the bottom chat box is for the worktree/task scope. A separate area adds diff-level/file-level comments. Both inline and overall.
- Available actions per task: **Comment, Click-to-open-file, Run, Finalize**
- "Rinse and repeat — at last we will have a diff we sync through the tick dropdown"

**Screen 4 — Per-Ticket Testing (Page 3)**
- Per-ticket view with: files list, test status ($100-100 format), "Open" button, "Testing" status
- Can run **Playwright or similar** to execute tests
- If tests fail, overlay shows what went wrong — user can count errors, annotate
- Then user can **submit the report** or go back to fix
- **Overlay input**: pre-filled with navigation context. "This overlay in function is pre-filled — basically means we are prompted with 'how do I test this?' and a viewreport through the navigation"
- Current interim: a button that takes to a page. Better: **inline overlay** where results render directly in the same view
- **Screenshot capture**: automatic screenshots while going through preview/test runs
- **Video recording**: "Record this as video if possible. Can we integrate Loom?? Yes, its records are going through preview"
- **Reasoning window**: inline, showing Claude's thinking about the test results

**Screen 5 — Deep Review (Page 2)**
- Full composite: T1 panels showing Diff, Text, Runs
- Progression through states → leads to final/initial review state
- Expanded view: T1 with Diff + Comments columns (multi-column layout)
- **Default prompt injection**: "It will be inflicted/injected as well to all comments, and then if any comments we open directly"
- Workflow: pull the PR, review, work on worktree/branch, comment
- **Admin controls**: let the user comment directly
- This is where **Reasoning Trails** (the tool_use stream visualization from `where-we-are.md`) lives — it's the content engine for the review view

### Execution Modes (Page 6, detailed)
- **Sequential**: Ticket 1 → Ticket 2 → Ticket 3. "Once T1 completes, take output to T2 to T3." One Claude session chains context.
- **Parallel**: T1 | T2 | T3 side by side. "Disjointed from each other, have common mainpath." Independent worktrees.
- User controls mode by how they arrange task cards. "Main select for work: sequential means in sequence, parallel means in bundle"
- Both modes operate within the SAME Claude Code `--resume` session

### Key Clarifications (from chat)
- This is scoped to `projects/discourse-graph` only — generalize later
- Entry point: on load, read `README.md` from the target project, which drives the auto-prompts
- There are NO custom agents defined. Just Claude Code with its existing MCP tools.
- "Sequential vs parallel" is a workflow planning mode, not a multi-agent architecture claim

This context is the destination. Sections below lock the implementation decisions for the first deliverable (V0).

### Session 35 Translation (V0 Plugin Workflow)

The sketches above remain the source material. The current V0 translation is:
- This is the plugin workflow for the `discourse-graph` codebase, not a generic "DG workflow."
- Arrangement is no longer a separate node or command. It lives inside intake as an ordering subphase rendered in the right pane.
- V0 currently ships one ordered batch, not true parallel orchestration. Parallel lanes remain future model territory.
- Testing is folded into review as an artifact tab/workspace concern, not a separate top-level journey in V0.
- The invariant is: left pane = map, right pane = current artifact/workspace.

## 1. Locked Decisions

1. Runtime model: one Claude Code session (`--resume` continuity), no custom multi-agent architecture.
2. Interaction model: UI actions trigger prompt templates; prompts are treated as API calls.
3. Integration model: MCP tools (Linear, shell, etc.) are the backend capability surface.
4. Build order: fixtures-first development before live-stream wiring.
5. Data contract: parser normalizes to a fixed event envelope and enforces invariants at the boundary.
6. Flow model: named user-flow states are kept, but transitions are graph-based (not a strict pipeline).

## 2. Canonical Stream Event Contract (8 Kinds)

Reference locked from Round 5:

```clojure
| :run-start       | {:provider :prompt :argv :session-id} |
| :text-delta      | {:text "..."} |
| :tool-use-start  | {:tool-id :tool-name} |
| :tool-input-delta| {:tool-id :json-chunk} |
| :tool-result     | {:tool-id :content} |
| :block-stop      | {:block-idx} |
| :run-done        | {:status :exit-code ...} |
| :run-error       | {:error} |
```

## 3. Parser Boundary Invariants (Must Hold)

1. Every emitted event has `:kind` and `:ts`.
2. `:tool-input-delta` and `:tool-result` must reference an existing `:tool-id`.
3. Stream terminates in exactly one terminal event: `:run-done` xor `:run-error`.
4. Invalid/malformed input is surfaced as parse failure signal, not silently dropped.

## 4. State Graph (Named Nodes + Explicit Back-Edges)

> **Updated 2026-03-09:** `:arrange` removed as a node. Arrangement is an intake subphase (spatial, keyboard-native). `/arrange` command removed; `/run-flow` renamed to `/run`.

Nodes:
- `:bootstrapping`
- `:intake` (owns both selection and ordering as subphases)
- `:run`
- `:review`
- `:rework`
- `:finalize`

Primary edges:
- `:bootstrapping -> :intake`
- `:intake -> :run` (directly, when batch has lanes)
- `:run -> :review`
- `:review -> :rework`
- `:rework -> :review`
- `:review -> :finalize`
- `:finalize -> :intake`

Explicit back-edges:
- `:bootstrapping -> :bootstrapping` (retry on fetch/bootstrap failure)
- `:review -> :intake` (re-scope after review feedback)
- `:run -> :intake` (scope changed or reselection needed)
- `:rework -> :intake` (re-scope after failed rework)

Global override:
- Human override allowed from any node to `:intake`.

Object model (locked 2026-03-09):
- **batch** — top-level work session for selected tickets
- **lane** — one ticket (V0) or one sequential chain (future)
- **run** — one agent execution attempt inside a lane
- **artifact** — diff, trail, tests, comments, summary
- **decision** — approve, rework, split, defer, finalize

## 5. V0 Scope Checklist

IN (must ship):
- [ ] Auto-bootstrap path exists and is wired to load flow.
- [ ] Tickets are fetched and rendered as structured UI state (blocked by [OPEN] 1).
- [ ] User can select tickets.
- [ ] User can compose an ordered batch inside intake (execution stack in right pane) and trigger run.
- [ ] User can trigger run and see run/review outputs.
- [ ] User can send rework from actionable review comments.
- [ ] Same session continuity is preserved across the flow.

DEFERRED (explicitly out of V0):
- [ ] Playwright-driven testing workflow UI.
- [ ] Screenshot/video/Loom capture and overlay pipelines.
- [ ] Full per-ticket testing/evidence annotation loop from the sketches.
- [ ] Advanced "actionable review inbox" UX polish from Screen 5 concept.
- [ ] Full background process/worktree automation UX beyond minimal run loop.
- [ ] Multi-project generalization/platformization.
- [ ] Advanced discourse-graph ontology extensions.

## 6. Execution Order (Immediate)

0. Capture a real CLI stream output from a Linear MCP call and validate fixture shape against it.
1. Lock fixtures and replay path for deterministic UI development.
2. Make parser pass fixture cases and enforce invariants.
3. Project event stream into client state for flow nodes.
4. Wire user actions to prompt templates for V0 loop.
5. Validate end-to-end against one real workload batch.

## 7. [OPEN] Items

1. [OPEN] Response contract details: exact JSON shape required for structured rendering and fallback behavior on malformed/non-JSON output.
2. [OPEN] Bootstrap semantics: behavior on fresh load vs existing resumed session (`--resume`) and re-fetch policy.
3. [OPEN] Transcript gap: recover and archive missing Rounds 1-4 commission content.

## 8. Signoff

- Codex: APPROVED (drafted, applied Claude's 3 additions)
- Claude: APPROVED (2026-02-18)

## 9. Implementation Slice Review Checklist (Acceptance Gate)

This gate applies to every implementation slice before acceptance.

### 9.1 Required Submission From Implementer
1. Slice scope: which execution step(s) and which files were changed.
2. Contract mapping: exact checklist items from this doc that the slice claims to satisfy.
3. Diff summary: behavior change in plain language.
4. Validation evidence: commands/tests run and observed outcomes.
5. Known gaps: anything intentionally left for a later slice.

### 9.2 Mandatory Gate Checks (Pass/Fail)
1. Contract compliance: no deviation from Sections 1-6 unless explicitly approved.
2. Event contract compliance: emitted/consumed events align to the 8 canonical kinds only.
3. Invariant enforcement: Section 3 invariants are provably enforced (with tests or assertions).
4. State graph compliance: transitions match Section 4, including back-edges and human override.
5. Scope discipline: no implementation of items listed as DEFERRED in Section 5.
6. Failure handling: malformed/invalid stream input is surfaced and not silently dropped.
7. Session continuity: no regressions to single-session (`--resume`) behavior.

### 9.3 Reactive Safety Checks (Required for Step 3-4 Work)
1. No `m/ap` with multiple `m/?<` in flows fed to `m/latest`.
2. Event filtering uses `m/eduction` with deref where appropriate, not `m/ap` + `m/watch` forks.
3. No `try/catch` introduced inside `e/defn`.
4. Any flow-combination points preserve cancellation-safe Missionary patterns.

### 9.4 Review Ownership
1. Steps 0-2 implementation owner: Codex; primary reviewer: Claude.
2. Steps 3-4 implementation owner: Claude; primary reviewer: Codex.
3. Step 5 validation: joint (Codex + Claude).

### 9.5 Acceptance Rule
1. A slice is accepted only when all applicable checks above are `PASS`.
2. Any `FAIL` blocks merge until resolved or explicitly waived in writing in this document.

## 10. Screen 1 UI/UX Mini-Commission (Claude + Codex, 3 Rounds)

> Date: 2026-02-19
> Updated 2026-03-09: this section now acts as an intake quality bar. Session 35 already shipped the V0 dogfooding loop beyond intake, so the gate below is no longer a hard blocker to run/review implementation.
> Scope: Screen 1 intake quality only (`:bootstrapping -> :intake`)
> Goal: lock concrete intake decisions and preserve the boundary between intake and the later artifact surfaces
> Implementation spec: `docs/plans/screen-1-spec.md` (exact layout, colors, card anatomy, architecture)
> This section: process contract (rounds, execution order, acceptance gates, handoff rules)

### Round 1 (Claude Position: Gap Analysis + Two-Pass Proposal)
Claude identified the following Screen 1 gaps from current implementation:
1. No hover feedback on cards.
2. Selection feedback may be too weak visually.
3. Card title truncation/polish issues.
4. Header information is present but weak as a "tickets loaded" affordance.
5. No clear "next action" affordance after selection.
6. No status grouping/filtering.
7. Agent output area competes too much with ticket canvas.
8. Empty/loading states need clear rendering.

Claude proposal:
1. Pass A: interaction polish now (hover, output collapse behavior, next-step hint, loading/empty).
2. Pass B: UI architecture review before deeper expansion (grouping/filtering/panel strategy/Screen 2 transition).

### Round 2 (Codex Position: Scope Discipline + Architecture-First Gate)
Codex agreed with the gap list but tightened execution:
1. Screen 1 must be finalized as a standalone product slice before Screen 2 implementation.
2. Architecture review should happen before adding new interaction surface that changes ownership boundaries.
3. Grouping/filtering are not mandatory for Screen 1 acceptance; treat as optional enhancement unless explicitly promoted.
4. Mandatory Screen 1 "done" behaviors:
   - Visible hover and selected states.
   - Clear next-action affordance once `:selected` is non-empty.
   - Loading and empty/error ingestion states.
   - Panel-space balance so cards remain the dominant surface in intake mode.
   - No regression to command panel shortcuts or normal editor mode transitions.

### Round 3 (Locked Consensus: Screen 1 Completion Contract)
The following is now locked for Screen 1 completion:

1. **Execution order**
   - A. Run Screen 1 UI architecture review first.
   - B. Implement Screen 1 polish against that review.
   - C. Run Screen 1 acceptance pass and freeze.
   - D. Only then deepen workflow polish beyond intake.

2. **Screen 1 architecture review checklist (must be resolved before polish merge)**
   - A. State ownership boundaries: `!flow-state` vs `!agent-output` vs editor atoms.
   - B. Mode boundaries: shared resources between code mode and flow-canvas mode (`!scroll-y`, focus, panel visibility).
   - C. Render branching points: keep mode-gated branches explicit and limited.
   - D. Input routing: card hit-testing and command/editor focus rules must not conflict.
   - E. Layout governance: card canvas remains primary in intake mode.

3. **Screen 1 acceptance checklist (intake quality bar)**
   - A. Auto-bootstrap transitions to visual loading then intake cards.
   - B. Cards support clear hover + clear selected feedback.
   - C. Selection count and next-action guidance are visible.
   - D. Card scrolling is bounded to visible canvas space (not hidden under panels).
   - E. Empty/parse-failure states are understandable and actionable.
   - F. `Ctrl-K`, command panel submit, and `/reset` transitions still work.
   - G. Returning from flow mode to code mode restores expected editing behavior.

4. **Deferred from Screen 1 (explicit)**
   - A. Status-grouped lanes and filter/sort bars.
   - B. Motion/transition animation between intake and later artifact states.
   - C. Screen 3/4/5 UI affordances.

5. **Handoff rule**
   - Treat this as an intake quality bar; do not use it to deny the already-shipped V0 run/review shell.

## 11. Locked UI Approach: Master-Detail Split (updated 2026-03-09)

> Status: APPROVED (Claude + Codex + User)
> Source: Claude Approach 1, refined through 3-way discussion
> Implementation spec per screen: `docs/plans/screen-1-spec.md` (Screen 1), future files for Screen 2+

### Core Pattern
- **Intake**: 2-pane split — grouped ticket list (left) + empty state / single detail / execution stack (right)
- **Run**: 2-pane split — batch map with lane statuses (left) + live execution trail (right)
- **Review**: 2-pane split — batch map with outcomes (left) + artifact workspace (Summary / Trail / Diff / Tests) (right)

### Design Principles
1. **Left pane remains the map** as flow progresses: all tickets → selected stack → lane statuses → review outcomes
2. **Right pane grows in richness**: empty/detail → execution stack → trail stream → review workspace ("UI under UI")
3. **2 panes, not 3** — a third pane is too cramped. Details show inline in the right panel on click.
4. **Status grouping** in Screen 1 list (Ready to Merge, Ready for Review, In Progress, Todo, etc.)
5. **Status bar** always visible at bottom: context info left, project/provider right
6. **Cmd panel** overlays at bottom on Ctrl-K (unchanged from current implementation)

### Left Pane Content Per Screen
| Screen | Left pane shows | Width |
|--------|----------------|-------|
| Intake | All tickets, grouped by status, checkboxes | ~40% |
| Intake ordering | Same list (selected highlighted) while right pane shows execution stack | ~40% |
| Run | Batch progress: ticket names + progress bars + status (DONE/RUN/WAIT) | ~40% |
| Review | Results list: ticket names + review status (needs review/approved/reworked) | ~40% |

### Right Pane Content Per Screen
| Screen | Right pane shows |
|--------|-----------------|
| Intake | Empty state, single-ticket detail, or execution stack for multi-select |
| Intake ordering | Ordered batch stack, keyboard-native reorder (`Shift+Up/Down`), run trigger (`Enter` or `/run`) |
| Run | Active ticket's execution feed: tool calls, inline diffs, agent reasoning |
| Review | Selected ticket's diff viewer with tabs (Diff, Trail, Tests, Comments) + approve/rework actions |

### Screen Transitions
| From → To | Trigger | Left pane change | Right pane change |
|-----------|---------|------------------|-------------------|
| Intake → Intake ordering | Multi-select | Selected tickets highlighted | Execution stack appears |
| Intake ordering → Run | `Enter` or `/run` | Switches to progress view | Execution feed starts |
| Run → Review | All tickets complete | Switches to results view | First ticket's diff loads |
| Review → Rework | Click "Rework" on a ticket | Ticket marked for rework | Rework prompt shown |
| Review → Finalize | `/finalize` | Summary view | Final report |
| Any → Intake | Human override to intake or `/reset` | Full ticket list restored | Clears to empty state |

### What This Replaces
- The card grid from Tier 2c is replaced by a grouped list view
- Card rendering functions (`compute-ticket-card-rects`, `compute-ticket-card-text-ops`, `ticket-card-layout`) will be replaced by list rendering functions
- The mode-switch architecture (`flow-canvas-active?` branching) remains valid — the branch now produces list+pane data instead of card data

### Signoff
- Claude: APPROVED (2026-02-19) — designed and refined through 3 approaches
- Codex: APPROVED (2026-02-19) — contributed UI A/B/C alternatives, converged on master-detail
- User: APPROVED (2026-02-19) — selected Approach 1 as final direction
