# Softland Thread Map V1 (Codex)

Date: 2026-03-03
Input comparison set: `thread-map-claude.md`, `thread-map-codex.md`, `thread-map-gemini.md`

## 0) Standing Right Now

```text
THREAD-MAP QUALITY STANDING (current)

1) CLAUDE  >  Best narrative continuity + timeline readability + conflict storytelling
2) CODEX   >  Best evidence discipline + status conflict rulings + code-truth grounding
3) GEMINI  >  Best concise snapshot, but not enough depth for CEO-grade verification
```

## 1) Unified Status Legend

```text
[DONE]      shipped and working in current code path
[ACTIVE]    live thread; partially complete or still being extended
[BLOCKED]   cannot advance until explicit gate/dependency clears
[DEFERRED]  intentionally postponed (not killed)
[STALE]     doc claim out-of-date vs newer docs/code
```

## 2) Big-Picture System Flow (Best Unified View)

```text
                                   SOFTLAND PROGRAM (2026-03-03)

Editor Core [ACTIVE]
  ├─ Reactive-First Refactor [DONE]
  │   ├─ Agent Runtime + SSE Streaming [ACTIVE]
  │   │   ├─ Prompt-Driven Workflow Runtime [DONE]
  │   │   │   ├─ Screen 1 Intake [ACTIVE]
  │   │   │   └─ Screens 2-5 [BLOCKED]
  │   │   └─ Reasoning Trails Product Layer [ACTIVE]
  │   └─ Electric/Missionary Pattern Hardening [ACTIVE]
  │
  ├─ Rect Tree + Layout + SDF Stack [DONE]
  │   ├─ Sidebar DOM -> GPU -> Simplified [DONE]
  │   ├─ 3-Pane File Layout [DONE]
  │   └─ UI Excellence Phases 1-2 [DONE]
  │       └─ UI Excellence Phases 3-4 [ACTIVE]
  │
  └─ Design Converter [ACTIVE]
      ├─ Deterministic Browser Pipeline [DONE-files, ACTIVE-integration]
      ├─ JIT Component Library Architecture [DEFERRED]
      └─ Phase-3 Reconnect Path (`file-content-type` + `build-component-detail`) [ACTIVE]
```

## 3) Component-by-Component Status Board

```text
COMPONENT STATUS BOARD (what is where, now)

01. EDITOR CORE / WEBGPU CANVAS ..................... [ACTIVE]
02. LLM CLI INTEGRATION .............................. [ACTIVE]
03. ELECTRIC + MISSIONARY REACTIVE LAYER ............ [ACTIVE]
04. FLOW STATE MACHINE + COMMAND ACTIONS ............ [DONE]
05. PROMPT-DRIVEN UI WORKFLOW ........................ [ACTIVE]
06. SCREEN 1 (INTAKE MASTER-DETAIL) ................. [ACTIVE]
07. SCREENS 2-5 (ARRANGE/RUN/REVIEW) ................ [BLOCKED]
08. SIDEBAR SYSTEM ................................... [DONE]
09. 3-PANE WORKSPACE (EDITOR|CHAT|PREVIEW) .......... [DONE]
10. UI/UX EXCELLENCE PROGRAM ......................... [ACTIVE]
11. DESIGN CONVERTER PIPELINE ........................ [ACTIVE]
12. COMPONENT LIBRARY / JIT PATH ..................... [DEFERRED]
13. REVIEW PACK PROTOTYPE ............................ [DEFERRED]
14. REASONING TRAILS PRODUCTIZATION .................. [ACTIVE]
```

## 4) LLM CLI Integration Flow (Runtime Truth)

```text
[Cmd Panel Input]
       |
       v
 parse-agent-command
       |
       +--> :run (plain prompt)
       |      |
       |      v
       |   /api/agent/stream  ---> server stream parser ---> SSE events ---> agent panel/trail UI
       |
       +--> :flow-* command family
              |
              +--> /bootstrap --> /api/linear/issues (direct API)
              |
              +--> /run-flow /rework /finalize --> flow prompt template --> /api/agent/stream

Server Side Surface
  /api/agent/stream        [ACTIVE]
  /api/agent/run           [ACTIVE]
  /api/linear/issues       [DONE]
  /api/review-pack/*       [DEFERRED thread, endpoints alive]
  /api/components/*        [ACTIVE infra, DEFERRED UX]
  /api/extract/compile     [ACTIVE]
```

## 5) Electric + Reactive Graph (Operational Shape)

```text
 SOURCES (atoms/events)
   !editor-doc !flow-state !sidebar-state !agent-output !active-pane !scroll-x !viewport ...
            \      |           |              |            |            |         /
             \     |           |              |            |            |        /
              +-------------------- m/watch / m/latest composition ----------------+
                                         |
                                         +--> <editor-rects
                                         +--> <combined-text-ops
                                         +--> input/event consumers
                                         |
                                         v
                               GPU buffers (rect/text/shadow)
                                         |
                                         v
                                  draw-frame! (WebGPU)
```

## 6) UI/UX Actions Map (Command + Interaction Layer)

```text
USER ACTION MATRIX

Keyboard / Cmd Actions
  Ctrl+K        -> command panel toggle                     [DONE]
  Cmd+1/2/3     -> pane focus editor/chat/preview          [DONE]
  /bootstrap    -> idle->bootstrapping->intake             [DONE]
  /select       -> intake selection updates                 [DONE]
  /arrange      -> intake->arrange transition              [DONE]
  /run-flow     -> arrange->run transition                 [DONE]
  /review       -> review status display                   [DONE]
  /rework       -> review->rework->review loop             [DONE]
  /finalize     -> review->finalize->intake                [DONE]
  /status       -> flow-state introspection                [DONE]
  /reset        -> reset flow to idle                      [DONE]
  /extract      -> design converter preview mode           [ACTIVE]
  /hardcode     -> component demo injection                [ACTIVE]

Pointer / Scroll Actions
  Sidebar click/expand/open                               [DONE]
  Pane click -> active-pane switch                        [DONE]
  Editor horizontal scroll isolation                      [DONE]
  Status bar click no-op                                  [DONE]
  Flow-canvas row interactions                            [ACTIVE]
```

## 7) Workflow Screen State Machine (Product Path)

```text
                    (human override allowed to intake/arrange)

 [idle]
    |
    | /bootstrap
    v
 [bootstrapping] --retry--> [bootstrapping]
    |
    v
 [intake] --/arrange--> [arrange] --/run-flow--> [run] --done--> [review]
    ^                         ^                                  |      |
    |                         |                                  |      +--> /finalize --> [intake]
    +--------- /reset --------+                                  |
                                                             /rework
                                                                |
                                                                v
                                                              [rework] ---> [review]

Build status overlay:
  intake  [ACTIVE]
  arrange [BLOCKED by Screen-1 gate policy]
  run     [BLOCKED by upstream gate]
  review  [BLOCKED by upstream gate]
```

## 8) Sidebar + Component Library Evolution (Critical Conflict Area)

```text
SIDEBAR EVOLUTION

v1 DOM Sidebar (Session 16)         [DONE historical]
        |
        v
v2 WebGPU Sidebar (Session 26)      [DONE historical]
   - files/review/components modes existed
        |
        v
v3 Simplified Sidebar (Session 28)  [DONE current]
   - file explorer only
   - review/components branches removed from active UI path

COMPONENT THREAD CONSEQUENCE
  "Archive" labels in some docs  ---> [STALE wording risk]
  JIT architecture approved       ---> [TRUE]
  Phase-3 reintegration planned   ---> [TRUE]

RULING: Component thread is [DEFERRED], not killed.
```

## 9) Design Converter -> JIT -> Phase-3 Reconnect

```text
(what exists)                                    (what reconnects next)

Browser DOM / source code
      |
      v
 adapter -> token_matcher -> compiler -> verifier -> rt-node
      |
      +--> /api/extract/compile
      |
      +--> components/ inventory + _source.edn stubs
               |
               v
      JIT component architecture (approved, deferred UX)
               |
               v
      UI Excellence Phase 3 reconnect:
        - file-content-type
        - build-component-detail
        - status dots / type labels / convert CTA
```

## 10) What Is Actually Next (Execution Queue)

```text
NEXT QUEUE (most leverage first)

1) Close Screen-1 acceptance gate ........................ [UNBLOCKER]
2) Start UI Excellence Phase 3 (context-aware content) ... [ACTIVE queue]
3) Reconnect component detail + convert CTA path .......... [DEFERRED->ACTIVE]
4) Resume Screens 2-5 implementation path ................. [currently BLOCKED]
5) Continue reasoning-trail productization ................ [parallel ACTIVE]
```
