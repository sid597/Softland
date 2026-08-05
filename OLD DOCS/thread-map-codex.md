# Softland Project Thread Map (Codex)

## 1. Master Thread Graph (ASCII)

```
Legend: [DONE] [ACTIVE] [DEFERRED] [BLOCKED] [STALE-CLAIM]

2026-02-09                                                                                         2026-03-03
   |                                                                                                   |
   v                                                                                                   v
FOUNDATION TRACK
  T01 Editor Core + WebGPU Shell [ACTIVE]
  (sessions 1-7 editor foundations)
   |
   +--> T02 Reactive-First Refactor [DONE]
   |      |
   |      +--> T03 Agent Runtime + SSE Streaming [ACTIVE]
   |      |      |
   |      |      +--> T17 Reasoning Trails / Tool-Use Trail [ACTIVE]
   |      |
   |      +--> T04 Flow State Machine + Slash Commands [DONE]
   |             |
   |             +--> T06 Screen 1 Intake (Master-Detail) [ACTIVE]
   |             |      |
   |             |      +--> T16 Screens 2-5 Prompt-Driven Workflow [BLOCKED]
   |             |
   |             +--> T07 Direct Linear Bootstrap API [DONE]
   |
   +--> T08 Rect Tree UI [DONE]
          |
          +--> T09 Virtual Layout Engine + Composable Components [DONE]
          |
          +--> T10 Sidebar Evolution (DOM -> WebGPU -> Simplified File Explorer) [DONE]
          |      |
          |      +--> T05 Review Pack UI Mode [DEFERRED]
          |
          +--> T11 3-Pane File Layout (Editor|Chat|Preview) [DONE]
                 |
                 +--> T12 UI Excellence Program [ACTIVE]
                        |
                        +--> T18 Phase-3 Context-Aware Reintegration [ACTIVE]

DESIGN-CONVERTER / COMPONENT TRACK
  T13 Design Converter Pipeline (multi-LLM) [ACTIVE]
   |
   +--> T14 Component Inventory + _source.edn Stub Corpus [DEFERRED]
   |
   +--> T15 JIT Component Library Architecture [DEFERRED]
          |
          +--> (planned reconnect via T18)

PLANNING / DECISION TRACK
  Commission Consensus -> Master-Detail Lock -> Screen-1 First rule
   |
   +--> T06 implementation priority
   +--> T16 blocked until Screen-1 gate passes

CONFLICT FLAGS
  [!] Component Library status labeling conflict:
      _map/where-we-are = "Archive" [STALE-CLAIM]
      component-library-jit + next-prompt + ui-excellence = reintegration path exists

  [!] Session numbering/date conflict:
      "Session 30" dated 2026-03-01 while "Session 28" work dated 2026-03-02
      -> chronology must follow absolute dates, not session ordinals

  [!] Auto-bootstrap semantics drift:
      older docs: auto-bootstrap on load
      current code: auto-bootstrap disabled, manual /bootstrap
```

## 2. Thread Detail Cards

### T01 — Editor Core + WebGPU Shell [ACTIVE]
- Origin: Session 1-7 editor/caret/editing foundations (`docs/history/progressive-summary.md:10`, `docs/history/progressive-summary.md:42`).
- Depends on: none.
- Feeds into: T02, T08, T10, T11.
- Key artifacts: `src/app/client/webgpu/loop.cljs`, `src/app/electric_flow.cljc`.
- Current status + evidence: ACTIVE (core is "~90% done" with remaining editor polish like save/smooth-scroll still open) (`docs/plans/where-we-are.md:258`, `docs/plans/ui-ux-tracker.md:130`).
- Reconnect path: continues as baseline platform for all UI/workflow threads.

### T02 — Reactive-First Refactor [DONE]
- Origin: "Great Refactor" session (`docs/history/progressive-summary.md:47`).
- Depends on: T01.
- Feeds into: T03, T04, T12.
- Key artifacts: Missionary flow model + architecture plan (`docs/architecture/reactive-first-plan.md:5`, `docs/architecture/reactive-first-plan.md:29`).
- Current status + evidence: DONE as a completed refactor milestone; now used as the operating model (`docs/history/progressive-summary.md:47`, `docs/_map.md:47`).
- Reconnect path: none required; this is foundation.

### T03 — Agent Runtime + SSE Streaming [ACTIVE]
- Origin: streaming sessions and server stream pipeline (`docs/sessions/2026-02-13-streaming-threads.md:6`, `docs/history/progressive-summary.md:121`).
- Depends on: T02.
- Feeds into: T17, T11.
- Key artifacts: `/api/agent/stream`, stream parser/invariants (`src/app/server_jetty.clj:577`, `src/app/server_jetty.clj:880`), client stream consumer in loop.
- Current status + evidence: ACTIVE (working stream path + ongoing extensions) (`docs/plans/where-we-are.md:259`, `docs/plans/ui-ux-tracker.md:123`).
- Reconnect path: feeds review/reasoning UX (T17/T16).

### T04 — Flow State Machine + Slash Commands [DONE]
- Origin: Step 4 implementation session (`docs/sessions/2026-02-18-step4-flow-state-machine.md:4`).
- Depends on: T02, T03.
- Feeds into: T06, T16.
- Key artifacts: `flow-transitions`, `initial-flow-state`, 9 slash commands (`src/app/client/webgpu/loop.cljs:523`, `src/app/client/webgpu/loop.cljs:410`).
- Current status + evidence: DONE (Tier 2b marked complete + commands implemented in code) (`docs/plans/ui-ux-tracker.md:23`, `src/app/client/webgpu/loop.cljs:476`).
- Reconnect path: used continuously by intake/arrange/run/review flows.

### T05 — Review Pack Prototype / Review Mode [DEFERRED]
- Origin: Review Pack v0 session (`docs/sessions/2026-02-13-review-pack-v0.md:5`).
- Depends on: T03.
- Feeds into: T16 (future review surface).
- Key artifacts: review-pack endpoints remain live (`src/app/server_jetty.clj:714`, `src/app/server_jetty.clj:725`).
- Current status + evidence: DEFERRED (prototype exists; sidebar integration removed) (`docs/plans/where-we-are.md:268`, `docs/history/progressive-summary.md:446`).
- Reconnect path: can return through Screen 4/5 review implementation.

### T06 — Screen 1 Intake (Master-Detail) [ACTIVE]
- Origin: master-detail lock + Screen 1 implementation (`docs/plans/commission-consensus.md:296`, `docs/plans/screen-1-spec.md:1`).
- Depends on: T04, T08.
- Feeds into: T16.
- Key artifacts: `build-intake-tree` / intake rendering path (`docs/history/progressive-summary.md:175`, `src/app/client/webgpu/loop.cljs:2341`).
- Current status + evidence: ACTIVE (implemented but acceptance gate still pending) (`docs/plans/where-we-are.md:264`, `docs/plans/where-we-are.md:147`).
- Reconnect path: once gate passes, transitions to Screen 2/3/4/5 thread.

### T07 — Direct Linear Bootstrap API [DONE]
- Origin: bootstrap direct API milestone (`docs/plans/ui-ux-tracker.md:39`, `docs/history/progressive-summary.md:153`).
- Depends on: T03, T04.
- Feeds into: T06.
- Key artifacts: `/api/linear/issues` endpoint + `/bootstrap` path (`src/app/server_jetty.clj:818`, `src/app/client/webgpu/loop.cljs:449`).
- Current status + evidence: DONE (`docs/plans/ui-ux-tracker.md:39`, `src/app/server_jetty.clj:818`).
- Reconnect path: none.

### T08 — Rect Tree UI [DONE]
- Origin: rect-tree architecture + implementation sessions (`docs/architecture/rect-tree-ui.md:3`, `docs/history/progressive-summary.md:172`).
- Depends on: T01.
- Feeds into: T09, T10, T11.
- Key artifacts: rect-tree pipeline and wrappers (`docs/architecture/rect-tree-ui.md:359`, `docs/architecture/rect-tree-ui.md:367`).
- Current status + evidence: DONE (steps 1-7 complete) (`docs/architecture/rect-tree-ui.md:371`).
- Reconnect path: none.

### T09 — Virtual Layout Engine + Composable Components [DONE]
- Origin: layout-engine architecture + delivery (`docs/architecture/virtual-layout-engine.md:3`, `docs/history/progressive-summary.md:187`).
- Depends on: T08.
- Feeds into: T10, T11, T12.
- Key artifacts: `resolve-layout`, composable panel/list pattern (`docs/architecture/virtual-layout-engine.md:46`, `docs/architecture/virtual-layout-engine.md:111`).
- Current status + evidence: DONE (all phases complete) (`docs/architecture/virtual-layout-engine.md:3`).
- Reconnect path: none.

### T10 — Sidebar Evolution (DOM -> WebGPU -> Simplified) [DONE]
- Origin: Session 16 DOM sidebar -> Session 26 WebGPU migration -> Session 28 simplification (`docs/history/progressive-summary.md:85`, `docs/history/progressive-summary.md:198`, `docs/history/progressive-summary.md:442`).
- Depends on: T08, T09.
- Feeds into: T11, T12.
- Key artifacts: file-explorer-only sidebar tree + 7-key sidebar state (`src/app/client/webgpu/loop.cljs:1577`, `src/app/client/webgpu/loop.cljs:4268`).
- Current status + evidence: DONE for current target shape (simplified file explorer shipped) (`docs/plans/where-we-are.md:267`, `docs/history/progressive-summary.md:447`).
- Reconnect path: component/review modes were removed and now reconnect through T18.

### T11 — 3-Pane File Layout (Editor|Chat|Preview) [DONE]
- Origin: Session 28 generalization to all open files (`docs/history/progressive-summary.md:450`).
- Depends on: T08, T09, T10.
- Feeds into: T12, T13.
- Key artifacts: `build-file-layout` with 40/30/30 split (`src/app/client/webgpu/loop.cljs:1793`, `src/app/client/webgpu/loop.cljs:1809`).
- Current status + evidence: DONE (implemented as default open-file layout) (`docs/plans/where-we-are.md:266`, `docs/history/progressive-summary.md:453`).
- Reconnect path: basis for phase-3 context-aware content.

### T12 — UI Excellence Program [ACTIVE]
- Origin: UI excellence specification and session 29 pass (`docs/plans/ui-excellence-spec.md:1`, `docs/history/progressive-summary.md:463`).
- Depends on: T10, T11.
- Feeds into: T18.
- Key artifacts: typography/depth/focus/empty-state polish in loop + char-width changes (`docs/plans/ui-excellence-spec.md:6`, `docs/plans/ui-excellence-spec.md:252`).
- Current status + evidence: ACTIVE (Phase 1-2 done, Phase 3-4 TODO) (`docs/plans/ui-excellence-spec.md:6`, `docs/plans/ui-excellence-spec.md:273`).
- Reconnect path: Phase 3 starts component-detail + sidebar metadata work.

### T13 — Design Converter Pipeline [ACTIVE]
- Origin: multi-LLM design-converter build (`docs/sessions/2026-03-01-design-converter.md:1`, `docs/history/progressive-summary.md:418`).
- Depends on: T09, T11.
- Feeds into: T14, T15, T18.
- Key artifacts: adapter/tokenize/compile/verifier path (`src/components/adapter.cljc:52`, `src/components/token_matcher.cljc:200`, `src/components/compiler.cljc:229`, `src/components/_verifier.cljc:1`), server compile endpoint (`src/app/server_jetty.clj:823`).
- Current status + evidence: ACTIVE (pipeline built; end-to-end/visual acceptance still pending) (`docs/plans/where-we-are.md:269`, `docs/plans/ui-ux-tracker.md:116`).
- Reconnect path: feeds component-detail/reintegration work (T18).

### T14 — Component Inventory + Stub Corpus [DEFERRED]
- Origin: component registry/stub generation + status files (`components/_registry.edn:3`, `components/card/_source.edn:2`).
- Depends on: T13.
- Feeds into: T15, T18.
- Key artifacts: 59-component registry + per-component `_source.edn` statuses (`components/_registry.edn:14`, `components/button/_source.edn:1`).
- Current status + evidence: DEFERRED (inventory exists, mostly stubbed, not wired back into sidebar UX) (`docs/history/progressive-summary.md:446`, `docs/plans/where-we-are.md:270`).
- Reconnect path: Phase-3 component-detail/status-dot work.

### T15 — JIT Component Library Architecture [DEFERRED]
- Origin: Session 30 pivot from extraction-first to JIT conversion (`docs/sessions/2026-03-01-jit-component-library.md:1`, `docs/sessions/2026-03-01-jit-component-library.md:5`).
- Depends on: T13, T14.
- Feeds into: T18.
- Key artifacts: approved JIT architecture doc and interaction model (`docs/architecture/component-library-jit.md:3`, `docs/architecture/component-library-jit.md:324`).
- Current status + evidence: DEFERRED (architecture approved, runtime/sidebar integration not currently active) (`docs/architecture/component-library-jit.md:3`, `docs/history/progressive-summary.md:446`).
- Reconnect path: resume via context-aware component detail flow in UI Excellence Phase 3.

### T16 — Prompt-Driven Screens 2-5 [BLOCKED]
- Origin: 5-screen workflow contract + master-detail per-screen model (`docs/plans/commission-consensus.md:27`, `docs/plans/commission-consensus.md:303`).
- Depends on: T06 gate completion.
- Feeds into: final workflow product.
- Key artifacts: screen mockups/specs (`docs/plans/ui-mockups-master-detail.md:3`, `docs/plans/screen-1-spec.md:186`).
- Current status + evidence: BLOCKED (explicit handoff rule: Screen 2 blocked until Screen 1 PASS) (`docs/plans/commission-consensus.md:294`, `docs/plans/where-we-are.md:147`).
- Reconnect path: unblock after Screen 1 acceptance checklist passes.

### T17 — Reasoning Trails / Context Teleporter Lineage [ACTIVE]
- Origin: vision brief + streaming-thread architecture (`docs/vision/design-brief-reasoning-trails.md:32`, `docs/sessions/2026-02-13-streaming-threads.md:22`).
- Depends on: T03.
- Feeds into: T16 Screen 5 review UX.
- Key artifacts: trail event concepts + stream contract extensions (`docs/sessions/2026-02-13-streaming-threads.md:129`, `docs/plans/where-we-are.md:260`).
- Current status + evidence: ACTIVE (in-progress lane; review-panel integration still pending) (`docs/plans/where-we-are.md:260`, `docs/plans/ui-ux-tracker.md:123`).
- Reconnect path: Screen 4/5 review implementation.

### T18 — Phase-3 Context-Aware Reintegration [ACTIVE]
- Origin: UI Excellence Phase 3 + next-prompt handoff (`docs/plans/ui-excellence-spec.md:273`, `docs/sessions/next-prompt.md:17`).
- Depends on: T12, T13, T14, T15.
- Feeds into: restored component-centric workflow in sidebar/detail pane.
- Key artifacts: planned `file-content-type`, `build-component-detail`, status dots, convert CTA (`docs/plans/ui-excellence-spec.md:207`, `docs/plans/ui-excellence-spec.md:275`).
- Current status + evidence: ACTIVE (declared as the next step, not started yet) (`docs/sessions/next-prompt.md:7`, `docs/sessions/next-prompt.md:17`).
- Reconnect path: this is the reconnect path for deferred component threads.

## 3. Decision Log (Chronological)

| Date | Decision / Fork | Outcome | Deferred / Blocked | Owner | Evidence |
|---|---|---|---|---|---|
| 2026-02-11 | Architecture audit split system into working path vs inert scaffold | Rama session continuity wiring added; architecture truth clarified | Full streaming + richer persistence still future | AI_INFERENCE | `docs/sessions/2026-02-11-architecture-audit.md:9`, `docs/sessions/2026-02-11-architecture-audit.md:91` |
| 2026-02-13 | Review Pack v0 built as risk-first slice | Review-pack domain + API + tests shipped | Durable persistence + richer UI actions deferred | AI_INFERENCE | `docs/sessions/2026-02-13-review-pack-v0.md:5`, `docs/sessions/2026-02-13-review-pack-v0.md:79` |
| 2026-02-13 | Token-level SSE streaming path chosen | StreamableResponseBody + client stream rendering implemented | Tool-use trail enrichment still iterative | AI_INFERENCE | `docs/sessions/2026-02-13-streaming-threads.md:6`, `docs/sessions/2026-02-13-streaming-threads.md:13` |
| 2026-02-18 | V0 flow-state machine model implemented | 9 slash-command state graph wired | Visual layers beyond V0 deferred | AI_INFERENCE | `docs/sessions/2026-02-18-step4-flow-state-machine.md:4`, `docs/sessions/2026-02-18-step4-flow-state-machine.md:26` |
| 2026-02-18 | Commission locks workflow contract | Prompt-as-API + event/state contract adopted | Scope discipline for deferred items | AI_INFERENCE | `docs/plans/commission-consensus.md:4`, `docs/plans/commission-consensus.md:94` |
| 2026-02-19 | UI direction chosen: master-detail across screens | Screen shell pattern locked | Screen 3/4/5 affordances explicitly deferred | USER | `docs/plans/commission-consensus.md:298`, `docs/plans/commission-consensus.md:350`, `docs/plans/commission-consensus.md:291` |
| 2026-02-25 | Bootstrap path pivots to direct Linear API | `/api/linear/issues` becomes source for `/bootstrap` | None for this slice | AI_INFERENCE | `docs/plans/ui-ux-tracker.md:39`, `src/app/server_jetty.clj:818` |
| 2026-02-25 | Rect-tree architecture implemented for intake surface | Scene-graph rendering path replaces flat ticket builders | Editor still keeps flat path | AI_INFERENCE | `docs/history/progressive-summary.md:172`, `docs/architecture/rect-tree-ui.md:367` |
| 2026-02-27 | Layout engine + composable component model completed | `resolve-layout` + composable pattern stabilized | None for core layout layer | AI_INFERENCE | `docs/architecture/virtual-layout-engine.md:3`, `docs/architecture/virtual-layout-engine.md:46` |
| 2026-03-01 | Multi-LLM design converter pipeline shipped | Deterministic + LLM-assisted extraction converge to IR pipeline | End-to-end visual acceptance still pending | AI_INFERENCE | `docs/history/progressive-summary.md:418`, `docs/plans/ui-ux-tracker.md:116` |
| 2026-03-01 | Browser-extraction-first assumption challenged; JIT model proposed | JIT architecture documented as approved solution | Runtime reintegration not yet active | USER (problem framing) + AI_INFERENCE (formalization) | `docs/sessions/2026-03-01-jit-component-library.md:120`, `docs/sessions/2026-03-01-jit-component-library.md:126`, `docs/architecture/component-library-jit.md:3` |
| 2026-03-02 | Sidebar scope reduced to file explorer only | Review/component sidebar modes removed; 7-key sidebar state | Component/review UX deferred | AI_INFERENCE | `docs/history/progressive-summary.md:442`, `docs/history/progressive-summary.md:447`, `src/app/client/webgpu/loop.cljs:4268` |
| 2026-03-02 | 3-pane layout generalized for all open files | Editor/Chat/Preview 40/30/30 default path for files | Rich preview content deferred | AI_INFERENCE | `docs/history/progressive-summary.md:450`, `src/app/client/webgpu/loop.cljs:1793`, `src/app/client/webgpu/loop.cljs:1809` |
| 2026-03-02 | Ticket canvas gated behind explicit bootstrap | Initial state set to `:idle`; manual bootstrap model | Auto-bootstrap behavior retired in code | AI_INFERENCE | `docs/history/progressive-summary.md:457`, `src/app/client/webgpu/loop.cljs:547`, `src/app/client/webgpu/loop.cljs:5234` |
| 2026-03-02 | UI Excellence Phase 1-2 pass completes | Rendering integrity + depth/focus/typography upgraded | Phase 3+4 pending | AI_INFERENCE | `docs/plans/ui-excellence-spec.md:6`, `docs/plans/ui-excellence-spec.md:252` |
| 2026-03-03 | Horizontal scroll correctness hardening lands | Gutter/text contamination fix completed in tracker | None for this fix slice | AI_INFERENCE | `docs/plans/ui-ux-tracker.md:97` |

## 4. Status Conflicts (Docs Disagree -> Ruling)

| Conflict | Claim A | Claim B | Why mismatch | Correct ruling |
|---|---|---|---|---|
| C01 Component library status | `_map` and `where-we-are` label component library as "Archive" (`docs/_map.md:58`, `docs/plans/where-we-are.md:270`) | JIT doc says "APPROVED SOLUTION" and Phase-3 docs define reintegration work (`docs/architecture/component-library-jit.md:3`, `docs/sessions/next-prompt.md:17`, `docs/plans/ui-excellence-spec.md:275`) | "Archive" was a snapshot label after sidebar simplification; feature intent remained alive | Treat as **DEFERRED**, not killed. Archive label is a **STALE-CLAIM** unless explicit user kill is recorded. |
| C02 Session numbering vs chronology | "Session 30" is dated 2026-03-01 (`docs/sessions/2026-03-01-jit-component-library.md:1`, `docs/sessions/2026-03-01-jit-component-library.md:3`) | "Session 28" work appears on 2026-03-02 (`docs/history/progressive-summary.md:442`) | Session numbers are narrative labels, not strict chronological IDs | Use **absolute dates** for timeline ordering. |
| C03 Screen 1 completion semantics | Screen 1 shown as "Implemented" (`docs/plans/where-we-are.md:264`) | Screen 1 acceptance gate still pending and Screen 2 blocked until pass (`docs/plans/where-we-are.md:147`, `docs/plans/commission-consensus.md:294`) | "Implemented" used for shipped UI slice; gate criteria not yet fully closed | Thread status should be **ACTIVE** (implemented, not fully closed). |
| C04 Auto-bootstrap semantics drift | Step-4 session records auto-bootstrap behavior (`docs/sessions/2026-02-18-step4-flow-state-machine.md:36`) | Current code explicitly disables auto-bootstrap and requires manual `/bootstrap` (`src/app/client/webgpu/loop.cljs:5234`) | Behavior changed in later simplification pass; older docs not fully reconciled | Runtime truth = **manual bootstrap**. Earlier auto-bootstrap note is **STALE**. |
| C05 Char-width invariant drift | UI excellence claims char-width normalized to 0.56 (`docs/plans/ui-excellence-spec.md:44`, `docs/plans/ui-excellence-spec.md:49`) | `electric_flow.cljc` still contains `char-advance (* font-size 0.60)` in one path (`src/app/electric_flow.cljc:466`) while another path uses 0.56 (`src/app/electric_flow.cljc:413`) | Partial patch / dual code paths | Mark as **open consistency debt** under T12; do not treat as fully closed invariant yet. |

