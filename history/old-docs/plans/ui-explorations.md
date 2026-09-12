# UI Explorations Archive (Codex + Claude + Final Decision)

> Date: 2026-02-19
> Purpose: preserve all explored UI directions before the lock, including both Codex and Claude proposals.
> Final lock: Approach 1 (Master-Detail Split) — see `docs/plans/commission-consensus.md` Section 11.

## 0. Final Decision (Locked)

Chosen approach: **Master-Detail Split**.

Why selected:
1. Fits the user's screen-by-screen workflow with low cognitive load.
2. Preserves persistent context in left pane while right pane evolves by phase.
3. Handles sequential/parallel planning cleanly in Screen 2.
4. Best path to implement without breaking existing loop/reactive architecture.

Primary references:
- Locked process + state/flow contract: `docs/plans/commission-consensus.md`
- Approved full-screen ASCII mockups (master-detail): `docs/plans/ui-mockups-master-detail.md`
- Screen 1 build spec (master-detail): `docs/plans/screen-1-spec.md`

---

## 1. Codex Explored UIs

### 1.1 Progressive Flow (recommended by Codex pre-lock)

```txt
SCREEN 1: List Intake (scan + select)
┌───────────────────────────────────────────────────────────────────────────────┐
│ Filter: [Mine v] [Status v] [Priority v] [Search...............]   21 tickets│
├───────────────────────────────────────────────────────────────────────────────┤
│ [ ] ENG-1207  P1  In Progress  Global left sidebar                          │
│ [x] ENG-1272  P2  Todo         Port small global settings                    │
│ [x] ENG-1273  P3  Review       Port small personal settings                  │
│ ...                                                                           │
├───────────────────────────────────────────────────────────────────────────────┤
│ Selected: 2 tickets                                         [Continue -> Arrange]│
└───────────────────────────────────────────────────────────────────────────────┘

SCREEN 2: Arrange Canvas
┌────────────────────────────┬───────────────────────────────────────┬──────────────┐
│ Selected Backlog           │ Arrange Canvas                        │ Details      │
│ [≡] ENG-1272              │ Sequential: [ ENG-1272 ]->[ ENG-1291 ]│ ENG-1272     │
│ [≡] ENG-1291              │ Parallel:   [Lane A: ENG-1273] [B: ]  │ status, AC   │
│ [≡] ENG-1273              │                         [Run Plan]      │ deps, files  │
└────────────────────────────┴───────────────────────────────────────┴──────────────┘
```

### 1.2 Always-On Tri-Pane

```txt
┌────────────────────────────┬───────────────────────────────────────┬──────────────┐
│ Backlog (all tickets)      │ Arrange Canvas                        │ Details      │
│ [ ] ENG-1207 P1 InProg     │ Sequential: [T1]->[T2]->[T3]         │ ENG-1207     │
│ [x] ENG-1272 P2 Todo       │ Parallel:   [P1:T4,T7] [P2:T8] [P3:] │ full text    │
│ [x] ENG-1273 P3 Review     │                        [Run] [Save]   │ AC, deps     │
└────────────────────────────┴───────────────────────────────────────┴──────────────┘
```

### 1.3 Kanban + Arrangement Drawer

```txt
┌───────────────────────────────────────────────────────────────────────────────┐
│ Todo (9)                  In Progress (1)             Ready Review (9)        │
│ [ ] ENG-1291              [ ] ENG-1207                [x] ENG-1272            │
│ ...                                                                          │
├───────────────────────────────────────────────────────────────────────────────┤
│ Selected: 2 tickets                                            [Arrange Plan >]│
└───────────────────────────────────────────────────────────────────────────────┘

ARRANGEMENT DRAWER
┌──────────────────────────────────────────────┐
│ Sequential: [ ENG-1272 ] -> [ ENG-1273 ]     │
│ Parallel lanes: [A: ] [B: ] [C: ]            │
│                           [Run Plan] [Close] │
└──────────────────────────────────────────────┘
```

---

## 2. Claude Explored UIs

### 2.1 Approach 1: Master-Detail Split (chosen)

Pattern:
- Persistent list on left
- Context workspace on right
- Same split across Screen 1..5 with right-pane content changing by phase

Captured full mockups:
- Screen 1 Intake
- Screen 2 Arrange
- Screen 3 Run
- Screen 4/5 Review

See full approved ASCII: `docs/plans/ui-mockups-master-detail.md`.

### 2.2 Approach 2: Kanban Flow

Pattern:
- Multi-column board (`AVAILABLE`, `SELECTED`, `SEQUENCE`, etc.)
- Good global visibility of status movement
- Weaker than master-detail for detailed sequential chaining + focused review

Representative frame:

```txt
+──────────────────┬──────────────────┬──────────────────┬─────────────────────────────+
| AVAILABLE (21)   | SELECTED (3)     | SEQUENCE         |                             |
| Ready to Merge   | ENG-1264    P3   | 1→ ENG-1264     | ▶ /run                      |
| Ready for Review | ENG-1440    P3   | 2→ ENG-1440     | Mode: Sequential/Parallel   |
| In Progress      | ENG-1280    P3   | 3→ ENG-1280     |                             |
+──────────────────┴──────────────────┴──────────────────┴─────────────────────────────+
```

### 2.3 Approach 3: Timeline Feed

Pattern:
- Vertical event feed (bootstrap, batch creation, run updates, review actions)
- Best auditability and narrative flow
- Best as a supporting lens, not primary planning UI

Representative frame:

```txt
┌ BOOTSTRAP ───────────────────────── 11:42 AM ─────────────────────────┐
│ Loaded 21 active tickets from Linear. Select tickets, then /arrange. │
└────────────────────────────────────────────────────────────────────────┘

┌ ENG-1264 ─── ✓ DONE 1:42 ─────────────────────────────────────────────┐
│ ▸ Read ...  ▸ Edit ... +12 -3  ▸ Bash typecheck ✓                    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Comparison Snapshot

| Approach | Strength | Weakness | Decision |
|----------|----------|----------|----------|
| Master-Detail Split | Best clarity + continuity across screens | Slightly less "board-like" at glance | **Chosen** |
| Kanban Flow | Great macro status visibility | More clutter for run/review details | Rejected as primary |
| Timeline Feed | Excellent audit trail | Not ideal for selection/arrangement | Use as supporting view |
| Always-On Tri-Pane | Power-user density | High initial complexity/crowding | Not chosen |
| Progressive Flow | Strong staged UX | Requires explicit mode transitions | Incorporated into chosen flow |

---

## 4. Implementation Guidance from This Exploration

1. Keep master-detail as base shell for all screens.
2. Add timeline feed as an optional/secondary panel in Run/Review.
3. Keep kanban-like grouping in Screen 1 list via collapsible status sections.
4. Preserve command contract consistency:
   - `/arrange sequential|parallel`
   - `/run-flow`
   - `/finalize`
   - `/reset`


---

## 5. Verbatim Archive (As Pasted by User from Claude)

> Source: user-pasted Claude message
> Note: preserved verbatim for historical traceability, including command strings that may differ from current locked contract.

```txt
Approach 1: Master-Detail Split

  The Linear/email pattern. Persistent list left, contextual workspace right. Closest to your sketch.

  Screen 1 — Intake (select from list)
  +────────────────────────────────────────+──────────────────────────────────────────+
  │ DISCOURSE-GRAPH  21 active             │                                          │
  │ ─────────────────────────────────────  │                                          │
  │                                        │                                          │
  │ ● Ready to Merge  3                    │      No tickets selected yet.            │
  │   ☐ ENG-1274  Add feature flags   P2   │                                          │
  │   ☐ ENG-1272  Port all small gl…  P2   │      Select tickets from the list,       │
  │   ☐ ENG-1273  Port all small pe…  P2   │      then /arrange to start.             │
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

  Screen 2 — Arrange (selected tickets in right panel)
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
  │   ☐ ENG-1217  Section componen…   P3   │  ▶ /run to start execution               │
  │   ☐ ENG-1291  Port discourse n…   P3   │                                          │
  │   ☐ ENG-1290  Port Discourse n…   P3   │                                          │
  │                                        │                                          │
  │ ◐ In Progress  1                       │                                          │
  │   ☐ ENG-1207  Global left side…   P1   │                                          │
  │                                        │                                          │
  +────────────────────────────────────────+──────────────────────────────────────────+
  │ 3 selected │ sequential                                      discourse-graph│CLAUDE│
  +──────────────────────────────────────────────────────────────────────────────────────+

  Screen 3 — Run (execution progress)
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

  Screen 4/5 — Review (click a completed ticket)
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
  │                                        │  💬 Add comment...                       │
  │                                        │                                          │
  +────────────────────────────────────────+──────────────────────────────────────────+
  │ 3/3 done │ ✓ Approve  ✎ Rework  ▶ Finalize                  discourse-graph│CLAUDE│
  +──────────────────────────────────────────────────────────────────────────────────────+

  ---
  Approach 2: Kanban Flow

  Tickets move across columns. Like Trello but columns = your flow states. Best for seeing all tickets' status at once.

  Screen 1+2 — Intake + Arrange (one view)
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ DISCOURSE-GRAPH  Batch Planner                                                       │
  +──────────────────┬──────────────────┬──────────────────┬─────────────────────────────+
  │ AVAILABLE (21)   │ SELECTED (3)     │ SEQUENCE         │                             │
  │ ────────────     │ ────────────     │ ────────────     │                             │
  │                  │                  │                  │                             │
  │ Ready to Merge   │ ENG-1264    P3   │ 1→ ENG-1264     │     ▶ /run                  │
  │  ENG-1274   P2   │ ENG-1440    P3   │    ▼             │                             │
  │  ENG-1272   P2   │ ENG-1280    P3   │ 2→ ENG-1440     │     Execution mode:         │
  │  ENG-1273   P2   │                  │    ▼             │     ● Sequential            │
  │                  │                  │ 3→ ENG-1280     │     ○ Parallel              │
  │ Ready for Review │                  │                  │                             │
  │  ENG-1264 → P3   │                  │                  │                             │
  │  ENG-1281   P3   │                  │                  │                             │
  │  ENG-1440 → P3   │                  │                  │                             │
  │  ENG-1438   P3   │                  │                  │                             │
  │  ENG-1225   P3   │                  │                  │                             │
  │  ENG-1280 → P3   │                  │                  │                             │
  │  ENG-1217   P3   │                  │                  │                             │
  │  ENG-1291   P3   │                  │                  │                             │
  │  ENG-1290   P3   │                  │                  │                             │
  │                  │                  │                  │                             │
  │ In Progress      │                  │                  │                             │
  │  ENG-1207   P1   │                  │                  │                             │
  │                  │                  │                  │                             │
  │ Todo (9)  ▾      │                  │                  │                             │
  │                  │                  │                  │                             │
  +──────────────────┴──────────────────┴──────────────────┴─────────────────────────────+
  │ 3 selected │ sequential                                      discourse-graph│CLAUDE│
  +──────────────────────────────────────────────────────────────────────────────────────+

  Screen 3 — Run (columns show progress)
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ DISCOURSE-GRAPH  Running batch (1/3)                                                 │
  +──────────────────┬──────────────────┬──────────────────┬─────────────────────────────+
  │ QUEUE            │ RUNNING          │ DONE             │ AGENT OUTPUT                │
  │ ────────────     │ ────────────     │ ────────────     │ ──────────────              │
  │                  │                  │                  │                             │
  │ ENG-1280    P3   │ ENG-1440    P3   │ ENG-1264  ✓ P3  │ [CLAUDE] running:           │
  │                  │ ██████░░░░ 0:38  │ 1:42  3 edits   │ Port page groups            │
  │                  │                  │ 1 file changed   │ settings in suggestive      │
  │                  │ ▸ Read index.ts  │                  │ mode                        │
  │                  │ ▸ Edit config.ts │ click to review  │                             │
  │                  │ ▸ Bash typecheck │                  │ ▸ Read suggestive.ts        │
  │                  │                  │                  │ ▸ Read page-groups.ts       │
  │                  │                  │                  │ ▸ Edit suggestive.ts        │
  │                  │                  │                  │   +28 -4                    │
  │                  │                  │                  │                             │
  │                  │                  │                  │ I'm updating the page       │
  │                  │                  │                  │ group settings to use       │
  │                  │                  │                  │ the new suggestive mode     │
  │                  │                  │                  │ configuration pattern...    │
  │                  │                  │                  │                             │
  │                  │                  │                  │                             │
  │                  │                  │                  │                             │
  │                  │                  │                  │                             │
  +──────────────────┴──────────────────┴──────────────────┴─────────────────────────────+
  │ 1/3 done │ ENG-1440 running                                  discourse-graph│CLAUDE│
  +──────────────────────────────────────────────────────────────────────────────────────+

  Screen 4/5 — Review (click a done ticket, expands inline)
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ DISCOURSE-GRAPH  Batch complete (3/3)                                                │
  +──────────────────┬───────────────────────────────────────────────────────────────────+
  │ RESULTS          │  ENG-1264  Update descriptions of tasks                           │
  │ ────────────     │  ──────────────────────────────────────────────────────────────── │
  │                  │  Diff │ Trail │ Tests                                              │
  │ ✓ ENG-1264 ◂    │  ──────────────────────────────────────────────────────────────── │
  │   1:42  3 edits  │                                                                   │
  │                  │  src/tasks/descriptions.ts  +12 -3                                │
  │ ✓ ENG-1440      │   14 │- const desc = getDesc(task);                               │
  │   2:15  5 edits  │   14 │+ const desc = getLocalizedDesc(task, settings.locale);     │
  │                  │                                                                   │
  │ ✓ ENG-1280      │  src/tasks/index.ts  +2 -0                                        │
  │   1:58  2 edits  │    9 │+ export { getLocalizedDesc }                               │
  │                  │                                                                   │
  │                  │  ──────────────────────────────────────────────────────────────── │
  │                  │  💬 Looks good. Approve?                                          │
  │                  │  ──────────────────────────────────────────────────────────────── │
  │                  │  ✓ Approve   ✎ Rework   Skip                                     │
  │                  │                                                                   │
  │                  │                                                                   │
  +──────────────────┴───────────────────────────────────────────────────────────────────+
  │ 3/3 done │ 0 approved │ ▶ Finalize when all approved              discourse-graph│CL│
  +──────────────────────────────────────────────────────────────────────────────────────+

  ---
  Approach 3: Timeline Feed

  Every action is an event in a vertical feed. Like GitHub PR timeline or Slack. Best for understanding what happened and why.

  Screen 1 — Intake (tickets as feed items)
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ DISCOURSE-GRAPH                                                         Filter ▾     │
  +──────────────────────────────────────────────────────────────────────────────────────+
  │                                                                                      │
  │  ┌ BOOTSTRAP ─────────────────────────────────────── 11:42 AM ─────────────────────┐ │
  │  │ Loaded 21 active tickets from Linear.  Select tickets, then /arrange.           │ │
  │  └─────────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                      │
  │  ● Ready to Merge (3)                                                                │
  │  ☐ ENG-1274  Add feature flags in settings                            P2  siddharth  │
  │  ☐ ENG-1272  Port all small global settings                           P2  siddharth  │
  │  ☐ ENG-1273  Port all small personal settings                         P2  siddharth  │
  │                                                                                      │
  │  ○ Ready for Review (9)                                                              │
  │  ☑ ENG-1264  Update descriptions of tasks                             P3  siddharth  │
  │  ☐ ENG-1281  Port Discourse Node block panel                          P3  siddharth  │
  │  ☑ ENG-1440  Port page groups settings in suggestive mode             P3  siddharth  │
  │  ☐ ENG-1438  Port Keyboard shortcut keys/triggers settings            P3  siddharth  │
  │  ☐ ENG-1225  Port small Discourse node settings                       P3  siddharth  │
  │  ☑ ENG-1280  Port Discourse node index setting                        P3  siddharth  │
  │  ☐ ENG-1217  Section components in Left sidebar                       P3  siddharth  │
  │  ☐ ENG-1291  Port discourse node specification                        P3  siddharth  │
  │  ☐ ENG-1290  Port Discourse node config panels                        P3  siddharth  │
  │                                                                                      │
  │  ◐ In Progress (1)                                                                   │
  │  ☐ ENG-1207  Global left sidebar                                      P1  siddharth  │
  │                                                                                      │
  │  ○ Todo (9)  ▾ collapsed                                                             │
  │                                                                                      │
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ 3 selected │ /arrange sequential|parallel                    discourse-graph│CLAUDE │
  +──────────────────────────────────────────────────────────────────────────────────────+

  Screen 2+3 — Arrange + Run (events append to feed)
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ DISCOURSE-GRAPH                                                         Filter ▾     │
  +──────────────────────────────────────────────────────────────────────────────────────+
  │                                                                                      │
  │  ┌ BATCH: 3 tickets, sequential ──────────────────── 11:45 AM ─────────────────────┐ │
  │  │ 1→ ENG-1264  2→ ENG-1440  3→ ENG-1280                                          │ │
  │  └─────────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                      │
  │  ┌ ENG-1264  Update descriptions ─── ✓ DONE 1:42 ─────────────────────────────────┐ │
  │  │  ▸ Read src/tasks/descriptions.ts                                               │ │
  │  │  ▸ Read src/tasks/index.ts                                                      │ │
  │  │  ▸ Edit src/tasks/descriptions.ts  +12 -3                                       │ │
  │  │  ▸ Bash npm run typecheck  ✓                                                    │ │
  │  │  ▸ Edit src/tasks/index.ts  +2 -0                                               │ │
  │  │  "Updated task descriptions to use localized strings."                           │ │
  │  └─────────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                      │
  │  ┌ ENG-1440  Port page groups ─── ◐ RUNNING 0:38 ─────────────────────────────────┐ │
  │  │  ▸ Read suggestive.ts                                                           │ │
  │  │  ▸ Read page-groups.ts                                                          │ │
  │  │  ▸ Edit suggestive.ts  +28 -4                                                   │ │
  │  │  █ I'm updating the page group settings to use the new suggestive mode...       │ │
  │  └─────────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                      │
  │  ┌ ENG-1280  Port Discourse node index ─── ○ WAITING ──────────────────────────────┐ │
  │  │  Queued. Starts after ENG-1440 completes.                                       │ │
  │  └─────────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                      │
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ 1/3 done │ ENG-1440 running 0:38                             discourse-graph│CLAUDE │
  +──────────────────────────────────────────────────────────────────────────────────────+

  Screen 4/5 — Review (expand a completed ticket in the feed)
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ DISCOURSE-GRAPH                                                         Filter ▾     │
  +──────────────────────────────────────────────────────────────────────────────────────+
  │                                                                                      │
  │  ┌ ENG-1264  Update descriptions ─── ✓ DONE 1:42 ──────── ▾ expanded ─────────────┐ │
  │  │                                                                                 │ │
  │  │  src/tasks/descriptions.ts  +12 -3                                              │ │
  │  │  ┌────────────────────────────────────────────────────────────────────────┐      │ │
  │  │  │  14 │- const desc = getDesc(task);                                   │      │ │
  │  │  │  14 │+ const desc = getLocalizedDesc(task, settings.locale);         │      │ │
  │  │  └────────────────────────────────────────────────────────────────────────┘      │ │
  │  │                                                                                 │ │
  │  │  src/tasks/index.ts  +2 -0                                                      │ │
  │  │  ┌────────────────────────────────────────────────────────────────────────┐      │ │
  │  │  │   9 │+ export { getLocalizedDesc }                                   │      │ │
  │  │  └────────────────────────────────────────────────────────────────────────┘      │ │
  │  │                                                                                 │ │
  │  │  💬 > Looks clean. Approve.                                                     │ │
  │  │  ┌──────────────────────────────────────────────────────────────┐               │ │
  │  │  │ ✓ Approve │ ✎ Rework: "change X to Y" │ Skip               │               │ │
  │  │  └──────────────────────────────────────────────────────────────┘               │ │
  │  └─────────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                      │
  │  ┌ ENG-1440  Port page groups ─── ✓ DONE 2:15 ──────── ▸ collapsed ───────────────┐ │
  │  │  5 edits, 3 files changed.  Click to expand.                                   │ │
  │  └─────────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                      │
  │  ┌ ENG-1280  Port Discourse node ─── ✓ DONE 1:58 ──── ▸ collapsed ────────────────┐ │
  │  │  2 edits, 1 file changed.  Click to expand.                                    │ │
  │  └─────────────────────────────────────────────────────────────────────────────────┘ │
  │                                                                                      │
  +──────────────────────────────────────────────────────────────────────────────────────+
  │ 3/3 done │ 1 approved │ ▶ Finalize                           discourse-graph│CLAUDE │
  +──────────────────────────────────────────────────────────────────────────────────────+
```
