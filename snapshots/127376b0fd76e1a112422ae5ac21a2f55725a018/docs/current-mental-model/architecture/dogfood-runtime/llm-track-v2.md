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
- **2026-05-10 v2.1 (post-second-review)**: three additional repairs:
  - **WorldTurn added** between WorldThread and ContextBundle (not
    every user move triggers an LLM execution).
  - **Pattern X removed** as a valid architecture path. World-first
    is the only canonical send.
  - **Fork semantics corrected** to thread-level only (Codex's
    `thread/fork` takes `threadId` + config; no `fromItemId`).
    Softland owns span anchoring via slice-and-quote.

## Three Thin Views (canonical entry points)

These three views answer different questions. Use whichever matches the
question you're trying to answer. The fat composite view is at the bottom
of this doc for reference.

### View A — Thin spine (flow, with breadth labels in margin)

```text
   WorldThread chat-A         ← long-lived chat container
         │                    ★ :parent-thread/id makes threads a DAG
         ▼
   WorldTurn WT-N             ← ONE user move
         │                       (compose-and-send / slice / edit /
         │                        accept-patch / cancel / fork / ...)
         │
         │  only :compose-and-send WorldTurns continue down
         │
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

   Non-:compose-and-send WorldTurns route directly to world artifacts
   (overlays / derivatives / decisions) without ContextBundle or
   LLMTurnRun.
```

### View B — Thin containment (nesting / what-owns-what)

```text
   WorldThread chat-A
         │
         ▼
   LLMThread codex-thread-123                 (parallel structure on
         │                                     the execution side)
         │
         │  WorldTurns and their consequences:
         │
         ├── WT-1 (compose & send)  ──► ContextBundle B-1 ──► LLMTurnRun run-1
         │                                                          └─ items
         │
         ├── WT-2 (slice paragraph) ──► world overlay only (no LLM)
         │
         ├── WT-3 (edit derivative) ──► world derivative only (no LLM)
         │
         ├── WT-4 (follow-up & send)──► ContextBundle B-4 ──► LLMTurnRun run-2
         │                                                          └─ items
         │
         └── WT-5 (accept patch)    ──► world catalog mutation (no LLM)
```

ContextBundle and items are siblings of each LLMTurnRun, not
predecessors. WorldTurns are NOT all paired with LLMTurnRuns — only
`:compose-and-send` WorldTurns produce a ContextBundle + LLMTurnRun
on the LLMThread. Most user moves never involve the model.

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

   $$llm-conversation-graph     $$world-thread-graph
   (keyed by run-id)          + $$llm-thread-graph

                                $$world-thread-graph:
                                  user-facing fork/reconciliation DAG

                                $$llm-thread-graph:
                                  native executor fork lineage

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

## World-first send (canonical)

All user intents flow through the World track first. There is **no**
direct UI → `*llm-depot` path. (Earlier versions of this doc listed a
"Pattern X" — direct-to-LLM — as acceptable for MVP. That option has
been removed. World-first is the only canonical path.)

```text
    UI ──► *world-action ──► WorldTopology
                                  │
                                  │ creates WorldTurn + ContextBundle
                                  │ + pending citeable object in $$objects
                                  │ (citeable BEFORE LLM run starts)
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

Why world-first is the only path: every LLM execution should have a
world-side authored intent behind it. Capability checks, approval
gating, provenance, and catalog citeability all live in the World
track. Bypassing world creates an asymmetry where some chats are
first-class citeable objects and others aren't — exactly what
collapses Softland into a chat app.

## Codex fork semantics (thread-level only)

Codex's `app-server thread/fork` takes `threadId` + config overrides
ONLY. No `fromItemId`. No `excludeTurns`. Earlier inference of
item-boundary fork was wrong. Confirmed by regenerating the local
TypeScript schema.

```text
   Codex fork    = thread-LEVEL fork (whole history copied)
   Softland      = owns span/item anchoring via slice-and-quote:
                     1. create $$slices[S-1] with content snapshot
                     2. thread/fork {threadId, ...config} → new thread
                     3. turn/start with input quoting S-1.content
```

Span semantics live in Softland data (`$$slices`, `$$artifact-graph`),
not in the Codex wire.

## Open Decisions (Sid-decidable, not cross-model decidable)

1. **ContextBundle physical home.** Three options:
   - Own depot (`*context-bundle-depot`) — auditable, replayable
   - Topology-internal (composed in WorldTopology at SEND time,
     stored in `$$context-bundles` before foreign-append to LLMDepot)
   - World track sub-component
   (Either way it's authored on the World/Context side per world-first;
   the choice is where in that side it physically lives.)

2. **World derivatives organization.** Single PState (`$$world-derivatives`)
   vs kind-typed many (`$$edited-messages`, `$$curated-excerpts`,
   `$$synthesized-notes`). Trade-off: simplicity vs queryability.

3. **Derivative versioning.** Edit twice → keep both versions, or single
   mutable artifact? Trade-off: history vs storage.

(Earlier versions of this doc listed "Pattern X vs Pattern Y" as an open
decision. **Closed**: world-first / Pattern Y is canonical. See "World-first
send" section.)

## What Was Compressed By Codex (preserved as breadth labels)

```text
  CONCEPT                        WHERE IT LIVES IN V2+BREADTH

  Forks / DAG of threads         :parent-thread/id on $$world-threads
                                 $$world-thread-graph PState

  Native fork lineage            $$llm-thread-graph PState

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
  v2.1 is the SPINE.
  The four breadth labels keep v2 from collapsing into a chat-app
  architecture.

  CANONICAL (post-second-review):
    - Four-class artifact taxonomy (raw / overlay / derivative / bundle)
    - Five-level naming (WorldThread / WorldTurn / [ContextBundle /
      LLMTurnRun] / LLMItem; LLMThread parallel on execution side)
    - ContextBundle as first-class (HOME: World/Context track)
    - World-first send (the only path; Pattern X removed)
    - Codex thread/fork is thread-level only; Softland owns span anchoring

  BREADTH LABELS (must stay visible):
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
   │ WorldThread (long-lived chat container)         │
   │   refs + pointers into world artifacts and      │
   │   raw LLM items                                  │
   │                                                  │
   │   ◄── :parent-thread/id                         │  ★ DAG of forks
   │       (threads form a DAG via parent refs;      │
   │        plurality is preserved by default)       │
   └────────────────────┬────────────────────────────┘
                        │
                        ▼
   ┌─────────────────────────────────────────────────┐
   │ WorldTurn (one user move)                       │
   │   kind: :compose-and-send | :slice | :edit |    │
   │         :accept-patch | :cancel | :fork |       │
   │         :reconcile | :abandon | ...             │
   │                                                  │
   │   only :compose-and-send continues to LLM;      │
   │   other kinds write to world artifacts only     │
   └────────────────────┬────────────────────────────┘
                        │
                        │ SEND  (only :compose-and-send)
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
