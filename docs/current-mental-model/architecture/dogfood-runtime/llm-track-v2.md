# LLM Track — v2 Architecture (post-Codex gate, with breadth labels)

Status: candidate architecture for the LLM track of the dogfood runtime,
2026-05-10. Codex-gated v2 plus four breadth labels to keep the spine from
narrowing into a chat-app architecture. Spine is implementation-ready;
breadth concepts are structurally hooked but require elaboration.

## Lineage

- v1 (full buildout): chat synthesis 2026-05-10 + `agent-track-aor.md`.
  Foregrounded plurality, DAG of forks, discourse graph, custom tools,
  replay, multi-agent.
- v2 (Codex gate): three repairs:
  - World owns derivatives, not just catalog.
  - Run vs Thread split (run = one execution; thread = the conversation).
  - ContextBundle as a first-class layer (immutable model input per turn).
  Plus a tiny home repair: ContextBundle home is World/Context, not LLM.
- v2 + breadth (this doc): four labels added so breadth stays visible:
  - parent-thread-id (DAG of forks)
  - multi-WorldThread bundles (reconciliation)
  - reverse-MCP (custom tools — agent reads world state)
  - typed projections (discourse graph as filter on artifact graph)

## Three Thin Views (canonical entry points)

These three views answer different questions. Use whichever matches the
question you're trying to answer. The fat composite view is at the bottom
of this doc for reference.

### View A — Thin spine (flow, with breadth labels in margin)

```text
   WorldThread chat-A         ← composed view; drafts / slices / edits / forks
         │                    ★ :parent-thread/id makes threads a DAG
         │ SEND               ★ may aggregate multiple WorldThreads (reconcile)
         ▼
   ContextBundle ctx-N        ← immutable model input for ONE turn
         │                    ★ the replay primitive
         ▼
   LLMTurnRun run-N           ← ONE execution
         │                    ★ reverse-MCP: agent reads world state
         ▼
   Raw LLM Items              ← immutable: user / assistant / tool / reasoning
         │
         ▼
   Object Catalog + Graph     ← citeable identity, typed edges
                              ★ discourse graph = filter {Q,C,E,D,R,F}
```

### View B — Thin containment (nesting / what-owns-what)

```text
   WorldThread chat-A
         │
         ▼
   LLMThread codex-thread-123
         │
         ├── TurnRun run-1
         │     ├── ContextBundle ctx-1   (frozen at SEND)
         │     └── items                  (user / assistant / tool / reasoning)
         │
         ├── TurnRun run-2
         │     ├── ContextBundle ctx-2
         │     └── items
         │
         └── TurnRun run-3
               ├── ContextBundle ctx-3
               └── items
```

ContextBundle and items are siblings of each TurnRun, not predecessors —
the bundle is the turn's input, items are the turn's output, both keyed
by run-id.

### View C — Thin fork DAG (plurality across chats)

```text
            WorldThread chat-A           (root)
                  │
        ┌─────────┼──────────┐
        ▼         ▼          ▼
    chat-A1   chat-A2    chat-A3        (siblings; same parent)
        │
    ┌───┼───┐
    ▼   ▼   ▼
  chat-B1 ...                            (deeper forks; preserved plurality)


                   chat-A1   chat-A2
                       \     /
                        \   /                 (multi-parent merge =
                         \ /                   reconciliation node;
                          ▼                    optional, late-bound)
                    chat-S1 (synthesis)
```

## When to use which view

```text
   QUESTION                                 VIEW

   "what flows where in one turn?"          A (spine)
   "how do thread/turn/item nest?"          B (containment)
   "how does plurality look across chats?"  C (fork DAG)
   "give me everything at once"             Fat composite (bottom)
```

## Four-Class Artifact Taxonomy

```text
   ┌──────────────────┬──────────────────────┬─────────────────────────┐
   │ CLASS            │ HOME                 │ NOTES                   │
   ├──────────────────┼──────────────────────┼─────────────────────────┤
   │ RAW LLM ITEM     │ LLM track            │ immutable; what model   │
   │                  │                      │ emitted                 │
   │ WORLD OVERLAY    │ World track          │ comment / annotation /  │
   │                  │                      │ slice / anchor /        │
   │                  │                      │ bookmark                │
   │ WORLD DERIVATIVE │ World track          │ edited version /        │
   │                  │                      │ curated excerpt /       │
   │                  │                      │ synthesized note        │
   │ CONTEXT BUNDLE   │ World/Context track  │ immutable; rendered     │
   │                  │ (consumed by LLM)    │ input for ONE turn      │
   └──────────────────┴──────────────────────┴─────────────────────────┘
```

## Source-of-Truth, Three Layers

```text
   ┌─────────────────────────────────────┐
   │ RAW EXECUTION TRUTH                 │
   │   LLM track                         │
   │   "what the model saw + emitted"    │
   └────────────────┬────────────────────┘
                    +
   ┌────────────────▼────────────────────┐
   │ USER MEANING / WORKING TRUTH        │
   │   World track                       │
   │   "what we annotate, edit, slice,   │
   │    fork, accept"                    │
   └────────────────┬────────────────────┘
                    +
   ┌────────────────▼────────────────────┐
   │ NEXT MODEL INPUT                    │
   │   ContextBundle                     │
   │   "what we choose to show now"      │
   └─────────────────────────────────────┘
```

## PState Renames (from v1)

```text
   OLD                          NEW
   ──────────────────────────   ─────────────────────────────────
   $$llm-runs                   $$llm-threads      (one per LLMThread)
                              + $$llm-turn-runs    (one per turn execution)

   $$llm-items-by-run           $$llm-items-by-thread
                              + $$llm-items-by-turn-run

   $$llm-conversation-graph     $$llm-thread-graph
   (keyed by run-id)            (keyed by thread-id; DAG of forks)

   (new)                        $$context-bundles  (one per turn;
                                                    immutable;
                                                    replay primitive)

   (new)                        $$world-derivatives (edited messages, etc.)
                                                    (or kind-typed:
                                                     $$edited-messages,
                                                     $$curated-excerpts,
                                                     $$synthesized-notes)

   $$objects                    $$objects          (unchanged — catalog)
                              + $$artifact-graph   (typed edges; discourse
                                                    graph is a projection)
```

## The Caveat (precise)

```text
  WRONG (overstated):
    "The LLM never sees the right column."

  RIGHT (precise):
    "The LLM never retroactively sees the user-side composed view.
     New turns receive a ContextBundle that may render parts of
     that view as fresh input."
```

## Pattern X vs Pattern Y

```text
  PATTERN X — DIRECT (acceptable for MVP)

    UI ──► *llm-depot ──► LLMTopology ──► $$llm-threads,
                                          $$llm-turn-runs
                                       ──► (catalog updated downstream)


  PATTERN Y — WORLD-FIRST (canonical long-term)

    UI ──► *world-action ──► WorldTopology
                                  │
                                  │ creates citeable pending
                                  │ chat/turn object in $$objects
                                  │ (visible BEFORE run starts)
                                  │
                                  │ foreign-append
                                  ▼
                              *llm-depot
                                  │
                                  ▼
                              LLMTopology
                                  │
                                  ▼
                              LLMTurnRun streams
                                  │
                                  ▼
                              raw items + catalog refs
```

For Sid's "everything is part of the world" instinct, Pattern Y is the
canonical shape. Pattern X is a refactor-later concession to MVP simplicity.

## Open Decisions (Sid-decidable, not cross-model decidable)

1. **ContextBundle physical home.** Three options:
   - Own depot (`*context-bundle-depot`) — auditable, replayable
   - Topology-internal (composed at SEND time, stored as part of `$$llm-turn-runs`)
   - World track (composed by WorldTopology, foreign-appended to LLMDepot)

2. **World derivatives organization.** Single PState (`$$world-derivatives`)
   vs kind-typed many (`$$edited-messages`, `$$curated-excerpts`,
   `$$synthesized-notes`). Trade-off: simplicity vs queryability.

3. **MVP commitment.** Pattern X day-one (and refactor later) or Pattern Y
   day-one? Trade-off: ship speed vs eventual rework.

4. **Derivative versioning.** Edit twice → keep both versions, or single
   mutable artifact? Trade-off: history vs storage.

## What Was Compressed By Codex (preserved as breadth labels)

```text
  CONCEPT                        WHERE IT LIVES IN V2+BREADTH

  Forks / DAG of threads         :parent-thread/id on $$llm-threads
                                 $$llm-thread-graph PState

  Plurality / disagreement       enabled by thread DAG;
                                 no automatic reconciliation

  Reconciliation / synthesis     ContextBundle aggregates from
                                 multiple WorldThreads

  Discourse graph                typed projection of $$artifact-graph
                                 filtered to {Q,C,E,D,R,F}

  Custom tools (Softland MCP)    reverse-channel from LLMTurnRun;
                                 tools read $$objects, etc.

  Replay / time-travel           ContextBundle is the replay primitive
                                 (Codex implicitly named it)

  Cost / quota tracking          observation-layer concern;
                                 no architecture change

  Approvals                      separate back-arrow loop
                                 (out-of-scope for this diagram)

  Canvas / semantic zoom         UI projection over PStates;
                                 no architecture change

  Multi-agent                    LLMThread polymorphic by agent kind
                                 (Codex / Claude / future agents)
```

## Bottom Line

```text
  v2 is the SPINE.
  The four breadth labels keep v2 from collapsing into a chat-app
  architecture.

  ACCEPT (from Codex gate):
    - Four-class artifact taxonomy
    - Three-level naming (Thread / TurnRun / Item)
    - ContextBundle as first-class (HOME: World/Context track)
    - Pattern Y as canonical (Pattern X for MVP)

  ADD (so breadth stays visible):
    - parent-thread-id field        (forks)
    - multi-WorldThread bundles     (reconciliation)
    - reverse-MCP back-arrow        (custom tools)
    - typed-projection notation     (discourse graph)

  DON'T SHIP V2 ALONE.
  V2 + BREADTH LABELS = THE ARCHITECTURE.
```

## Companions

- v1 full buildout: chat synthesis 2026-05-10 + `agent-track-aor.md`
- Compute-track sibling: `slice-a-compute-run-command.md` (4-tier asymmetry,
  back-arrow rule, claim/grant pattern)
- Three-depot framing: `three-depot-current-system.md` (World / Compute / LLM)
- Codex protocol map: chat synthesis 2026-05-10 (~78 EventMsg variants,
  Submission ops, mode comparison, JSON-RPC wire format)

## Fat Composite View (reference)

The dense five-layer diagram with everything inside each box. Use the thin
views above for orientation; consult this when you need full detail in one
picture.

```text
            USER-FACING CHAT / CANVAS
                 (working surface)
                       │
                       ▼
   ┌─────────────────────────────────────────────────┐
   │ WorldThread (composed view)                     │
   │   drafts, slices, comments, annotations,        │
   │   edited derivatives, forks, refs               │
   │   + pointers into raw LLM items                 │
   │                                                  │
   │   ◄── :parent-thread/id                         │  ★ DAG of forks
   │       (threads form a DAG via parent refs;      │
   │        plurality is preserved by default)       │
   └────────────────────┬────────────────────────────┘
                        │
                        │ SEND
                        │ (freeze the composed view)
                        │ (may aggregate from MULTIPLE                ★ reconciliation
                        │  WorldThreads → synthesis input)
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ ContextBundle  (HOME: World/Context track)      │
   │   immutable model input for ONE upcoming turn   │
   │   renders: quotes, slices, edited derivatives,  │
   │            notes, system instructions           │
   │                                                  │
   │   THE ONLY THING THE MODEL ACTUALLY SEES         │
   │   THE REPLAY PRIMITIVE                           │
   └────────────────────┬────────────────────────────┘
                        │
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ LLMTurnRun                                      │
   │   one execution of one turn                     │
   │   receives ContextBundle, streams output        │
   │                                                  │
   │   ◄── reverse-channel: Softland-MCP server      │  ★ custom tools
   │       (agent calls tools that read $$objects,   │
   │        $$discourse-graph, $$llm-threads, etc.   │
   │        → self-introspective AI)                 │
   └────────────────────┬────────────────────────────┘
                        │
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ Raw LLM Items                                   │
   │   immutable: user-as-sent, assistant output,    │
   │              tool calls, reasoning,             │
   │              token usage                        │
   └────────────────────┬────────────────────────────┘
                        │
                        │ catalog / cite
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ Object Catalog + Artifact Graph                 │
   │   makes raw items citeable, sliceable, anchored │
   │                                                  │
   │   discourse-graph = typed projection where      │  ★ discourse
   │     artifact/kind ∈ {Q,C,E,D,R,F}               │
   │   (no separate substrate; filtered view)        │
   └─────────────────────────────────────────────────┘

   ★ = breadth labels (small additions that prevent v2 from
       collapsing into a chat-app architecture)
```
