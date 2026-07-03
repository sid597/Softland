# Task: Build the Softland Project Thread Map

## What You're Building

A single document that visually maps every development thread, decision fork, and dependency in the Softland project from inception to now (March 3, 2026).

This is a **verification artifact for the CEO** — not documentation for developers. It should be:
- **Visual first**: ASCII trees/graphs, not prose paragraphs
- **Detailed but not bloated**: Every thread and fork point, but concise per node
- **Honest status labels**: DONE, ACTIVE, DEFERRED (+ reason), BLOCKED (+ blocker), STALE (docs not updated)
- **Never use "ARCHIVE"** unless the user explicitly decided to kill a feature. A thread whose code was removed during a refactor but whose plan still exists is DEFERRED, not archived.

The CEO should be able to read this in 10-15 minutes and say "yes, this is correct" or "no, this fork happened differently."

## Output Files — One Per LLM

Each LLM writes to its own file. Do NOT write to another LLM's file.

| LLM | Output file |
|-----|-------------|
| **Claude** | `docs/thread-map-claude.md` |
| **Gemini** | `docs/thread-map-gemini.md` |
| **Codex** | `docs/thread-map-codex.md` |

The CEO will compare all three and merge the best parts.

## Suggested Structure

```
1. Master Thread Graph (ASCII art — the full picture, ~50-80 lines)
   - Show all threads as branches from a timeline
   - Show fork points, merge points, and current status
   - Use visual markers: [DONE] [ACTIVE] [DEFERRED] [BLOCKED] [STALE]

2. Thread Detail Cards (one per thread, ~10-20 lines each)
   - Thread name
   - Origin: which session/doc spawned it
   - Depends on: other threads
   - Feeds into: other threads
   - Key artifacts: files created/modified
   - Current status + evidence (which doc says what)
   - Where it reconnects: if deferred, which future thread picks it up

3. Decision Log (chronological)
   - Every fork/decision point with date, what was decided, what was deferred
   - Who decided (user vs AI agent inference)

4. Status Conflicts (where docs disagree)
   - List cases where different docs say different things about the same thread
   - Flag which version is correct based on evidence
```

## Source Documents to Read (IN THIS ORDER)

### Living docs (current state of truth — but may be stale)
1. `docs/_map.md` — master doc index with causality tree
2. `docs/plans/where-we-are.md` — orientation doc (what exists, decisions, what's next)
3. `docs/plans/ui-ux-tracker.md` — UI/UX implementation checklist (Tier 1-3)
4. `docs/plans/ui-excellence-spec.md` — UI excellence phases (1-4) with per-section status
5. `docs/sessions/next-prompt.md` — resume point for next session

### Architecture docs (design decisions — some may be stale)
6. `docs/architecture/rect-tree-ui.md` — scene graph architecture
7. `docs/architecture/virtual-layout-engine.md` — layout engine
8. `docs/architecture/gpu-component-library.md` — GPU component library vision (Zed GPUI model)
9. `docs/architecture/component-library-jit.md` — JIT component library (**says "APPROVED SOLUTION" but `_map.md` says "archive" — this is a known conflict**)
10. `docs/architecture/design-converter-how-it-works.md` — design converter explanation
11. `docs/architecture/reactive-first-plan.md` — reactive architecture decision

### Vision docs (north star — not all implemented)
12. `docs/vision/design-brief-reasoning-trails.md` — core product definition
13. `docs/vision/design-converter-ux.md` — design converter UX vision
14. `docs/vision/prompt-driven-ui-gemini.md` — prompt-driven UI strategy
15. `docs/exploration/claude-3-moonshot.md` — the moonshot vision (Context Teleporter)

### Planning docs (consensus decisions)
16. `docs/plans/commission-consensus.md` — multi-LLM planning commission (Sections 1-10 approved)
17. `docs/plans/ui-mockups-master-detail.md` — ASCII mockups for all screens
18. `docs/plans/screen-1-spec.md` — Screen 1 spec (SUPERSEDED by list view)
19. `docs/plans/ui-explorations.md` — explored UI alternatives

### Session logs (what actually happened, chronological)
20. `docs/history/progressive-summary.md` — compact implementation history (ALL sessions)
21. `docs/sessions/2026-03-01-design-converter.md` — design converter session
22. `docs/sessions/2026-03-01-jit-component-library.md` — JIT pivot session
23. All other files in `docs/sessions/` — read for context

### Multi-LLM collaboration artifacts
24. `docs/softland/consensus-design-converter.md` — 3-LLM consensus on design converter
25. `docs/softland/claude/design-converter-plan.md` — Claude's converter plan
26. `docs/softland/gemini/ui-ingestion-pipeline.md` — Gemini's converter plan

### Code-level truth (what's actually in the codebase)
27. `CLAUDE.md` (project root) — agent instructions, critical patterns
28. `src/app/client/webgpu/loop.cljs` — THE main file (~5700 lines), all UI logic
29. `src/app/electric_flow.cljc` — entry point, server/client bootstrap
30. `src/app/server_jetty.clj` — HTTP endpoints, agent streaming
31. `components/` directory — 62 `_source.edn` stubs, pipeline files, token maps
32. `src/components/` — CSS parsers, compiler, adapter, design IR, verifier, token matcher, design tokens

## Key Context You Need

### The Stack
- **Clojure** (Rama backend) + **ClojureScript** (WebGPU frontend) + **Electric 3** (reactive framework)
- **Missionary** for reactive flows (m/latest, m/reduce, m/eduction, m/watch)
- **WebGPU** for all rendering — no DOM UI, everything is GPU rects + MSDF text on a single canvas
- **Rama** for backend state (PStates, depots, stream topologies)
- **SCI** (Small Clojure Interpreter) for in-browser Clojure eval

### The Product Vision (condensed)
Softland is a tool where an AI agent does development work, and the artifact of that work is not just the code diff but the **reasoning trail** — the exploration path, decision tree, rejected alternatives. The UI is a spatial workspace (like Linear meets Zed meets Figma) rendered entirely on a WebGPU canvas.

### Known Thread Relationships (verify against docs)

```
Editor Core ──────────> Rect Tree UI ──────> Layout Engine ──> Composable Components
                             |                    |
Agent Runtime ─────────> Agent Output Panel       |
     |                       |                    |
     └──> Reasoning Trails   └──> 3-Pane Layout ──┘
               |                      |
Planning Commission ──> Prompt-Driven UI ──> Screen 1 (Intake)
     |                      |                    |
     └──> Design Converter ──> JIT Component Lib  ├──> Screen 2 (Arrange) [not started]
               |                    |              ├──> Screen 3 (Run) [not started]
               └──> UI Excellence ──┘              └──> Screen 4/5 (Review) [not started]

Sidebar (DOM) ──> Sidebar (WebGPU) ──> Sidebar (simplified)
                                           |
                                    removed component/review tabs
                                    BUT component flow still planned in UI Excellence Phase 3
```

### Critical Status Conflict to Document
- `_map.md` and `where-we-are.md` label Component Library as "Archive"
- `component-library-jit.md` itself says "APPROVED SOLUTION"
- `ui-excellence-spec.md` Phase 3 references `build-component-detail` as active TODO
- `next-prompt.md` points to Phase 3 as the next implementation step
- **Reality**: The component library was DEFERRED when the sidebar was simplified (session 28), not archived. The plan is to re-integrate it through Phase 3 of UI Excellence via `file-content-type` detection.

### Session Timeline (from progressive-summary.md)
```
Session 1-7:   Editor foundations (caret, editing, bracket matching, SCI, core features, UI system)
Session 8:     Reactive-first architecture refactor (the Great Refactor)
Session 9-10:  CLI integration, Electric fixes
Session 11:    Architecture audit — discovered System A (working) vs System B (dead)
Session 12-13: Agent output panel, streaming, code folding, status bar (Tier 1 complete)
Session 14:    Streaming fix (text overlap bug)
Session 15:    Multi-agent vision sessions
Session 16:    Sidebar (imperative DOM), settings panel
Session 17:    Streaming architecture + review pack prototype
Session 18:    Flow state machine, slash commands, auto-bootstrap (Step 4)
Session 19:    Streaming overhaul (token-level)
Session 20:    Review pack API
Session 21-22: Screen 1 intake list implementation
Session 23:    Bootstrap rewired to direct Linear API
Session 24-25: Rect tree UI + DnD
Session 26:    Sidebar rewritten from DOM to WebGPU
Session 27:    SDF rich quads + shadows + design tokens + component library
Session 27-28: Design converter pipeline (3 LLMs)
Session 28:    Sidebar simplification + 3-pane default + idle start (~540 lines removed)
Session 29:    UI Excellence pass Phases 1-2
Session 30:    JIT component library pivot (same day as design converter)
```

Note: Session numbers may not be perfectly sequential with dates. Session 30 (JIT pivot) is dated Mar 1, session 28 (sidebar simplify) is dated Mar 2. Cross-reference with `progressive-summary.md` for accuracy.

## Rules

1. **Write ONLY to your designated output file** (see table above) — do not modify any existing file
2. **Read docs fully** — don't skim. The devil is in the status labels and the session details.
3. **When docs conflict, flag it** — don't silently pick one version. Show both and explain why they disagree.
4. **Distinguish user decisions from AI inferences** — if an AI agent labeled something "archive" but the user never said that, flag it as an AI inference, not a user decision.
5. **Include evidence** — for every status claim, cite the doc + line/section that supports it.
6. **Visual first** — if you can express something as a tree/graph instead of a paragraph, do that.
