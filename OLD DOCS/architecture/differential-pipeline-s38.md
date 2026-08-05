# The Differential Pipeline — Session 38

> **Date:** 2026-03-15
> **What this is:** The concrete architectural insight from Session 38. Every layer operates on changes, never on full state.
> **Origin:** Started from "should interactions be pull-based?" → discovered the differential principle is the real answer.

---

## The Core Principle

Every layer should operate on changes, never on full state.

```
    SNAPSHOT MODEL (current)              DIFFERENTIAL MODEL (vision)
    ════════════════════════              ══════════════════════════

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

---

## With 1000 Nodes and Zoom

```
    ZOOM: 50 visible → 200 visible (150 new nodes enter viewport)

    ╔═══════════════════════════════╦═══════════════════════════════╗
    ║     SNAPSHOT (current)        ║     DIFFERENTIAL (vision)     ║
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

---

## The Diff Compounds Across Layers

Not just one layer diffing — EVERY layer receives diffs and emits diffs:

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

## Electric's Diff Structure (From Actual Source)

The `e/diff-by` produces structural diffs with five operations:

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

---

## Electric's Generic Mount (From Actual Source: mount_impl.cljc)

Five callbacks. NOT DOM-specific. Works for ANY target:

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

---

## The Old Code Already Did This (Partially)

`electric_flow_old.cljc` — proof the concept was attempted:

```
    ╔══════════════════════════════════════════════════════════════╗
    ║              OLD CODE: ELECTRIC + WEBGPU                     ║
    ╠══════════════════════════════════════════════════════════════╣
    ║                                                              ║
    ║   line 343:                                                  ║
    ║   diff (e/input (e/pure (e/diff-by identity visible-rects))) ║
    ║                          ▲                                   ║
    ║                          │                                   ║
    ║              Electric tracking which rects                   ║
    ║              entered/left the viewport                       ║
    ║                                                              ║
    ║   lines 310-341:                                             ║
    ║   (mount                                                     ║
    ║     (fn [el child]      (.push el child))        ;; ADD      ║
    ║     (fn [el child prev] (aset el idx child))     ;; REPLACE  ║
    ║     (fn [el child sib]  (.splice el idx 0 child));; MOVE     ║
    ║     (fn [el child]      (.splice el idx 1))      ;; REMOVE   ║
    ║     (fn [el i]          (aget el i)))            ;; LOOKUP   ║
    ║                                                              ║
    ║   ┌──────────────────────────────────────────────────────┐   ║
    ║   │  BUT: the render path still flattened everything:    │   ║
    ║   │                                                      │   ║
    ║   │  line 237:                                           │   ║
    ║   │  (let [rects-data (flatten (into [] (vals all-rects))│   ║
    ║   │    (render-rect "zoom" rects-data ...))              │   ║
    ║   │                                                      │   ║
    ║   │  Differential TRACKING was there.                    │   ║
    ║   │  Differential RENDERING was not achieved.            │   ║
    ║   └──────────────────────────────────────────────────────┘   ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
```

---

## What e/for-by Actually Does

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

---

## WebGPU Contract (Dumb Renderer)

```
    ╔══════════════════════════════════════════════════════════════╗
    ║                    WEBGPU CONTRACT                           ║
    ╠══════════════════════════════════════════════════════════════╣
    ║                                                              ║
    ║   IN (from Electric):           OUT (to Electric):           ║
    ║   ───────────────────           ──────────────────           ║
    ║                                                              ║
    ║   {:add    [{:id 42             {:viewport {:x 0 :y 0        ║
    ║              :rect [x y w h]                :w 1920           ║
    ║              :color [r g b a]               :h 1080           ║
    ║              :text "defn"                   :zoom 2.5}        ║
    ║              :layer :code}]                                   ║
    ║                                  :visible-ids #{42 43 ...}   ║
    ║    :remove [17 23]                                           ║
    ║                                  :hit-test {:id 42           ║
    ║    :update [{:id 42                         :local [12 3]}}  ║
    ║              :color [1 0 0 1]}]                              ║
    ║                                                              ║
    ║    :camera {:x 0 :y -300                                     ║
    ║             :zoom 2.5}}                                      ║
    ║                                                              ║
    ║   ┌──────────────────────────────────────────────────────┐   ║
    ║   │  WebGPU knows NOTHING about:                         │   ║
    ║   │  editors, sidebars, cursors, command panels,         │   ║
    ║   │  discourse graphs, trails, local worlds              │   ║
    ║   │                                                      │   ║
    ║   │  It knows about:                                     │   ║
    ║   │  rects, text, layers, camera, colors                 │   ║
    ║   │                                                      │   ║
    ║   │  ALL SEMANTICS live in Electric.                     │   ║
    ║   │  ZERO SEMANTICS live in WebGPU.                      │   ║
    ║   └──────────────────────────────────────────────────────┘   ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
```

---

## Demand-Driven Data Loading (Zoom Feedback Loop)

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

---

## No API Anywhere (Felix's Confirmation)

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
    ║   VISION (Electric + Rama):                                  ║
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

## Architecture Now vs Should Be (Full Diagrams)

### Now

```
╔══════════════════════════════════════════════════════════════════╗
║                        DOM EVENTS                               ║
║            (keydown, mousedown, paste, wheel, resize)           ║
╚════════════════════════════╤═════════════════════════════════════╝
                             │
                             ▼
╔══════════════════════════════════════════════════════════════════╗
║                    HANDLER DISPATCH TREE                         ║
║                                                                  ║
║   ┌─────────┐  ┌──────────┐  ┌─────────┐  ┌──────────────────┐ ║
║   │ global  │  │ editor   │  │  cmd    │  │  chat / settings │ ║
║   │  keys   │  │  keys    │  │  keys   │  │      keys        │ ║
║   └────┬────┘  └────┬─────┘  └────┬────┘  └────────┬─────────┘ ║
║        │            │             │                 │           ║
║   ┌────┴────────────┴─────────────┴─────────────────┴────┐      ║
║   │  500+ lines of case/cond branching logic             │      ║
║   │  hit-testing, coordinate conversion, fold toggling   │      ║
║   │  ALL computation happens here in the push path       │      ║
║   └──────────────────────┬───────────────────────────────┘      ║
╚══════════════════════════╤═══════════════════════════════════════╝
                           │
                           │  reset! / swap!
                           ▼
╔══════════════════════════════════════════════════════════════════╗
║                     ~30 MUTABLE ATOMS                            ║
║                                                                  ║
║   !editor-doc    !focus       !viewport     !scroll-y            ║
║   !cmd-panel     !settings    !active-font  !current-file        ║
║   !flow-state    !agent-out   !sidebar-st   !caret-visible       ║
║   !drag-start    !dragging?   !clipboard    !undo-stack          ║
║   !chat-input    !trail-coll  !active-pane  !eval-result  ...    ║
║                                                                  ║
║   ┌──────────────────────────────────────────────────────────┐   ║
║   │  NO HISTORY  ·  NO REPLAY  ·  NO BRANCHING              │   ║
║   │  NO EVENT LOG  ·  LOCAL ONLY  ·  EPHEMERAL               │   ║
║   └──────────────────────────────────────────────────────────┘   ║
╚══════════════════════════╤═══════════════════════════════════════╝
                           │
                           │  m/watch
                           ▼
╔══════════════════════════════════════════════════════════════════╗
║                 MISSIONARY DERIVED FLOWS                         ║
║                                                                  ║
║   <text-data ─────────────────────────┐                          ║
║   (tokenize, wrap, cull)              │                          ║
║                                       │                          ║
║   <editor-rects ──────────────────────┤                          ║
║   (selection, cursor, brackets)       ├───→  m/latest            ║
║                                       │      (world snapshot)    ║
║   <cmd-rects ─────────────────────────┤                          ║
║   <settings-rects ────────────────────┘                          ║
║                                                                  ║
╚══════════════════════════╤═══════════════════════════════════════╝
                           │
                           │  m/sample (RAF 60Hz)
                           ▼
╔══════════════════════════════════════════════════════════════════╗
║                    RENDER CONSUMER                               ║
║                                                                  ║
║   ┌──────────────────────────────────────────────────────────┐   ║
║   │  if (identical? world prev-world)                        │   ║
║   │      → skip                          ← MOST FRAMES       │   ║
║   │  else                                                    │   ║
║   │      → rebuild ALL text ops          ← O(total)          │   ║
║   │      → rebuild ALL rect buffers      ← O(total)          │   ║
║   │      → rebuild ALL shadow buffers    ← O(total)          │   ║
║   │      → upload ALL to GPU             ← O(total)          │   ║
║   │      → draw-frame! (9 layers)                            │   ║
║   └──────────────────────────────────────────────────────────┘   ║
║                                                                  ║
╚══════════════════════════╤═══════════════════════════════════════╝
                           │
                           ▼
                     ┌───────────┐
                     │  DISPLAY  │
                     └───────────┘
```

### Should Be

```
╔══════════════════════════════════════════════════════════════════╗
║                        DOM EVENTS                                ║
║             (keydown, mousedown, paste, wheel, resize)           ║
╚════════════════════════════╤═════════════════════════════════════╝
                             │
                             ▼
╔══════════════════════════════════════════════════════════════════╗
║                    THIN SIGNAL EMITTERS                          ║
║                                                                  ║
║   ┌──────────────────────────────────────────────────────────┐   ║
║   │  Capture raw gesture, emit to proposal pipeline          │   ║
║   │  NO hit-testing · NO coordinate conversion · NO logic    │   ║
║   │  Just: {:type :mousedown :x 342 :y 187 :time 1710...}   │   ║
║   └──────────────────────────┬───────────────────────────────┘   ║
╚══════════════════════════════╤═══════════════════════════════════╝
                               │
                               ▼
╔══════════════════════════════════════════════════════════════════╗
║               PROPOSAL / COMMITMENT BOUNDARY                     ║
║                                                                  ║
║   signal                                                         ║
║     │                                                            ║
║     ▼                                                            ║
║   interpretation ◄──── context (what's visible, what's focused)  ║
║     │             ◄──── personal AI (learned patterns, optional)  ║
║     ▼                                                            ║
║   candidate move                                                 ║
║     │                                                            ║
║     ▼                                                            ║
║   ┌────────────────────────────────────────────┐                 ║
║   │  COMMIT?                                   │                 ║
║   │                                            │                 ║
║   │  well-known actions → auto-commit          │                 ║
║   │  (typing, clicking, scrolling)             │                 ║
║   │                                            │                 ║
║   │  ambiguous proposals → hold for review     │                 ║
║   │  (AI suggestions, large refactors)         │                 ║
║   └─────────────────────┬──────────────────────┘                 ║
║                         │                                        ║
║                committed semantic action                         ║
║                 "insert char 'a' at line 5 col 3"                ║
║                 "link evidence E to claim C"                     ║
║                 "fork trail at step 7"                           ║
║                 "undo last edit burst"                            ║
╚═════════════════════════╤════════════════════════════════════════╝
                          │
                          │  append event
                          ▼
╔══════════════════════════════════════════════════════════════════╗
║                       RAMA DEPOT                                 ║
║                  (append-only, causal DAG)                        ║
║                                                                  ║
║   ┌──────────────────────────────────────────────────────────┐   ║
║   │  DURABLE  ·  REPLAYABLE  ·  BRANCHABLE  ·  QUERYABLE    │   ║
║   │  every committed action is permanent world history       │   ║
║   │  undo = append reversal event, not delete                │   ║
║   └──────────────────────────────────────────────────────────┘   ║
║                                                                  ║
║                          │                                       ║
║                          ▼                                       ║
║                    ┌───────────┐                                 ║
║                    │ TOPOLOGY  │  (incremental materialization)   ║
║                    │           │  processes new events only       ║
║                    │  fragment │  emits to shaped PStates         ║
║                    │    DSL    │                                  ║
║                    └─────┬─────┘                                 ║
║                          │                                       ║
║                          ▼                                       ║
║            ┌──────────────────────────────┐                      ║
║            │         P S T A T E S        │                      ║
║            │                              │                      ║
║            │  $$editor-state              │                      ║
║            │  $$discourse-graph           │                      ║
║            │  $$agent-trails              │                      ║
║            │  $$local-worlds              │                      ║
║            │  $$lineage                   │                      ║
║            │                              │                      ║
║            │  shaped · queryable          │                      ║
║            │  temporal · subscribable     │                      ║
║            └──────────────┬───────────────┘                      ║
╚═══════════════════════════╤══════════════════════════════════════╝
                            │
                            │  Electric e/watch (automatic)
                            │  server → client via websocket
                            │  NO API PLUMBING
                            ▼
╔══════════════════════════════════════════════════════════════════╗
║                  ELECTRIC DIFFERENTIAL LAYER                     ║
║                                                                  ║
║   e/server                                                       ║
║     │  (let [nodes (e/watch $$pstate)]                           ║
║     │                                                            ║
║     │    e/client                                                ║
║     │      │  (let [visible (viewport-cull nodes camera)]        ║
║     │      │                                                     ║
║     │      │    (e/for-by :id [node visible]                     ║
║     │      │                                                     ║
║     │      │      ;; ONLY runs for ADDED/CHANGED nodes           ║
║     │      │      ;; AUTOMATICALLY cleans up REMOVED nodes       ║
║     │      │      ;; O(changed), never O(total)                  ║
║     │      │                                                     ║
║     │      │      (gpu/update-slot! (:id node) node)             ║
║     │      │      (e/on-unmount                                  ║
║     │      │        #(gpu/free-slot! (:id node)))))              ║
║                                                                  ║
╚═══════════════════════════╤══════════════════════════════════════╝
                            │
                            │  mount callbacks
                            │  {:add [...] :remove [...] :update [...]}
                            ▼
╔══════════════════════════════════════════════════════════════════╗
║                   WEBGPU SCENE RECONCILER                        ║
║                     (custom engineering)                         ║
║                                                                  ║
║   ┌──────────────────────────────────────────────────────────┐   ║
║   │  buffer pool         slot allocator     text atlas       │   ║
║   │  draw batching       layer sorting      z-ordering       │   ║
║   │  camera uniforms     viewport culling   LOD              │   ║
║   └──────────────────────────────────────────────────────────┘   ║
║                                                                  ║
║   on scene dirty:                                                ║
║     request ONE RAF → submit command encoder → present → done    ║
║     (not a loop — a one-shot sync with display v-sync)           ║
║                                                                  ║
╚═══════════════════════════╤══════════════════════════════════════╝
                            │
                            ▼
                      ┌───────────┐
                      │  DISPLAY  │
                      └───────────┘
```

---

## The Three Gaps That Unlock Everything Else

```
    ┌──────────────────────────────────────────────────────────┐
    │                                                          │
    │   1. COMMITMENT BOUNDARY                                 │
    │      ─────────────────────                               │
    │      Define what counts as a world move.                 │
    │      Currently: every handler mutates atoms directly.    │
    │      Needed: signal → proposal → commitment pipeline.    │
    │      This defines undo, history, and public form.        │
    │                                                          │
    │                          │                               │
    │                          ▼                               │
    │                                                          │
    │   2. ATOMS → PSTATES                                     │
    │      ─────────────────────                               │
    │      Migrate ground truth from mutable atoms             │
    │      to Rama event-sourced PStates.                      │
    │      Committed actions → depot → topology → PState.      │
    │      This enables history, replay, branching.            │
    │                                                          │
    │                          │                               │
    │                          ▼                               │
    │                                                          │
    │   3. SNAPSHOT → DIFFERENTIAL                             │
    │      ─────────────────────────                           │
    │      Replace identical? rebuild-everything               │
    │      with Electric e/for-by keyed diffs.                 │
    │      PState → e/watch → e/for-by → mount callbacks.     │
    │      This enables 1000-node zoom.                        │
    │                                                          │
    │                          │                               │
    │                          ▼                               │
    │                                                          │
    │   Everything else follows:                               │
    │   render pacing, semantic zoom, multiple minds,          │
    │   explicit world objects, personal AI.                   │
    │                                                          │
    └──────────────────────────────────────────────────────────┘
```

---

## What the Original Question Actually Answered

Started: "Should we make the interaction layer pull-based too?"

The push/pull question dissolved:
1. **Electric handles propagation** — no manual push/pull distinction needed
2. **The commitment boundary replaces handler dispatch** — not push vs pull, but proposal vs commitment
3. **RAF is sink-specific detail** — dirty-present replaces continuous loop
4. **The real question was ontological** — not "how should data flow?" but "what are the objects of this world, and what does a change mean in it?"

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
