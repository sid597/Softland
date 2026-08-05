# Insights - Electric/Missionary & WebGPU Editor

A collection of hard-won insights from building a reactive WebGPU editor.

---

## Missionary Flow Lifecycle

### Fresh Flows vs Shared Flows

```clojure
;; Works (re-subscribable), but shares nothing useful; factory preferred
(def >raf (m/observe ...))

;; GOOD: Factory function = fresh instance per subscription
(defn make-raf-flow [] (m/observe ...))
```

**Rule (corrected 2026-07-05, VERDICTS.md Claim 10):** `m/observe` and `m/ap` flows ARE re-subscribable — each subscription independently re-runs the observe setup fn, and all subscribers receive values. The genuine single-subscription/shared-process property belongs to `m/signal` / `m/stream` (memoized, multicast). Factory functions remain good practice, but their value is isolating per-subscription mutable state, not avoiding a subscription failure.

**Exception:** `m/watch` on atoms is fine as `def` because atoms persist independently.

---

### m/sample Argument Order

```clojure
;; WRONG - causes "Undefined continuous flow" error
(m/sample vector >raf <world-snapshot)

;; CORRECT - continuous first, discrete last
(m/sample vector <world-snapshot >raf)
```

**Signature:** `(m/sample f continuous-flow* discrete-trigger)`
- First args: flows being sampled (must be "initially ready")
- Last arg: discrete flow that triggers sampling

---

### m/join Cancellation Semantics

From Missionary docs:
> "If any task fails, others are cancelled then join fails with this error."

**Implication:** If ANY consumer in `m/join` fails or completes unexpectedly, ALL watches get cancelled. This produces "Watch cancelled" errors.

**Debugging tip:** If you see "Watch cancelled", one of your `m/join` branches is failing.

---

## WebGPU Game Loop Patterns

### Push vs Pull Model

**Push Model (Problematic for Game Loops):**
```clojure
;; Multiple independent consumers update GPU whenever data changes
(->> <text-ops (m/reduce (fn [_] (upload-to-gpu!))))
(->> <rects (m/reduce (fn [_] (upload-to-gpu!))))
(m/reduce (fn [_] (draw!)) nil >raf)
```

**Problems:**
- GPU uploads happen between frames (wasted work)
- Render loop might see half-updated state
- No guaranteed consistency per frame

**Pull Model (Correct for Game Loops):**
```clojure
;; All derived flows are PURE - no GPU side effects
<text-data (m/ap ...)
<rects (m/ap ...)

;; SINGLE terminal: sample on RAF, upload, draw
(m/reduce
  (fn [prev [world _frame]]
    (let [new-gpu-state (if (= world (:prev prev))
                          (:gpu prev)        ;; Reuse!
                          (upload! world))]  ;; Upload only if changed
      (draw! new-gpu-state)
      {:gpu new-gpu-state :prev world}))
  initial-state
  (m/sample vector <world-snapshot >raf))
```

**Benefits:**
- Consistent snapshot per frame
- No wasted GPU uploads
- Frame-synchronized updates

---

### Memoized Reducer for GPU Efficiency

**The Problem:** Naive pull model uploads to GPU 60x/sec even when idle.

**The Solution:** Compare with previous frame, skip upload if unchanged:

```clojure
(m/reduce
  (fn [prev-state [world _frame]]
    (let [text-changed? (not= (:text world) (:prev-text prev-state))

          ;; Reuse existing GPU buffer if unchanged
          text-sys (if text-changed?
                     (upload-text! (:text world))
                     (:text-sys prev-state))]

      ;; Always draw (cheap), conditionally upload (expensive)
      (draw! text-sys)

      {:text-sys text-sys
       :prev-text (:text world)}))
  initial-state
  (m/sample vector <world-snapshot >raf))
```

**Result:**
- Idle: 0 uploads/sec, 60 draws/sec
- Active typing: 1 upload/frame (batched), 60 draws/sec

---

## Reactive Architecture Principles

### The Electric/Missionary Philosophy

1. **Events are flows, not stored state**
   - `m/observe` wraps DOM events
   - Events flow through once, not stored in atoms

2. **Derived values are flows, not atoms**
   - `m/latest` over `m/watch` declares dependencies (never multiple `m/?<` nested in one `m/ap` — that crashes "Watch cancelled"; see CLAUDE.md ban)
   - Runtime handles propagation automatically

3. **Focus routing at flow level**
   - Events split BEFORE reaching components
   - No `if (= focus :panel)` inside reducers

4. **Logic in flows, effects at terminals**
   - Only `m/reduce` consumers have side effects
   - Everything else is pure transformation

---

### 7-Layer Architecture

```
Layer 1: PRIMARY SOURCES (atoms)
    !editor-doc, !cmd-panel, !focus, !scroll-y, !viewport, !folded-lines

Layer 2: EVENT FLOWS
    >keyboard, >mouse, >wheel, >resize, >blink-timer, >raf

Layer 3: DERIVED FLOWS (pure)
    <line-lengths, <fold-regions, <tokenized, <line-mapping

Layer 4: FOCUS-BASED ROUTING
    <global-keys, <editor-keys, <cmd-panel-keys

Layer 5: COMPONENT UPDATES
    Each component owns one atom, one update flow

Layer 6: GPU STATE DERIVED (pure data)
    <text-data, <editor-rects, <cmd-rects

Layer 7: TERMINAL CONSUMER (Pull model)
    m/sample on RAF → upload if changed → draw
```

---

### Imperative Trap to Avoid

**BAD: Push logic disguised as reactive:**
```clojure
(m/reduce
  (fn [state event]
    (case (:type event)
      :char (if (= (:focus state) :panel)  ;; Manual routing!
              (update-panel ...)
              (update-editor ...))))
  initial-state
  event-stream)
```

**GOOD: Route at flow level:**
```clojure
;; Events pre-filtered by focus
<editor-events (m/ap
  (let [event (m/?< events)
        focus (m/?< (m/watch !focus))]
    (when (= focus :editor) event)))

;; Editor reducer only sees editor events
(->> <editor-events
     (m/reduce update-editor initial-state))
```

---

## Multi-Agent Collaboration

From Session 8, different AI agents contributed different strengths:

| Agent | Contribution |
|-------|--------------|
| **Codex** | Fast implementation of pure functions (TextInputCore) |
| **Claude** | Architectural vision and refactoring |
| **Gemini** | Domain-specific critique (WebGPU performance) |

**Lesson:** Multiple reviews catch different issues. Codex built fast, Claude structured it, Gemini optimized for the domain.

---

## Debugging Tips

### "Undefined continuous flow"
- Check `m/sample` argument order (continuous first, discrete last)
- Ensure all inputs to `m/latest` are continuous flows
- Verify `m/ap` flows with `m/?<` on `m/watch` produce immediately

### "Watch cancelled" / "Reactor failure" - THE REAL FIX (Session 10)

**The Problem:**

The error `Reactor failure: missionary.Cancelled {message: 'Watch cancelled.'}` happens when multiple `m/?<` forks over `m/watch` are nested inside ONE `m/ap`. It crashes even when that flow is consumed directly — feeding it to `m/latest` is NOT required (verified 2026-07-05, VERDICTS.md Claim 1 shape A).

**Root Cause - m/ap Fork Cancellation:**

```clojure
;; BAD PATTERN - m/ap with multiple m/?< forks
(defn <derived-data [!atom1 !atom2 !atom3]
  (m/ap
    (let [a (m/?< (m/watch !atom1))
          b (m/?< (m/watch !atom2))
          c (m/?< (m/watch !atom3))]  ;; ← Each m/?< can trigger cancellation!
      (compute a b c))))
```

**What happens:**
1. `m/ap` with `m/?<` creates an "ambiguous process"
2. When ANY input changes, the old computation branch is **CANCELLED**
3. A new branch starts with the updated value
4. The outer fork's restart cancels the still-live NESTED `(m/watch ...)` fork — cancelling a live `m/watch` throws "Watch cancelled". This crashes even standalone (consumed directly, no `m/latest` — verified on build -45).

**m/latest is NOT the trigger (corrected 2026-07-05) — it only spreads the failure:**

```clojure
;; m/latest propagates cancellation from ANY input flow
<world-snapshot (m/latest
                  (fn [a b c] {...})
                  <derived-flow-using-m/ap   ;; ← If this cancels, EVERYTHING dies
                  (m/watch !other)
                  (m/watch !another))
```

From Missionary docs: *"If any flow fails or is cancelled, the resulting flow fails or is cancelled as well."*

Propagation through `m/latest` spreads the failure wider, but it is not the cause: the same `m/ap` crashes consumed directly, and two SEPARATE single-`m/?<` `m/ap`s fed to `m/latest` do NOT crash (VERDICTS.md Claim 1, shapes A and B2).

**THE FIX - Use m/latest instead of m/ap:**

```clojure
;; GOOD PATTERN - m/latest is continuous, never cancels
(defn <derived-data [!atom1 !atom2 !atom3]
  (m/latest
    (fn [a b c]
      (compute a b c))
    (m/watch !atom1)
    (m/watch !atom2)
    (m/watch !atom3)))
```

**Why m/latest works:**
- `m/latest` combines continuous flows into a continuous flow
- `m/watch` on atoms are inherently continuous
- No cancellation during value propagation
- Always maintains the latest combined value

**Before vs After:**

| Pattern | Flow Type | On Input Change | Safe in m/latest? |
|---------|-----------|-----------------|-------------------|
| `m/ap` + `m/?<` | Ambiguous | Cancel old branch, start new | ❌ NO |
| `m/latest` + `m/watch` | Continuous | Recompute with new values | ✅ YES |

**Debugging Timeline (Session 10):**

```
1. UI appears briefly, then disappears after ~530ms
2. Logs show: "Blink update: true" → "Blink update: false" → CRASH
3. Blink timer changes !caret-visible every 530ms
4. <editor-rects uses m/ap with m/?< on !caret-visible
5. When blink fires, m/ap cancels → m/latest propagates → reactor dies
```

**The Fix Applied:**

Changed all derived flows from `m/ap` to `m/latest`:
- `<editor-rects`
- `<cmd-panel-rects`
- `<combined-text-ops`

**Also Fixed: Event Filtering Flows**

For discrete event filtering (not combining continuous values), use `m/eduction` with `deref`:

```clojure
;; BAD - m/ap forks on focus change, causing cancellation
(defn <editor-keys [>keyboard !focus]
  (m/ap
    (let [event (m/?< >keyboard)
          focus (m/?< (m/watch !focus))]  ;; ← FORK = CANCEL
      (when (= focus :editor) event))))

;; GOOD - m/eduction with deref, no forking
(defn <editor-keys [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :editor)  ;; ← DEREF = NO CANCEL
                                  (not (:global? event))))))))
```

**Key Difference:**
- `m/?< (m/watch !atom)` = Fork on every atom change → cancellation
- `@!atom` inside filter = Read current value, no fork, no cancellation

Result: Render loop now runs stably at 60fps indefinitely.

**Electric 3 Limitation:**

`try/catch` is NOT supported inside `e/defn` bodies:
```clojure
;; This will fail with "try is TODO"
(e/defn MyComponent []
  (try
    (e/Task ...)
    (catch :default e ...)))  ;; ❌ NOT ALLOWED
```

Error handling in Electric must be done outside the reactive context.

### Text appears then disappears
- A consumer's flow got cancelled upstream (check `m/join` siblings — one dying branch cancels all). NOT an `m/observe` re-subscription limit: m/observe/m/ap are re-subscribable (VERDICTS.md Claim 10)
- RAF loop stopped (cleanup function ran)
- Check `m/observe` cleanup is not running prematurely

---

---

## Text Rendering & Cursor Alignment

### The 0.6 Factor Must Be Consistent Everywhere

**The Problem:** Cursor position drifts from actual text when typing.

**Root Cause:** Character width assumptions differed between systems:

```clojure
;; Cursor calculation (loop.cljs)
char-w (* font-size 0.6)  ;; = 9.6px for all chars

;; Text rendering (editor.cljs) - BEFORE FIX
(= ch \space) (swap! !x + (* fsize 0.25))     ;; Spaces = 4px!
advance (* fsize (or (:advance g) 0))          ;; Regular = font metrics
```

**The Fix:** Force monospace rendering with fixed 0.6 advance:

```clojure
;; Text rendering (editor.cljs) - AFTER FIX
(= ch \space) (swap! !x + (* fsize 0.6))       ;; Spaces = 9.6px
advance (* fsize 0.6)                           ;; Regular = 9.6px
```

**Key Principle:** When cursor and text rendering are separate systems, they MUST agree on character widths. Either:
1. Use font metrics everywhere (cursor reads glyph advances)
2. Use fixed width everywhere (force monospace)

We chose #2 because the cursor already assumed fixed width.

---

*Last updated: Session 10 - m/ap vs m/latest Fix, Character Width Consistency*

---

## Horizontal Scroll — Fixed vs Scrolling Coordinate Systems

### The Problem: Global Coordinate Contamination

When adding horizontal scroll, the temptation is to redefine a shared coordinate at the top of a `let` block:

```clojure
;; ❌ BAD — contaminates ALL downstream code
(let [layout-x (- layout-x scroll-x)  ;; redefine!
      ...]
  ;; Now line numbers, gutter, right-pane text ALL use the shifted value)
```

**Symptoms:** Line numbers scroll off-screen, text bleeds into sidebar, fold indicators drift, right-pane content shifts.

### The Fix: Separate Coordinate Variables

```clojure
;; ✅ GOOD — explicit scrolled vs unscrolled
(let [layout-x (snap layout-x)              ;; unscrolled (gutter, line-nums)
      editor-lx (snap (- layout-x scroll-x)) ;; scrolled (editor text only)
      ...]
  ;; layout-fn uses editor-lx, line-number code uses layout-x)
```

### Clipping: Both Sides Required

Text must be clipped at the **gutter edge** (not x=0), because the gutter is a fixed column:

```clojure
;; clip-sub: left = gutter right edge, right = editor pane width
(when (and (< x code-w) (> text-end layout-x))
  ;; Left-trim: skip chars before gutter edge
  (let [skip (if (< x layout-x) (ceil (/ (- layout-x x) cw)) 0)]
    ...))
```

### Gutter Independence for Rects

`compute-editor-rects` takes `:gutter-lx` kwarg — fold indicators and gutter rects use the unscrolled position, while caret/selection/brackets use the scrolled position.

**Key Principle:** In a single-canvas editor, any per-element transform (h-scroll, zoom) must NEVER redefine a shared variable. Create a new scoped variable and use it only where the transform applies.

---

## Session 12: Theme Switching & Syntax Highlighting

### Architecture Evolution
The architecture evolution is significant:
1. **From 13 atoms → 6 primary sources** - Derived values are now flows, not stored atoms
2. **From single 600+ line reducer → Focus-based routing** - Events split BEFORE reaching components
3. **Critical Missionary lesson**: multiple `m/?<` forks nested in one `m/ap` over watches crash ("Watch cancelled") on input change — even consumed directly; `m/latest` is not the trigger. Use `m/latest` for combining continuous values, `m/eduction` + `deref` for filtering discrete events.

---

### The 0.56 Factor
The `0.56` factor is critical! It's the normalized glyph advance for Ubuntu Sans Mono. This value must be identical in:
- `loop.cljs`: cursor positioning (`char-w (* font-size 0.56)`)
- `editor.cljs`: text shaping (`advance (* fsize char-width)`)

If they differ, the cursor will drift from the text position as you type!

---

### Avoiding Circular Dependencies in ClojureScript
When you have a call graph like `A → B → A`:
1. **Extract the shared code** to a new namespace `C`
2. Have both `A` and `B` require `C`
3. Result: `A → C ← B` (no cycle)

In this case, themes were the shared data that both `electric-flow` (for layout-tokens) and `loop` (for UI/settings) needed.

---

### Why MSDF Over Bitmap Fonts
1. **Resolution independence** - Same atlas works at 8px or 80px
2. **Crisp edges** - Distance field gives perfect anti-aliasing
3. **Small texture** - One 512x512 atlas vs many bitmap sizes
4. **GPU-friendly** - Simple fragment shader, no CPU rasterization

The `pxRange` parameter (8 in this project) controls the distance field resolution - higher = sharper edges but larger atlas.

---

### Why Per-Instance Colors is "Reactive-Friendly"
Your architecture already computes colors reactively in `layout-tokens`. The colors flow through the system - they just get thrown away at the GPU boundary.

The fix is essentially: **"stop throwing away the colors"** - upload them with each glyph.

This means when `!settings :theme-id` changes:
1. `m/latest` triggers re-computation
2. `layout-tokens` produces tokens with new colors
3. `update-text-data` uploads new colors to GPU
4. Next frame renders with new theme

No extra work needed in the reactive layer - just plumbing the colors through to the GPU!

---

### Zed-Style GPU Text Rendering
What we built: A modern GPU text renderer matching Zed's architecture:
- Single draw call for all text (efficient batching)
- Per-glyph colors (syntax highlighting)
- Reactive updates (theme changes flow through automatically)

The key insight is that colors were already being computed - they just weren't reaching the GPU. Now the full pipeline is connected!

---

*Last updated: Session 12 - Theme Switching & Per-Instance GPU Colors*
