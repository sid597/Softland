# Plan: Merge Old Electric UI System with New WebGPU Editor

## Overview
Merge the reactive Electric/Missionary patterns from `electric_flow_old.cljc` with the new WebGPU editor in `electric_flow.cljc` + `loop.cljs` to create a unified UI system.

---

## Part 1: Analysis of Old Electric Patterns (with Documentation References)

### 1.1 `e/declare` - Forward Declarations
**Location:** `electric_flow_old.cljc:45-67`
```clojure
(e/declare canvas)
(e/declare device)
(e/declare offset)
```

**From Docs (`electric3.cljc:120-123`):**
> `declare` takes one or more symbols to declare. Creates Electric-aware reactive symbols that will be bound later via `binding`.

**Purpose:** Pre-declare reactive variables that get bound in the main function's `binding` block. Unlike Clojure's `declare`, these become reactive placeholders.

---

### 1.2 `e/defn` vs `defn` - Reactive Functions
**Location:** `electric_flow_old.cljc:119-227`

**From Tutorial (`electri_tutorial.txt:47-48`):**
> `e/defn` defines an Electric function. Electric functions are reactive at the granularity of individual expressions (as opposed to React, which is reactive at function granularity).

**Key Difference:**
| Regular `defn` | Electric `e/defn` |
|----------------|-------------------|
| Runs once | Re-runs when dependencies change |
| No reactivity | Tracks all `e/watch`, `e/snapshot` deps |
| Pure Clojure | Can contain `e/client`, `e/server` |

**Example from old code:**
```clojure
(e/defn Render-text [cx cy zf]   ;; Re-runs when cx, cy, or zf change
  (let [dv (e/snapshot device)]   ;; Captures device once
    ...))
```

---

### 1.3 `e/client` / `e/server` - Site Boundaries
**Location:** `electric_flow_old.cljc:120, 289`

**From Tutorial (`electri_tutorial.txt:439-446`):**
> `e/server` blocks run on the backend (JVM), `e/client` blocks run on the frontend (browser). Network transfer occurs at site boundaries.

**From Docs (`electric3.cljc:194-195`):**
```clojure
(defmacro client [& body] `(check-electric client (::lang/site :client ~@body)))
(defmacro server [& body] `(check-electric server (::lang/site :server ~@body)))
```

**Old code pattern:**
```clojure
(e/defn Setup-webgpu []
  (e/client                        ;; All WebGPU code runs client-side
    (let [adapter (e/Task (await-promise (.requestAdapter gpu)))]
      ...)))
```

---

### 1.4 `e/watch` vs `e/snapshot` - Reactive vs One-time
**Location:** `electric_flow_old.cljc:370-389` (watch), `130-132` (snapshot)

**From Tutorial (`electri_tutorial.txt:138-144`):**
> `e/watch` is how you subscribe to the current value from an atom, as it changes over time. It uses `add-watch` and `remove-watch` APIs.

**From Docs (`electric3.cljc:146-152`):**
```clojure
(defmacro watch [ref]
  `(check-electric watch (input (m/watch (watchable! ~ref)))))
```

**`e/snapshot` (`electric3.cljc:404-408`):**
> Snapshots the first non-Pending value of reactive value `x` and freezes it, inhibiting all further reactive updates.

**When to use which:**
| `e/watch` | `e/snapshot` |
|-----------|--------------|
| Continuous updates needed | One-time capture |
| UI bindings that change | GPU resources (stable) |
| Mouse position, scroll | Device, context, format |

**Old code example:**
```clojure
(binding [offset (e/watch !offset)           ;; Continuous - pan changes
          zoom-factor (e/watch !zoom-factor)] ;; Continuous - zoom changes
  ...
  (let [dv (e/snapshot device)]              ;; One-time - GPU device is stable
    ...))
```

---

### 1.5 `e/Token` - Transaction State Machine
**Location:** `electric_flow_old.cljc:238`

**From Tutorial (`electri_tutorial.txt:601-606`):**
> `(e/Token event)` constructs a tuple `[t err]` for each successive transactional intent. `t` starts nil (ready), becomes non-nil (busy), and can be "spent" exactly once to return to ready.

**From Docs (`electric3.cljc:481-485`):**
```clojure
(hyperfiddle.electric3/defn Token
  ([v] (Token v some?))
  ([v on?] (let [!x (atom [nil nil])]
             (step !x v on?)
             (watch !x))))
```

**Old code usage:**
```clojure
(let [[s e] (e/Token offset)]   ;; Track lifecycle of offset changes
  ...)
```

---

### 1.6 `e/Task` - Async Promise Integration
**Location:** `electric_flow_old.cljc:125-126, 417`

**From Docs (`electric3.cljc:553-555`):**
```clojure
(hyperfiddle.electric3/defn Task
  ([t] (join (task->incseq t)))
  ([t init-v] (input (initialized t init-v))))
```

**Purpose:** Wraps async operations (Promises) into Electric's reactive system.

**Old code pattern:**
```clojure
(let [adapter (e/Task (await-promise (.requestAdapter gpu)))
      device (e/Task (await-promise (.requestDevice adapter)))]
  ...)
```

---

### 1.7 Missionary Integration Patterns

#### `m/observe` - DOM Event Wrapping
**Location:** `shapes/rect.cljc:50-64`

**From Missionary Docs (`missionary-complete-reference.txt:95-105`):**
> A task is a value representing an action to be performed. `m/observe` creates a task from a callback-based API.

```clojure
(defn el-mouse-move-state> [movable id dragging?]
  (m/observe
    (fn [!]                                    ;; ! is the emit function
      (let [handler (fn [e] (! {:cords [(.-clientX e) (.-clientY e)]}))]
        (.addEventListener movable "mousemove" handler)
        #(.removeEventListener movable "mousemove" handler)))))  ;; cleanup
```

#### `m/reductions` - State Accumulation
**From Missionary Docs (`missionary-complete-reference.txt:297-307`):**
> `reduce` reduces flows like collections, turning it into a task.

```clojure
(->> events
     (m/reductions {} {:cord [0 0] :time 0})  ;; Initial state, accumulate
     ...)
```

#### `m/relieve` - Deduplication
**From Missionary Docs:** Filters consecutive duplicate values.

```clojure
(->> flow
     (m/relieve {})   ;; Drop duplicates
     ...)
```

#### `m/latest` - Extract + Side Effect
```clojure
(->> flow
     (m/latest (fn [x] (reset! !global-atom x)))  ;; Side effect on latest
     ...)
```

#### `m/signal` - Bridge to Electric
```clojure
(defn global-client-flow []
  (m/signal                          ;; Convert to Electric Signal
    (m/latest identity
      (m/watch !global-atom))))
```

---

## Part 2: Architecture Comparison

### Old System (SVG + Electric-DOM)
```
┌─────────────────────────────────────────────────────────┐
│  Electric Main (e/defn main)                            │
│  ├── binding [...] establishes reactive context         │
│  ├── Canvas-view → dom/canvas element                   │
│  ├── Add-panning → dom/On "mousemove"                   │
│  ├── Add-wheel → dom/On "wheel"                         │
│  └── Render-* → WebGPU calls                            │
│                                                         │
│  State: !offset, !zoom-factor, !device, etc. (atoms)    │
│  Events: !global-atom event bus                         │
│  Sync: Rama server via e/server blocks                  │
└─────────────────────────────────────────────────────────┘
```

### New System (WebGPU + Missionary)
```
┌─────────────────────────────────────────────────────────┐
│  Electric Main (e/defn main)                            │
│  └── dom/canvas → loop/start-loop!                      │
│                                                         │
│  Missionary Event Loop (loop.cljs)                      │
│  ├── m/join {} (parallel streams)                       │
│  │   ├── State reducer (events → state)                 │
│  │   ├── Text updater (lines → GPU)                     │
│  │   ├── Selection updater (cursor → rects)             │
│  │   └── Render loop (RAF → draw)                       │
│                                                         │
│  State: !state, !lines, !line-lengths (atoms in loop)   │
│  Events: keyboard, mouse, wheel via m/observe           │
└─────────────────────────────────────────────────────────┘
```

### Gap Analysis
| Feature | Old System | New System | Needed |
|---------|------------|------------|--------|
| Pan/Zoom | Add-panning, Add-wheel | Only scroll-y | Merge |
| Mouse drag | Full coords | Selection only | Keep new |
| Server sync | Rama integration | None | Add back |
| Global events | !global-atom bus | Local state | Add |
| Multi-region | Single canvas | Single editor | Build |
| Text input | None | Full editor | Extend |

---

## Part 3: Merge Strategy

### Phase 1: Extract Reusable Patterns from Old
1. **Event bus pattern** - `!global-atom` + `global-client-flow`
2. **Throttled mouse flow** - `el-mouse-move-state<`
3. **Server update flow** - `server-update`
4. **Pan/zoom calculations** - from `Add-panning`, `Add-wheel`

### Phase 2: Create Region/Layout System
```clojure
(def regions
  {:editor       {:y 0 :h (- screen-h 40)}
   :command-bar  {:y (- screen-h 40) :h 40}})

;; Each region has own state
(def !region-state
  (atom {:editor       {:lines [...] :cursor {...} :scroll-y 0}
         :command-bar  {:lines [""] :cursor {:line 0 :col 0}}}))

;; Focus determines keyboard routing
(def !active-region (atom :editor))
```

### Phase 3: Unified Rendering
```clojure
;; Combine all regions into single render call
(defn render-all-regions [device ctx regions state]
  (let [all-text-ops (mapcat (fn [[region-id region-state]]
                               (layout-region region-id region-state))
                             state)
        all-rects (concat editor-rects command-bar-rects)]
    (update-text-data device text-sys all-text-ops atlas font-size)
    (update-rects device rect-sys all-rects)
    (draw-frame! ...)))
```

### Phase 4: Keyboard Routing
```clojure
(cond
  ;; Global shortcuts always work
  (and ctrl? (= key "k")) (toggle-command-panel!)

  ;; Route to active region
  (= @!active-region :command-bar)
  (handle-command-input! event)

  :else
  (handle-editor-input! event))
```

---

## Part 4: Implementation Steps

### Step 1: Add Global Event Bus (from old)
**File:** `global_flow.cljs` (extend existing)
```clojure
(defonce !ui-event (atom nil))
(defn ui-event-flow [] (m/signal (m/latest identity (m/watch !ui-event))))
```

### Step 2: Add Region State to loop.cljs
**File:** `loop.cljs`
- Add `!command-panel` state atom
- Add `:command-visible?` to `!state`
- Add command panel text/cursor state

### Step 3: Modify Keyboard Handler
**File:** `loop.cljs`
- Add Ctrl+K toggle
- Add focus switching logic
- Route events based on active region

### Step 4: Extend Rendering for Multi-Region
**File:** `loop.cljs` + `electric_flow.cljc`
- Calculate layout regions based on screen size
- Render command panel background rect
- Render command panel text
- Handle command panel input events

### Step 5: Wire Up to Electric Entry Point
**File:** `electric_flow.cljc`
- Use `e/watch` for command panel visibility
- Consider bringing back `e/declare` + `binding` pattern for cleaner state

---

## Part 5: Key Files to Modify

1. **`/mnt/data/projects/Softland/src/app/client/webgpu/loop.cljs`**
   - Add command panel state
   - Add keyboard routing
   - Add region-based layout

2. **`/mnt/data/projects/Softland/src/app/electric_flow.cljc`**
   - Pass region config to loop
   - Consider Electric-level state for server sync later

3. **`/mnt/data/projects/Softland/src/global_flow.cljs`**
   - Add UI event bus if needed for cross-component communication

---

## Questions Before Implementation

1. **Should command panel have its own undo/redo stack?** Or share with editor?

2. **Server sync priority:** Should we add Rama integration now, or focus on UI first?

3. **Typing in command panel:** Should Enter submit and close, or allow multi-line?

4. **Pan/zoom for editor region:** Do you want to bring back the old pan/zoom from `electric_flow_old.cljc`?
