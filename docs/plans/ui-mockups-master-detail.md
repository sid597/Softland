# UI Mockups: Master-Detail Split (Locked 2026-02-19)

> These are the approved ASCII mockups for all screens.
> Locked in `commission-consensus.md` Section 11.
> V0 now ships intake ordering plus the run/review shell; these mockups remain the target shape for that local world.

## Screen 1 — Intake (select from list)

```
+────────────────────────────────────────+──────────────────────────────────────────+
│ DISCOURSE-GRAPH  21 active             │                                          │
│ ─────────────────────────────────────  │                                          │
│                                        │                                          │
│ ● Ready to Merge  3                    │      No tickets selected yet.            │
│   ☐ ENG-1274  Add feature flags   P2   │                                          │
│   ☐ ENG-1272  Port all small gl…  P2   │      Select tickets from the list,       │
│   ☐ ENG-1273  Port all small pe…  P2   │      then Enter to run, Shift+Up/Down to reorder. │
│                                        │                                          │
│ ○ Ready for Review  9                  │                                          │
│   ☐ ENG-1264  Update descript…    P3   │                                          │
│   ☐ ENG-1281  Port Discourse N…   P3   │                                          │
│   ☐ ENG-1440  Port page groups…   P3   │                                          │
│   ☐ ENG-1438  Port Keyboard sh…   P3   │                                          │
│   ☐ ENG-1225  Port small Disco…   P3   │                                          │
│   ☐ ENG-1280  Port Discourse n…   P3   │                                          │
│   ☐ ENG-1217  Section componen…   P3   │                                          │
│   ☐ ENG-1291  Port discourse n…   P3   │                                          │
│   ☐ ENG-1290  Port Discourse n…   P3   │                                          │
│                                        │                                          │
│ ◐ In Progress  1                       │                                          │
│   ☐ ENG-1207  Global left side…   P1   │                                          │
│                                        │                                          │
│ ○ Todo  9                              │                                          │
│   ☐ ENG-1329  Port Discourse r…   P3   │                                          │
│   ☐ ENG-273   document roam gl…   P4   │                                          │
│   ☐ ENG-735   Remove UI from s…   P4   │                                          │
│   ☐ ENG-1171  Remove migrateLe…   P4   │                                          │
│   ⋮  5 more                            │                                          │
│                                        │                                          │
+────────────────────────────────────────+──────────────────────────────────────────+
│ 0 selected                                                   discourse-graph│CLAUDE│
+──────────────────────────────────────────────────────────────────────────────────────+
```

### Screen 1 Rendering Requirements
- **Left pane (40% width)**: Grouped ticket list, scrollable independently
  - Group headers: status icon + name + count (collapsible)
  - Ticket rows: checkbox + ID + title (truncated) + priority badge + assignee
  - Row height: ~24px (one line per ticket, dense)
  - Hover: row bg highlight
  - Selected: checkbox filled + row tinted
- **Right pane (60% width)**: Empty state message, centered
- **Divider**: 1px vertical line between panes
- **Status bar**: "N selected" left, "discourse-graph | CLAUDE" right

## Screen 2 — Execution Stack (intake subphase, right panel)
> **Updated 2026-03-09:** `:arrange` removed as separate screen. Arrangement is now a spatial intake subphase — selected tickets appear as an ordered stack in the right pane.

```
+────────────────────────────────────────+──────────────────────────────────────────+
│ DISCOURSE-GRAPH  21 active             │  BATCH — 3 tickets, sequential           │
│ ─────────────────────────────────────  │  ─────────────────────────────────────── │
│                                        │                                          │
│ ● Ready to Merge  3                    │  1 ┃ ENG-1264  Update descriptions       │
│   ☐ ENG-1274  Add feature flags   P2   │    ┃ Ready for Review · P3               │
│   ☐ ENG-1272  Port all small gl…  P2   │    ┃ Output of 1 feeds into 2            │
│   ☐ ENG-1273  Port all small pe…  P2   │    ▼                                     │
│                                        │  2 ┃ ENG-1440  Port page groups           │
│ ○ Ready for Review  9                  │    ┃ Ready for Review · P3               │
│   ☑ ENG-1264  Update descript…    P3   │    ┃ Output of 2 feeds into 3            │
│   ☐ ENG-1281  Port Discourse N…   P3   │    ▼                                     │
│   ☑ ENG-1440  Port page groups…   P3   │  3 ┃ ENG-1280  Port Discourse node       │
│   ☐ ENG-1438  Port Keyboard sh…   P3   │    ┃ Ready for Review · P3               │
│   ☐ ENG-1225  Port small Disco…   P3   │                                          │
│   ☑ ENG-1280  Port Discourse n…   P3   │  ─────────────────────────────────────── │
│   ☐ ENG-1217  Section componen…   P3   │  ▶ Enter to run | Shift+Up/Down reorder     │
│   ☐ ENG-1291  Port discourse n…   P3   │                                          │
│   ☐ ENG-1290  Port Discourse n…   P3   │                                          │
│                                        │                                          │
│ ◐ In Progress  1                       │                                          │
│   ☐ ENG-1207  Global left side…   P1   │                                          │
│                                        │                                          │
+────────────────────────────────────────+──────────────────────────────────────────+
│ 3 selected │ sequential                                      discourse-graph│CLAUDE│
+──────────────────────────────────────────────────────────────────────────────────────+
```

## Screen 3 — Run (execution progress)

```
+────────────────────────────────────────+──────────────────────────────────────────+
│ BATCH — sequential                     │  ENG-1264  Update descriptions of tasks  │
│ ─────────────────────────────────────  │  ─────────────────────────────────────── │
│                                        │                                          │
│  1 ● ENG-1264  ██████████ DONE    1:42 │  ▸ Read src/tasks/descriptions.ts        │
│  2 ◐ ENG-1440  ████░░░░░░ RUN     0:38 │  ▸ Read src/tasks/index.ts              │
│  3 ○ ENG-1280  ░░░░░░░░░░ WAIT         │  ▸ Edit src/tasks/descriptions.ts       │
│                                        │    @@ -14,7 +14,12 @@                   │
│                                        │    - const desc = getDesc(task);         │
│                                        │    + const desc = getLocalizedDesc(      │
│                                        │    +   task,                             │
│                                        │    +   settings.locale                   │
│                                        │    + );                                  │
│                                        │  ▸ Bash  npm run typecheck              │
│                                        │    ✓ No errors                           │
│                                        │  ▸ Edit src/tasks/index.ts              │
│                                        │    @@ -8,3 +8,5 @@                      │
│                                        │    + export { getLocalizedDesc }          │
│                                        │                                          │
│                                        │  ─ Agent ─────────────────────────────── │
│                                        │  I've updated the task descriptions to   │
│                                        │  use localized strings. The typecheck    │
│                                        │  passes. Moving to the next ticket...    │
│                                        │                                          │
+────────────────────────────────────────+──────────────────────────────────────────+
│ 1/3 done │ ENG-1440 running 0:38                             discourse-graph│CLAUDE│
+──────────────────────────────────────────────────────────────────────────────────────+
```

## Screen 4/5 — Review (click a completed ticket)

```
+────────────────────────────────────────+──────────────────────────────────────────+
│ BATCH — 3/3 done                       │  ENG-1264  Update descriptions of tasks  │
│ ─────────────────────────────────────  │  ─────────────────────────────────────── │
│                                        │  Diff │ Trail │ Tests │ Comments         │
│  1 ✓ ENG-1264  DONE  ▸ review    1:42  │  ─────────────────────────────────────── │
│  2 ✓ ENG-1440  DONE             2:15  │                                          │
│  3 ✓ ENG-1280  DONE             1:58  │  src/tasks/descriptions.ts  +12 -3       │
│                                        │  ┌──────────────────────────────────────┐│
│                                        │  │  14 │-  const desc = getDesc(task); ││
│                                        │  │  14 │+  const desc =               ││
│                                        │  │  15 │+    getLocalizedDesc(         ││
│                                        │  │  16 │+      task,                   ││
│                                        │  │  17 │+      settings.locale         ││
│                                        │  │  18 │+    );                        ││
│                                        │  └──────────────────────────────────────┘│
│                                        │                                          │
│                                        │  src/tasks/index.ts  +2 -0              │
│                                        │  ┌──────────────────────────────────────┐│
│                                        │  │   8 │+                              ││
│                                        │  │   9 │+  export { getLocalizedDesc } ││
│                                        │  └──────────────────────────────────────┘│
│                                        │                                          │
│                                        │  Add comment...                          │
│                                        │                                          │
+────────────────────────────────────────+──────────────────────────────────────────+
│ 3/3 done │ Approve  Rework  Finalize                         discourse-graph│CLAUDE│
+──────────────────────────────────────────────────────────────────────────────────────+
```

## Implementation Notes

### What Changes from Card Grid
- Card rendering fns (`compute-ticket-card-rects`, `compute-ticket-card-text-ops`) → list rendering fns
- `ticket-card-layout` → `ticket-list-layout` (rows, not grid)
- Single scroll → two independent scroll regions (left pane, right pane)
- `flow-canvas-active?` branching remains valid — just produces different data
- Need: vertical divider rect, group header rects, checkbox rects, row hover/select rects

### Data Requirements per Screen
- Screen 1: id, title, status, priority, assignee (current 5 fields suffice)
- Screen 2: same + execution stack order
- Screen 3: same + execution status, elapsed time, tool call trail
- Screen 4/5: same + diffs, test results, comments

### GPU Budget
- 21 tickets × ~2 rects each (row bg + checkbox) + group headers = ~50 rects
- 21 tickets × ~5 text ops each (checkbox, ID, title, priority, assignee) = ~105 text ops
- Well within existing buffers (50k rects, 1M text ops)
