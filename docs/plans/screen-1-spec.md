# Screen 1 — Task Ingestion: Master-Detail Implementation Spec

> Date: 2026-02-19
> Status: LOCKED DIRECTION, IMPLEMENTATION SPEC READY
> UI authority: `docs/plans/commission-consensus.md` Section 11
> Visual authority: `docs/plans/ui-mockups-master-detail.md`
> Process/gates: `docs/plans/commission-consensus.md` Section 10

## 0. Scope

Screen 1 is the intake surface.

- Entry: app load -> auto-bootstrap -> ticket list appears.
- Primary user task: scan grouped tickets and select a batch.
- Exit: `Enter` (keyboard) or `/run` (command). Arrangement is spatial — tickets stack in right pane, reorder with Shift+Arrow.
- Out of scope: run/review pane rendering (handled by `build-run-tree` in `dg_flow.cljs`).

## 1. Layout (Screen 1 Only)

```txt
+────────────────────────────────────────+──────────────────────────────────────────+
| DISCOURSE-GRAPH  N active             |                                          |
| ─────────────────────────────────────  |                                          |
| ● Ready to Merge  3                    |      No tickets selected yet.            |
|   ☐ ENG-1274  Add feature flags   P2   |                                          |
|   ☐ ENG-1272  Port all small gl…  P2   |      Select tickets from the list,       |
| ○ Ready for Review  9                  |      then Enter to run, Shift+Arrows to reorder.  |
|   ☐ ENG-1264  Update descript…    P3   |                                          |
| ◐ In Progress  1                       |                                          |
|   ☐ ENG-1207  Global left side…   P1   |                                          |
| ○ Todo  9                              |                                          |
|   ☐ ENG-1329  Port Discourse r…   P3   |                                          |
+────────────────────────────────────────+──────────────────────────────────────────+
| 0 selected                                                   discourse-graph|CLAUDE|
+──────────────────────────────────────────────────────────────────────────────────────+
```

### 1.1 Pane Split

- Left pane: 40% width, grouped ticket list, independently scrollable.
- Right pane: 60% width, contextual workspace for Screen 1.
- Divider: 1px vertical separator.
- Bottom status bar: always visible.

### 1.2 Left Pane (Grouped List)

- Group sections: status icon + label + count (collapsible).
- Group order: Ready to Merge, Ready for Review, In Progress, Todo, Backlog, Done, Released.
- Ticket row fields (single line): checkbox, issue ID, truncated title, priority badge, assignee.
- Row density: ~24px line height.

### 1.3 Right Pane (Screen 1 Content)

- No selection: instruction panel (empty-state guidance).
- One selected: ticket quick-detail preview (id/title/status/priority/assignee + short description if available).
- Multiple selected: execution stack (ordered lanes, active focus, next-step guidance to run).

### 1.4 Status Bar

- Left: `INTAKE | <count> tickets | <selected> selected`.
- Right: `discourse-graph | <provider>`.

## 2. Interaction Contract

### 2.1 Mouse

- Hover row: highlight row background.
- Click checkbox: toggle selection for that ticket.
- Click row (non-interactive cell): same toggle behavior as checkbox.
- Click group header: collapse/expand that group.
- Click empty area: no destructive clear.
- Wheel over left pane: scroll list only.
- Wheel over right pane: scroll right-pane content if overflow.

### 2.2 Keyboard

- `Ctrl-K`: toggle command panel (unchanged).
- `Escape`: close command panel and dismiss agent panel (unchanged).
- `Ctrl-A`: select all visible tickets in intake context; if none, no-op; not a toggle.
- `Enter`: when a batch exists, trigger `/run`.
- `Shift+Up/Down`: reorder the active lane in the execution stack.

### 2.3 Commands

- `/bootstrap`: refresh ticket intake.
- `/select ...`: select by indices (existing parser behavior).
- `/run`: start the current ordered batch.
- `/status`: show flow state.
- `/reset`: return to `:idle` editor mode.

## 3. Screen 1 States

### 3.1 Bootstrapping (`:bootstrapping`)

- Left pane shows loading skeleton/placeholder grouped layout.
- Right pane shows loading copy: `Loading tickets...`.
- Agent output visible with stream progress.

### 3.2 Intake Loaded (`:intake`, selected = 0)

- Grouped list rendered.
- Right pane shows instruction panel.
- Agent output auto-collapsed after successful bootstrap.

### 3.3 Intake Selected (`:intake`, selected > 0)

- Selected rows visibly tinted; checkboxes checked.
- Right pane switches to detail (single) or batch summary (multi).
- Status bar selected count updates immediately.

### 3.4 Empty

- Left pane: no-ticket message in list region.
- Right pane: `No tickets found. Use /bootstrap to retry.`
- Agent output remains available.

### 3.5 Error

- Left pane: load failed banner.
- Right pane: `Bootstrap failed. Check agent output. Use /bootstrap to retry.`
- Agent output remains visible.

## 4. Architecture Notes (Screen 1)

### 4.1 State Ownership

- `!flow-state`: source of truth for node/tickets/selection/session.
- `!agent-output`: stream status/text/trail; collapses after successful intake.
- `!cmd-panel`: command UI state, unchanged contract.
- `!editor-doc`: still exists, dormant for intake rendering.
- `!list-scroll-y`: left pane scroll offset (new UI atom).
- `!detail-scroll-y`: right pane scroll offset (new UI atom, only if needed).
- `!hovered-row-idx`: ephemeral hover tracking atom.

### 4.2 Reactive Safety

- Keep mode switching reactive (no one-time deref at flow construction).
- Preserve Missionary-safe patterns (`m/latest`/safe composition).
- Do not introduce `m/ap` multi-`m/?<` cancellation hazards.
- No `try/catch` inside `e/defn`.

### 4.3 Branching Boundary

Current Screen 1 master-detail rendering should stay isolated as list-render functions.
Before workflow complexity grows further, keep branch count explicit and bounded at top-level flow composition.

## 5. Definition of Done (Screen 1 Gate)

### Visual

- [ ] Left pane grouped list renders with correct section headers and counts.
- [ ] Row fields render: checkbox, ID, truncated title, priority, assignee.
- [ ] Hover + selected row styles are clearly distinguishable.
- [ ] Right pane renders correct state (instruction/detail/batch summary).
- [ ] Status bar reflects intake counts correctly.

### Interaction

- [ ] Row/checkbox click toggles selection reliably.
- [ ] Group collapse/expand works.
- [ ] Left/right scroll regions are independent and bounded.
- [ ] `Ctrl-A`, `Ctrl-K`, `Escape` behavior matches contract.

### State + Flow

- [ ] Bootstrapping -> intake transition renders list view.
- [ ] Empty and error states are clear and actionable.
- [ ] Agent output auto-collapses after successful bootstrap.
- [ ] `Enter` triggers `/run` when a batch exists and `Shift+Up/Down` reorders the execution stack.
- [ ] `/reset` restores editor mode cleanly.

### Architecture

- [ ] Render path is list/master-detail (no card-grid dependency for Screen 1).
- [ ] Reactive safety checks hold.
- [ ] No regressions in command panel and agent streaming behavior.

## 6. Implementation Order

1. Build grouped list rendering primitives (headers, rows, checkbox, divider).
2. Add row selection + hover behavior.
3. Add right-pane state renderer (instruction/single/multi).
4. Add independent pane scroll handling.
5. Add bootstrapping/empty/error list states.
6. Keep agent panel auto-collapse behavior after successful bootstrap.
7. Run Screen 1 gate (Section 5), freeze intake quality, then continue into run/review polish.

## 7. Deferred (Not Screen 1)

- Dedicated parallel lane layout beyond the ordered V0 batch.
- Rich review tabs (Summary / Trail / Diff / Tests) beyond the current V0 shell.
- Provenance badges, resume fork, and pacing polish.
- Advanced filter/sort query language.
- Transition animation polish.

## 8. Signoff

- Claude: APPROVED direction (master-detail lock via consensus Section 11)
- Codex: SPEC REWRITTEN TO MATCH LOCKED DIRECTION (2026-02-19)
