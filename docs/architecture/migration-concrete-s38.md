# Migration to Differential Architecture — Session 38

> **Date:** 2026-03-15
> **What this is:** The concrete migration from snapshot-rebuild (current) to differential pipeline (target). This is what we work on.
> **Destination sketch:** `differential-pipeline-s38.md` — the full differential vision with all diagrams
> **Origin:** Started from "should interactions be pull-based?" → discovered the differential principle is the real answer.

---

## The Core Principle: Differential End-to-End

Every layer should operate on changes, never on full state.

```
    SNAPSHOT MODEL (current)              DIFFERENTIAL MODEL (target)
    ════════════════════════              ═════════════════════════════

    state changes                         state changes
         │                                     │
    rebuild EVERYTHING                    compute WHAT CHANGED
    O(total)                              O(changed)
         │                                     │
    compare to previous                   emit diff
    O(1) identity check                   O(changed)
         │                                     │
    same? → skip                          apply diff to target
    different? → upload ALL               O(changed)
    O(total)                                   │
         │                                done.
    done.                                 never touched unchanged nodes.
```

The diff compounds across layers — Rama, Electric, and WebGPU each receive diffs and emit diffs:

```
    RAMA                    ELECTRIC                WEBGPU
    ═════                   ════════                ══════

    1000 nodes              "150 entered            "add 150 rects
    in PState               viewport"               to buffer"

    new event               e/diff-by               mount callback:
    appended                computes                append-child
         │                  structural              (:grow 150)
         │                  diff:                        │
         ▼                  {:grow 150                   ▼
    topology                 :shrink 0              GPU allocates
    processes               :permutation {}         150 buffer slots
    ONLY the                :change {50 nodeA       uploads 150 rects
    new event                        51 nodeB       ONLY
    (incremental)                    ...}}
         │                       │                       │
         ▼                       ▼                       ▼
      Δ ──────────────→ Δ ──────────────→ Δ ──────────→ draw

    The ENTIRE pipeline is  Δ → Δ → Δ → Δ

    No layer ever sees the full 1000.
    Each layer processes only what changed.
```

---

## 1. The Three Things (Not Five Layers)

```
╔═══════════════════════════╦═══════════════════════════════╦══════════════════════════════╗
║                           ║                               ║                              ║
║      R A M A              ║       E L E C T R I C         ║       W E B G P U            ║
║   (ground truth)          ║    (connective tissue)        ║      (terminal)              ║
║                           ║                               ║                              ║
╠═══════════════════════════╬═══════════════════════════════╬══════════════════════════════╣
║                           ║                               ║                              ║
║  events go in             ║  watches PStates              ║  receives diffs              ║
║  state materializes       ║  diffs collections            ║  draws what it's told        ║
║  PStates hold             ║  transfers server→client      ║  reports what's visible      ║
║  queryable state          ║  manages lifecycles           ║                              ║
║                           ║  spans the whole program      ║                              ║
║                           ║                               ║                              ║
╠═══════════════════════════╬═══════════════════════════════╬══════════════════════════════╣
║                           ║                               ║                              ║
║  you DON'T think about    ║  you DON'T think about        ║  you DO think about          ║
║  the wire between         ║  the API between              ║  buffer pools,               ║
║  event and PState         ║  server and client            ║  atlas, batching,            ║
║                           ║                               ║  and present strategy        ║
║  (like you don't think    ║  (Felix: "I haven't thought   ║                              ║
║   about B-tree            ║   about an API in four        ║  (this is the custom         ║
║   rebalancing             ║   months")                    ║   engineering work)           ║
║   in Postgres)            ║                               ║                              ║
║                           ║                               ║                              ║
╚═══════════════════════════╩═══════════════════════════════╩══════════════════════════════╝
```

---

## 2. What Electric Gives vs What's Still Your Job

```
╔══════════════════════════════════════╦══════════════════════════════════════╗
║                                      ║                                      ║
║    WHAT ELECTRIC GIVES YOU           ║    WHAT'S STILL YOUR JOB             ║
║    (grounded, from code)             ║    (custom, sink-specific)           ║
║                                      ║                                      ║
╠══════════════════════════════════════╬══════════════════════════════════════╣
║                                      ║                                      ║
║  ✓ Generic keyed diffs               ║  ✗ GPU buffer pool / allocator       ║
║    (diff-by, for-by, on-unmount)     ║    (slot management, free lists)     ║
║                                      ║                                      ║
║  ✓ No API plumbing                   ║  ✗ GPU scene reconciler              ║
║    (server→client via websocket)     ║    (how diffs map to draw calls)     ║
║                                      ║                                      ║
║  ✓ Direct effectful mutation         ║  ✗ Present / pacing strategy         ║
║    (no virtual DOM, synchronous      ║    (dirty flag + RAF one-shot,       ║
║     settlement)                      ║     or m/relieve, or other)          ║
║                                      ║                                      ║
║  ✓ Stable identity across updates    ║  ✗ Text atlas management             ║
║    (keyed items preserved)           ║    (glyph caching, MSDF at zoom)    ║
║                                      ║                                      ║
║  ✓ Automatic cleanup on removal      ║  ✗ Batching and draw ordering        ║
║    (on-unmount lifecycle)            ║    (layer sorting, z-order)          ║
║                                      ║                                      ║
║  ✓ Composable in one program         ║  ✗ Spatial indexing for culling      ║
║    (e/server + e/client in           ║    (quadtree, or server-side         ║
║     one e/defn)                      ║     Rama spatial query)              ║
║                                      ║                                      ║
╠══════════════════════════════════════╬══════════════════════════════════════╣
║                                      ║                                      ║
║  The differential substrate —        ║  The GPU integration —               ║
║  the hard CONCEPTUAL problem         ║  the hard ENGINEERING problem        ║
║  that Electric solves.               ║  that remains.                       ║
║                                      ║                                      ║
╚══════════════════════════════════════╩══════════════════════════════════════╝
```

---

## The Differential Primitives (What Makes This Work)

### Electric's Diff Structure (from incseq.cljc source)

```
    ╔══════════════════════════════════════════════════════════════╗
    ║                    ELECTRIC DIFF SHAPE                       ║
    ╠══════════════════════════════════════════════════════════════╣
    ║                                                              ║
    ║   {:grow          3           ← 3 new slots appeared         ║
    ║    :degree        8           ← total size after growing     ║
    ║    :shrink        0           ← 0 slots removed              ║
    ║    :permutation   {0 2, 2 0}  ← items at 0 and 2 swapped    ║
    ║    :change        {5 :new-a   ← slot 5 got a new value      ║
    ║                    6 :new-b                                  ║
    ║                    7 :new-c}}                                ║
    ║                                                              ║
    ║   This is NOT virtual DOM diffing.                           ║
    ║   This is STRUCTURAL diffing at the language level.          ║
    ║   Every operator receives diffs and produces diffs.          ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
```

### Electric's Generic Mount (from mount_impl.cljc source)

```
    ╔══════════════════════════════════════════════════════════════╗
    ║                    GENERIC MOUNT                             ║
    ╠══════════════════════════════════════════════════════════════╣
    ║                                                              ║
    ║   (mount                                                     ║
    ║     append-child      ;; new item → add to scene             ║
    ║     replace-child     ;; item changed → update in place      ║
    ║     insert-before     ;; item moved → reorder                ║
    ║     remove-child      ;; item gone → free resources          ║
    ║     nth-child)        ;; lookup by index                     ║
    ║                                                              ║
    ║   For DOM:            For WebGPU:                            ║
    ║   ────────            ───────────                            ║
    ║   appendChild()       allocate buffer slot                   ║
    ║   replaceChild()      update buffer data                     ║
    ║   insertBefore()      reorder draw index                     ║
    ║   removeChild()       free buffer slot                       ║
    ║   childNodes[i]       lookup by slot index                   ║
    ║                                                              ║
    ║   Same mount. Different callbacks. Same diffs.               ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
```

### e/for-by In Action (How Keyed Diffs Work Frame-by-Frame)

```
    ╔══════════════════════════════════════════════════════════════╗
    ║                  e/for-by IN ACTION                          ║
    ╠══════════════════════════════════════════════════════════════╣
    ║                                                              ║
    ║   (e/for-by :id [node visible-nodes]                         ║
    ║     (gpu/update-slot! (:id node) node)                       ║
    ║     (e/on-unmount #(gpu/free-slot! (:id node))))             ║
    ║                                                              ║
    ║   Frame 1: visible = [A B C D E]                             ║
    ║   ─────────────────────────────────                          ║
    ║   body runs 5 times (A, B, C, D, E)                          ║
    ║   5 GPU slots allocated                                      ║
    ║                                                              ║
    ║   Frame 2: visible = [A B C D E F G H]                       ║
    ║   ──────────────────────────────────────                     ║
    ║   A-E UNCHANGED → body does NOT re-run                       ║
    ║   F, G, H are NEW → body runs 3 times                        ║
    ║   3 GPU slots allocated                                      ║
    ║                                                              ║
    ║   Frame 3: visible = [B C D E F G H]                         ║
    ║   ──────────────────────────────────                         ║
    ║   A REMOVED → on-unmount fires → GPU slot freed              ║
    ║   B-H UNCHANGED → nothing happens                            ║
    ║   1 GPU slot freed                                           ║
    ║                                                              ║
    ║   TOTAL WORK ACROSS 3 FRAMES:                                ║
    ║   5 + 3 + 1 = 9 operations                                  ║
    ║                                                              ║
    ║   SNAPSHOT MODEL WOULD HAVE DONE:                            ║
    ║   5 + 8 + 7 = 20 rebuilds                                   ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
```

### The Zoom Problem Solved Differentially

```
    ZOOM: 50 visible → 200 visible (150 new nodes enter viewport)

    ╔═══════════════════════════════╦═══════════════════════════════╗
    ║     SNAPSHOT (current)        ║     DIFFERENTIAL (target)     ║
    ╠═══════════════════════════════╬═══════════════════════════════╣
    ║                               ║                               ║
    ║  recompute all 1000 nodes     ║  Electric knows: 150 entered  ║
    ║  filter to 200 visible        ║  emit: 150 × :add commands    ║
    ║  rebuild 200 GPU rects        ║  GPU: allocate 150 slots      ║
    ║  upload 200 rects             ║  upload 150 rects             ║
    ║                               ║                               ║
    ║  O(1000) compute              ║  O(150) compute               ║
    ║  O(200) upload                ║  O(150) upload                ║
    ║                               ║                               ║
    ╚═══════════════════════════════╩═══════════════════════════════╝
```

### Zoom Feedback Loop (Demand-Driven Data Loading)

```
    ╔══════════════════════════════════════════════════════════════╗
    ║              ZOOM FEEDBACK LOOP                              ║
    ╠══════════════════════════════════════════════════════════════╣
    ║                                                              ║
    ║   WebGPU reports viewport                                    ║
    ║        │                                                     ║
    ║        ▼                                                     ║
    ║   Electric knows visible region                              ║
    ║        │                                                     ║
    ║        ▼                                                     ║
    ║   Rama query: "nodes in this region at this zoom"            ║
    ║        │                                                     ║
    ║        ▼                                                     ║
    ║   PState returns matching nodes                              ║
    ║        │                                                     ║
    ║        ▼                                                     ║
    ║   Electric diffs against current scene                       ║
    ║        │                                                     ║
    ║        ▼                                                     ║
    ║   Mount callbacks: add new, remove old                       ║
    ║        │                                                     ║
    ║        ▼                                                     ║
    ║   GPU renders → user sees → zooms/pans → new viewport        ║
    ║        │                                                     ║
    ║        └──────────────────────────────────────────────┐      ║
    ║                                                       │      ║
    ║   ◄──────────────────────────────────────────────────┘      ║
    ║                                                              ║
    ║   Zoom out → viewport grows → more nodes needed              ║
    ║            → Rama loads more → Electric diffs → GPU adds     ║
    ║                                                              ║
    ║   Zoom in  → viewport shrinks → nodes leave                  ║
    ║            → Electric diffs → GPU removes                    ║
    ║                                                              ║
    ║   Pan      → viewport slides → 5 enter, 5 leave             ║
    ║            → Electric diffs → GPU adds 5, removes 5          ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
```

### No API Anywhere (Felix's Confirmation)

```
    ╔══════════════════════════════════════════════════════════════╗
    ║           ELECTRIC + RAMA = NO API                           ║
    ╠══════════════════════════════════════════════════════════════╣
    ║                                                              ║
    ║   CURRENT SOFTLAND:                                          ║
    ║   ─────────────────                                          ║
    ║                                                              ║
    ║   client ──HTTP POST──→ /api/agent/stream ──→ server         ║
    ║   client ←──SSE────────────────────────────── server         ║
    ║   client ──fetch()───→ /api/sidebar/* ──────→ server         ║
    ║   client ──fetch()───→ /api/file/* ─────────→ server         ║
    ║                                                              ║
    ║   Custom parsing. Manual plumbing. API endpoints.            ║
    ║                                                              ║
    ║   ─────────────────────────────────────────────────────────  ║
    ║                                                              ║
    ║   TARGET (Electric + Rama):                                  ║
    ║   ─────────────────────────                                  ║
    ║                                                              ║
    ║   (e/defn main []                                            ║
    ║     (e/server                                                ║
    ║       (let [trails (e/watch $$agent-trails)]                 ║
    ║         (e/client                                            ║
    ║           ;; trails is just HERE                              ║
    ║           ;; no fetch, no parse, no endpoint                 ║
    ║           ;; Electric transferred it automatically           ║
    ║           (render-trails trails)))))                          ║
    ║                                                              ║
    ║   Felix (Multiply, production):                              ║
    ║   "I haven't thought about an API in four months."           ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
```

---

## 3. The Migration Path

```
╔════════════════════════════════════╦════════════════════════════════════════╗
║           N O W                    ║         D I R E C T I O N              ║
║     (working scaffold)             ║      (where this points)               ║
╠════════════════════════════════════╬════════════════════════════════════════╣
║                                    ║                                        ║
║  atoms as ground truth             ║  Rama PStates as ground truth          ║
║  ┌──────────────────────────────┐  ║  ┌──────────────────────────────────┐  ║
║  │ mutable, local, no history  │  ║  │ event-sourced, distributed,     │  ║
║  │ no replay, ephemeral        │  ║  │ temporal, replayable            │  ║
║  └──────────────────────────────┘  ║  └──────────────────────────────────┘  ║
║                                    ║                                        ║
╠════════════════════════════════════╬════════════════════════════════════════╣
║                                    ║                                        ║
║  identical? snapshot-compare       ║  Electric e/for-by keyed diffs         ║
║  ┌──────────────────────────────┐  ║  ┌──────────────────────────────────┐  ║
║  │ O(total) compute when       │  ║  │ O(changed) compute,             │  ║
║  │ anything changes,           │  ║  │ O(changed) update,              │  ║
║  │ O(1) identity compare       │  ║  │ never touches unchanged nodes   │  ║
║  └──────────────────────────────┘  ║  └──────────────────────────────────┘  ║
║                                    ║                                        ║
╠════════════════════════════════════╬════════════════════════════════════════╣
║                                    ║                                        ║
║  Missionary RAF loop               ║  dirty-present strategy                ║
║  ┌──────────────────────────────┐  ║  ┌──────────────────────────────────┐  ║
║  │ 60Hz poll + identical? skip │  ║  │ react to Electric diff,          │  ║
║  │ runs continuously even when │  ║  │ request one RAF,                 │  ║
║  │ nothing changes              │  ║  │ present, done.                   │  ║
║  └──────────────────────────────┘  ║  └──────────────────────────────────┘  ║
║                                    ║                                        ║
╠════════════════════════════════════╬════════════════════════════════════════╣
║                                    ║                                        ║
║  manual server→client              ║  Electric e/server → e/client          ║
║  ┌──────────────────────────────┐  ║  ┌──────────────────────────────────┐  ║
║  │ HTTP fetch, custom SSE      │  ║  │ automatic websocket transfer,    │  ║
║  │ parsing, API endpoints      │  ║  │ no API code, no endpoints        │  ║
║  └──────────────────────────────┘  ║  └──────────────────────────────────┘  ║
║                                    ║                                        ║
╠════════════════════════════════════╬════════════════════════════════════════╣
║                                    ║                                        ║
║  one zoom level                    ║  continuous semantic zoom               ║
║  ┌──────────────────────────────┐  ║  ┌──────────────────────────────────┐  ║
║  │ code editor OR flow canvas  │  ║  │ Electric filters by viewport,    │  ║
║  │ mode switch, not zoom       │  ║  │ identity preserved across zoom   │  ║
║  └──────────────────────────────┘  ║  └──────────────────────────────────┘  ║
║                                    ║                                        ║
╚════════════════════════════════════╩════════════════════════════════════════╝

  ┌───────────────────────────────────────────────────────────────────────┐
  │  NOTE: the right column is DIRECTION, not proven architecture.       │
  │  The e/watch PState → client culling shape is inference.             │
  │  Whether server-side Rama spatial queries or client-side             │
  │  filtering is right for 1000+ nodes — open design question.         │
  └───────────────────────────────────────────────────────────────────────┘
```

---

## 4. Solved vs Unsolved

```
╔══════════════════════════════════════╦══════════════════════════════════════╗
║                                      ║                                      ║
║    SOLVED                            ║    UNSOLVED                          ║
║    (by Electric / Rama)              ║    (custom engineering)              ║
║                                      ║                                      ║
╠══════════════════════════════════════╬══════════════════════════════════════╣
║                                      ║                                      ║
║  ✓ Keyed incremental tracking        ║  ✗ GPU buffer pool allocator         ║
║    (e/diff-by, e/for-by)             ║    (slot management, free lists)     ║
║                                      ║                                      ║
║  ✓ Server→client transfer            ║  ✗ How diffs map to draw calls       ║
║    (Electric websocket)              ║    (scene reconciler)               ║
║                                      ║                                      ║
║  ✓ Lifecycle management              ║  ✗ Spatial index for viewport        ║
║    (mount / unmount)                 ║    (quadtree, spatial query)         ║
║                                      ║                                      ║
║  ✓ Event sourcing +                  ║  ✗ Text atlas for MSDF at zoom       ║
║    materialization                   ║    (glyph caching, LOD)             ║
║    (Rama depot → topology → PState)  ║                                      ║
║                                      ║  ✗ Batching strategy per zoom        ║
║  ✓ Backpressure                      ║    (layer sorting, instancing)       ║
║    (m/relieve)                       ║                                      ║
║                                      ║  ✗ Present pacing for WebGPU         ║
║  ✓ Reactive composition              ║    (dirty-present, v-sync align)    ║
║    (m/latest, e/for-by)             ║                                      ║
║                                      ║                                      ║
╠══════════════════════════════════════╬══════════════════════════════════════╣
║                                      ║                                      ║
║  ┌──────────────────────────────┐    ║  ┌──────────────────────────────┐    ║
║  │ The left column is          │    ║  │ The right column is          │    ║
║  │ POWERFUL.                   │    ║  │ REAL WORK.                   │    ║
║  │                              │    ║  │                              │    ║
║  │ Confusing "the conceptual   │    ║  │ The GPU integration is the   │    ║
║  │ framework is sound" with    │    ║  │ hard engineering problem     │    ║
║  │ "the engineering is         │    ║  │ that remains.                │    ║
║  │ trivial" is the mistake     │    ║  │                              │    ║
║  │ Codex caught me making.     │    ║  │                              │    ║
║  └──────────────────────────────┘    ║  └──────────────────────────────┘    ║
║                                      ║                                      ║
╚══════════════════════════════════════╩══════════════════════════════════════╝
```

---

## 5. Change → Proposal → Commitment

```
╔══════════════════════════════════════════════════════════════════════╗
║                                                                      ║
║            THE SEMANTIC CHAIN                                        ║
║                                                                      ║
║   ┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐   ║
║   │             │     │             │     │                     │   ║
║   │   CHANGE    │────→│  PROPOSAL   │────→│    COMMITMENT       │   ║
║   │             │     │             │     │                     │   ║
║   └──────┬──────┘     └──────┬──────┘     └──────────┬──────────┘   ║
║          │                   │                       │              ║
║          ▼                   ▼                       ▼              ║
║                                                                      ║
║    what Electric         what the              what Rama              ║
║    propagates            personal AI           stores                 ║
║                          produces                                    ║
║                                                                      ║
║    ┌──────────────┐  ┌────────────────┐  ┌────────────────────┐     ║
║    │ raw          │  │ interpreted    │  │ accepted change    │     ║
║    │ value change │  │ candidate     │  │ that's now part    │     ║
║    │              │  │ move          │  │ of world history   │     ║
║    │ keystroke    │  │              │  │                    │     ║
║    │ LLM chunk   │  │ can be       │  │ durable            │     ║
║    │ timer tick   │  │ accepted     │  │ replayable         │     ║
║    │ PState upd   │  │ or rejected  │  │ undoable as unit   │     ║
║    │              │  │              │  │ public             │     ║
║    └──────────────┘  └────────────────┘  └────────────────────┘     ║
║                                                                      ║
║    ──────────────── ──────────────────── ────────────────────────    ║
║    ephemeral         provisional           permanent                 ║
║    private           interpretable         public                    ║
║    no semantics      has meaning           world history             ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝
```

---

## 6. The Old Code: Evidence AND Warning

```
╔══════════════════════════════════════════════════════════════════════╗
║                                                                      ║
║       electric_flow_old.cljc — WHAT IT PROVES                        ║
║                                                                      ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                      ║
║   ✓  Electric + WebGPU WAS attempted                                 ║
║                                                                      ║
║      line 343:                                                       ║
║      ┌────────────────────────────────────────────────────────┐      ║
║      │ diff (e/input (e/pure                                 │      ║
║      │        (e/diff-by identity visible-rects)))           │      ║
║      └────────────────────────────────────────────────────────┘      ║
║      Electric tracking which rects entered/left viewport             ║
║                                                                      ║
║   ✓  The mount abstraction WAS used                                  ║
║                                                                      ║
║      lines 310-341:                                                  ║
║      ┌────────────────────────────────────────────────────────┐      ║
║      │ (mount                                                │      ║
║      │   (fn [el child]      (.push el child))      ;; ADD   │      ║
║      │   (fn [el child prev] (aset el idx child))   ;; REPL  │      ║
║      │   (fn [el child sib]  (.splice el idx 0 c))  ;; MOVE  │      ║
║      │   (fn [el child]      (.splice el idx 1))    ;; REM   │      ║
║      │   (fn [el i]          (aget el i)))          ;; NTH   │      ║
║      └────────────────────────────────────────────────────────┘      ║
║                                                                      ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                      ║
║   ✗  BUT the render path STILL flattened everything                  ║
║                                                                      ║
║      line 237:                                                       ║
║      ┌────────────────────────────────────────────────────────┐      ║
║      │ (let [rects-data (flatten                             │      ║
║      │        (into [] (vals all-rects)))]                    │      ║
║      │   (render-rect "zoom" rects-data ...))                │      ║
║      └────────────────────────────────────────────────────────┘      ║
║      Full flat array → render-rect. Not differential.                ║
║                                                                      ║
╠══════════════════════════════════════════════════════════════════════╣
║                                                                      ║
║   ┌────────────────────────────────────────────────────────────┐     ║
║   │                                                            │     ║
║   │  Differential TRACKING was there.                          │     ║
║   │  Differential RENDERING was not achieved.                  │     ║
║   │                                                            │     ║
║   │  The concept works.                                        │     ║
║   │  The full end-to-end was not built.                        │     ║
║   │                                                            │     ║
║   └────────────────────────────────────────────────────────────┘     ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝
```

---

## Grounding Notes

| Claim | Status |
|---|---|
| Electric's `mount` is generic (5 callbacks) | **KNOW** — read mount_impl.cljc source |
| `e/diff-by` produces structural diffs | **KNOW** — read incseq.cljc source |
| Old code used `e/diff-by` with WebGPU | **KNOW** — read electric_flow_old.cljc |
| Electric handles server→client transfer | **KNOW** — Felix talk (Multiply, production) |
| `e/for-by` only runs body for changed items | **KNOW** — read macro expansion |
| Electric can drive WebGPU without RAF loop | **INFER** — DOM model suggests it, not built for GPU |
| Electric scales to 1000+ nodes with smooth zoom | **INFER** — diff model is O(changed), overhead unmeasured |
| Commitment boundary architecture | **DESIGN** — converged concept, not implemented |
| Atoms → PStates migration path | **DESIGN** — direction clear, engineering substantial |
