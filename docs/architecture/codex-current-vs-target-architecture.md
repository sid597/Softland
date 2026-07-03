```text
╔════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                      SOFTLAND: ARCHITECTURE NOW                                  ║
╚════════════════════════════════════════════════════════════════════════════════════════════════════╝


                                    ┌──────────────────────────────┐
                                    │      electric_flow.cljc      │
                                    │    boot / init / handoff     │
                                    │    starts runtime loop       │
                                    └──────────────┬───────────────┘
                                                   │
                                                   ▼

┌────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                      MAIN APP RUNTIME (CLIENT)                                     │
│                                                                                                    │
│   ┌──────────────┐      ┌──────────────┐      ┌────────────────────┐      ┌──────────────────┐    │
│   │  DOM EVENTS  │ ───▶ │  events.cljs │ ───▶ │  HANDLER DISPATCH   │ ───▶ │  runtime/state   │    │
│   │ key/mouse/   │      │  raw flows    │      │ mouse / kbd / scroll│      │   ~30 atoms      │    │
│   │ paste/wheel  │      │                │      │ hit-test / routing  │      │ editor/chat/flow │    │
│   └──────────────┘      └──────────────┘      │ swap! / reset!      │      │ sidebar/focus... │    │
│                                               └──────────┬──────────┘      └────────┬─────────┘    │
│                                                          │                            │              │
│                                                          │                            │ m/watch      │
│                                                          │                            ▼              │
│                                               ┌──────────▼──────────┐      ┌──────────────────┐    │
│                                               │ combined_text.cljs   │      │ editor_compute   │    │
│                                               │ text projection      │      │ rect/shadow proj │    │
│                                               └──────────┬──────────┘      └────────┬─────────┘    │
│                                                          │                            │              │
│                                                          └──────────────┬─────────────┘              │
│                                                                         ▼                            │
│                                                           ┌──────────────────────────┐               │
│                                                           │     WORLD SNAPSHOT        │               │
│                                                           │ text + rects + shadows    │               │
│                                                           └────────────┬─────────────┘               │
│                                                                        │                             │
│                                                                        │ RAF loop                    │
│                                                                        ▼                             │
│                                                           ┌──────────────────────────┐               │
│                                                           │ render.cljs consumer      │               │
│                                                           │ identical? skip           │               │
│                                                           │ else rebuild/upload all   │               │
│                                                           └────────────┬─────────────┘               │
│                                                                        │                             │
│                                                                        ▼                             │
│                                                           ┌──────────────────────────┐               │
│                                                           │ WebGPU renderer.cljs      │               │
│                                                           │ draw-frame!               │               │
│                                                           └──────────────────────────┘               │
│                                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────────────────────┘


                              ┌─────────────────────────────────────────────────────┐
                              │            RAMA TODAY = PARTIAL SIDECAR            │
                              │ server/rama/core.clj exists                         │
                              │ depots / topologies / PStates exist                 │
                              │ but NOT the ground truth of the whole workspace     │
                              └─────────────────────────────────────────────────────┘
```


```text
╔════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                SOFTLAND: ARCHITECTURE IT SHOULD BE                                ║
╚════════════════════════════════════════════════════════════════════════════════════════════════════╝


┌──────────────┐      ┌──────────────────┐      ┌──────────────────────┐      ┌───────────────────┐
│  DOM EVENTS  │ ───▶ │ SIGNAL / PROPOSAL│ ───▶ │  COMMITMENT BOUNDARY │ ───▶ │    RAMA DEPOT     │
│ key/mouse/   │      │ target / context │      │ accept / reject /     │      │ append-only       │
│ paste/wheel  │      │ candidate moves  │      │ defer / confirm       │      │ committed actions │
└──────────────┘      └──────────────────┘      └──────────┬───────────┘      └─────────┬─────────┘
                                                            │                              │
                                                            │ committed semantic action    │
                                                            ▼                              ▼
                                                 ┌──────────────────────┐      ┌───────────────────┐
                                                 │ example:             │      │  RAMA TOPOLOGIES  │
                                                 │ insert char          │      │ materialize only  │
                                                 │ link evidence        │      │ new events        │
                                                 │ fork trail           │      └─────────┬─────────┘
                                                 │ undo edit burst      │                │
                                                 └──────────────────────┘                ▼
                                                                                 ┌───────────────────┐
                                                                                 │      PSTATES      │
                                                                                 │ committed world   │
                                                                                 │ local worlds      │
                                                                                 │ trails / lineage  │
                                                                                 └─────────┬─────────┘
                                                                                           │
                                                                                           │ e/watch / transfer
                                                                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                ELECTRIC DISTRIBUTED DERIVATION                                     │
│                                                                                                    │
│   e/server  ────────────────────────────────────────────────────────────────────────────── e/client │
│     watch/query PStates            differential propagation            derive visible worlds        │
│     no manual API plumbing         keyed lifecycle                     no snapshot rebuild          │
│                                                                                                    │
└───────────────────────────────────────────────────────────────┬────────────────────────────────────┘
                                                                │
                                                                ▼
                                                     ┌────────────────────────┐
                                                     │ LOCAL WORLDS / SCENE IR│
                                                     │ one shared derived     │
                                                     │ world for:             │
                                                     │ render / hit-test / nav│
                                                     └──────────┬─────────────┘
                                                                │
                                                                ▼
                                                     ┌────────────────────────┐
                                                     │ WEBGPU RECONCILER      │
                                                     │ add / update / remove  │
                                                     │ slot alloc / batching  │
                                                     │ patch buffers, present │
                                                     └──────────┬─────────────┘
                                                                │
                                                                ▼
                                                     ┌────────────────────────┐
                                                     │        DISPLAY         │
                                                     └────────────────────────┘


                     ┌────────────────────────────────────────────────────────────┐
                     │     EPHEMERAL LOCAL STATE STAYS LOCAL, NOT IN RAMA        │
                     │ hover / drag-preview / blink / GPU bookkeeping / dirty bit │
                     └────────────────────────────────────────────────────────────┘
```


```text
╔════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                             THE GAP                                               ║
╚════════════════════════════════════════════════════════════════════════════════════════════════════╝


        NOW                                                     NEEDED
        ───                                                     ──────

┌───────────────────────┐                           ┌──────────────────────────────────┐
│ event                 │                           │ event                            │
│ -> handler            │                           │ -> proposal                      │
│ -> mutate atoms       │                           │ -> commitment                    │
│                       │                           │ -> Rama action                   │
└──────────┬────────────┘                           └───────────────┬──────────────────┘
           │                                                        │
           ▼                                                        ▼
┌───────────────────────┐                           ┌──────────────────────────────────┐
│ atom lake =           │                           │ PStates = committed world truth  │
│ practical ground truth│                           │ durable / replayable / branchable│
│ no history            │                           │                                  │
└──────────┬────────────┘                           └───────────────┬──────────────────┘
           │                                                        │
           ▼                                                        ▼
┌───────────────────────┐                           ┌──────────────────────────────────┐
│ snapshot-ish derive   │                           │ keyed differential derivation    │
│ -> compare            │                           │ -> add/update/remove only        │
│ -> rebuild/upload     │                           │ -> scene reconcile               │
└──────────┬────────────┘                           └───────────────┬──────────────────┘
           │                                                        │
           ▼                                                        ▼
┌───────────────────────┐                           ┌──────────────────────────────────┐
│ WebGPU full-ish       │                           │ WebGPU patch/present terminal    │
│ rebuild sink          │                           │                                  │
└───────────────────────┘                           └──────────────────────────────────┘
```


```text
╔════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                       THE 3 CORE SHIFTS                                           ║
╚════════════════════════════════════════════════════════════════════════════════════════════════════╝


      1. COMMITMENT                            2. GROUND TRUTH                        3. DIFFERENTIAL SCENE
      ─────────────                            ────────────────                        ────────────────────

   from: handler decides                    from: atom lake                        from: snapshot -> rebuild
         and mutates directly                     in runtime/state                       -> upload all

   to:   proposal -> commitment            to:   Rama PStates                      to:   keyed diffs
         -> committed world move                 for committed state                     -> scene reconcile
                                                                                         -> patch/present


                    These 3 shifts take Softland from a reactive scaffold
                        to a Rama-committed, Electric-derived world system.
```
