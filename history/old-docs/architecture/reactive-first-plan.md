# Reactive-First Architecture Implementation Plan

## Executive Summary

This plan implements the architecture suggested in `electric_architecture_review.md`. We're transforming the current **imperative-first** design (events → reducer → mutate atoms → observe → render) into a **reactive-first** design (sources → derived flows → terminal consumers).

The key insight: **You declare what values depend on what. The runtime handles propagation.**

---

## The Problem with Codex's Implementation

### Current Architecture (Imperative-First)

```
Events → Single Monolithic Reducer → Mutates 13 Atoms → Observers → Render
         ↑ (you write the orchestration logic)
```

**Issues:**
1. **13 atoms** where only 6 are true sources (the rest are derived)
2. **Single giant reducer** (600+ lines) with `if (= (:focus state) :command-panel)` branches everywhere
3. **Side effects inside reducer** - calling `reset!` on atoms within `swap!`
4. **Computed values stored as atoms** - `!line-lengths`, `!fold-regions`, `!line-mapping` should be derived flows

### Target Architecture (Reactive-First)

```
Sources (6 atoms) → Derived Flows → Derived Flows → Terminal Consumer
                    ↑ (the runtime handles propagation)
```

**Benefits:**
1. Each component owns **one atom** and **one update flow**
2. No branching for focus - routing happens at flow level
3. Derived values are **flows**, not atoms
4. Logic in flows, **effects only at terminals**

---

## The Electric/Missionary Way

### Key Patterns from Documentation

| Pattern | Purpose | Reference |
|---------|---------|-----------|
| `m/watch` | Continuous subscription to atom | Creates flow that emits on each atom change |
| `m/observe` | Wrap imperative events as flow | DOM events → Missionary flow |
| `m/ap` / `m/?<` | Compose flows reactively | Automatic dependency tracking |
| `m/latest` | Combine multiple flows | Sample latest values from all sources |
| `m/reduce` | Terminal consumer | The only place for side effects |
| `m/eduction` | Transform flow values | Like transducers for flows |
| `mx/mix` | Merge event streams | Multiple sources → single stream |

### The Reactive Principle

**Never write: "when X changes, update Y"**

Instead, declare: "Y depends on X" - the runtime figures out propagation.

```clojure
;; IMPERATIVE (wrong):
(add-watch !lines :update-lengths
  (fn [_ _ _ new-lines]
    (reset! !line-lengths (mapv count new-lines))))

;; REACTIVE (correct):
(def <line-lengths
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))]
      (mapv count (:lines doc)))))
```

---

## Implementation Layers

### Layer 1: Primary Sources (6 Atoms)

These are the **only mutable sources** in the system:

```clojure
;; Document state - the actual data being edited
(def !editor-doc
  (atom {:lines [""]
         :cursor {:line 0 :col 0}
         :selection nil
         :desired-col 0}))

;; Command panel state - separate document
(def !cmd-panel
  (atom {:text ""
         :cursor 0
         :visible false}))

;; Focus - which component receives keyboard input
(def !focus (atom :editor))  ;; :editor | :command-panel

;; Viewport state
(def !scroll-y (atom 0))
(def !viewport (atom {:width 0 :height 0 :dpr 1}))

;; Editor visual state
(def !folded-lines (atom #{}))
```

**Why only 6?** Everything else is **derived** from these:
- `line-lengths` → derived from `!editor-doc :lines`
- `fold-regions` → derived from `!editor-doc :lines`
- `line-mapping` → derived from fold-regions + `!folded-lines`
- `render-ops` → derived from tokenized lines + layout params

### Layer 2: Event Flows

Events are **flows** that produce values, not atoms that store values:

```clojure
;; Keyboard events as flow
(defn >keyboard [node]
  (m/observe
    (fn [!]
      (let [handler (fn [e]
                      (.preventDefault e)
                      (! (parse-key-event e)))]
        (.addEventListener node "keydown" handler)
        #(.removeEventListener node "keydown" handler)))))

;; Parse into semantic event
(defn parse-key-event [e]
  (let [key (.-key e)
        ctrl? (or (.-ctrlKey e) (.-metaKey e))]
    (cond
      (and ctrl? (= key "k")) {:type :toggle-command-panel}
      (and ctrl? (= key "Enter")) {:type :eval}
      (= key "Escape") {:type :escape}
      (= key "Backspace") {:type :backspace}
      (= key "Enter") {:type :enter}
      (and (= 1 (count key)) (not ctrl?)) {:type :char :char key}
      ;; ... etc
      )))
```

**Key insight:** Events flow through the system **once** and trigger updates. You never "read the current event" from an atom.

### Layer 3: Derived Flows

Each derived value is a flow that **automatically recomputes** when dependencies change:

```clojure
;; Line lengths - derived from editor doc
(def <line-lengths
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))]
      (mapv count (:lines doc)))))

;; Fold regions - derived from lines
(defn <fold-regions [detect-folds-fn]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          lengths (m/?< <line-lengths)]
      (detect-folds-fn (:lines doc) lengths))))

;; Line mapping (visual→logical) - derived from folds + folded state
(defn <line-mapping [detect-folds-fn]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          regions (m/?< (<fold-regions detect-folds-fn))
          folded (m/?< (m/watch !folded-lines))]
      (compute-line-mapping (count (:lines doc)) regions folded))))

;; Tokenized lines - derived from lines
(defn <tokenized-lines [tokenize-fn]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))]
      (mapv tokenize-fn (:lines doc)))))
```

**Notice:** No `reset!` anywhere. Each flow declares dependencies, runtime handles propagation.

### Layer 4: Focus-Based Event Routing

Instead of branching inside a reducer, **route events at the flow level**:

```clojure
;; Keyboard events split by current focus
(defn <editor-keys [>keyboard]
  (m/ap
    (let [event (m/?< >keyboard)
          focus (m/?< (m/watch !focus))]
      (when (= focus :editor)
        event))))

(defn <cmd-panel-keys [>keyboard]
  (m/ap
    (let [event (m/?< >keyboard)
          focus (m/?< (m/watch !focus))]
      (when (= focus :command-panel)
        event))))
```

**Result:** Each component only sees **its own events**. No branching needed in update logic.

### Layer 5: Component Update Flows

Each component updates its own atom based on its filtered event flow:

```clojure
;; Editor state updates - consumes <editor-keys, updates !editor-doc
(defn <editor-updates [<editor-keys save-undo!]
  (m/ap
    (when-let [event (m/?< <editor-keys)]
      (swap! !editor-doc
        (fn [doc]
          (let [input {:lines (:lines doc)
                       :cursor (:cursor doc)
                       :selection (:selection doc)
                       :desired-col (:desired-col doc)}]
            (case (:type event)
              :char
              (let [new-input (text-input/insert-char input (:char event) true)]
                (save-undo! doc)
                (merge doc new-input {:selection nil}))

              :backspace
              (let [new-input (text-input/delete-backward input true)]
                (save-undo! doc)
                (merge doc new-input {:selection nil}))

              :left (merge doc (text-input/move-cursor input :left true (mapv count (:lines doc))))
              :right (merge doc (text-input/move-cursor input :right true (mapv count (:lines doc))))
              ;; ... other cases
              doc)))))))

;; Command panel updates - same pattern
(defn <cmd-panel-updates [<cmd-panel-keys]
  (m/ap
    (when-let [event (m/?< <cmd-panel-keys)]
      (swap! !cmd-panel
        (fn [panel]
          (let [input {:text (:text panel) :cursor (:cursor panel)}]
            (case (:type event)
              :char
              (let [new-input (text-input/insert-char input (:char event) false)]
                (merge panel new-input))
              ;; ... other cases
              panel)))))))
```

**Key insight:** Each component owns ONE atom and ONE update flow. Focus routing happens **before** events reach components.

### Layer 6: GPU State (Derived Flows)

All rendering data is derived, then consumed imperatively at the terminal:

```clojure
;; Editor rects - derived from doc + folds + blink
(defn <editor-rects [<line-mapping detect-folds-fn find-bracket-fn >blink-timer font-size layout-x layout-y line-h]
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          mapping (m/?< (<line-mapping detect-folds-fn))
          regions (m/?< (<fold-regions detect-folds-fn))
          folded (m/?< (m/watch !folded-lines))
          blink (m/?< >blink-timer)
          lengths (m/?< <line-lengths)]
      (let [cursor-rect (when (and blink (:cursor doc) (not (:selection doc)))
                          (calculate-caret-rect (:cursor doc) mapping font-size layout-x layout-y line-h))
            selection-rects (when (:selection doc)
                              (calculate-selection-rects (:selection doc) mapping lengths font-size layout-x layout-y line-h))
            fold-rects (calculate-fold-indicators regions folded mapping font-size layout-x layout-y line-h)
            bracket-rects (calculate-bracket-match (:cursor doc) (:lines doc) lengths find-bracket-fn font-size layout-x layout-y line-h)]
        (vec (concat fold-rects bracket-rects (or selection-rects [])
                     (when cursor-rect [cursor-rect])))))))

;; Command panel rects - derived from panel state
(defn <cmd-rects [>blink-timer font-size cmd-panel-h]
  (m/ap
    (let [panel (m/?< (m/watch !cmd-panel))
          focus (m/?< (m/watch !focus))
          blink (m/?< >blink-timer)
          viewport (m/?< (m/watch !viewport))
          scroll (m/?< (m/watch !scroll-y))]
      (when (:visible panel)
        (let [panel-y (+ scroll (- (:height viewport) cmd-panel-h))]
          [(make-cmd-bg-rect panel-y (:width viewport) cmd-panel-h)
           (when (and blink (= focus :command-panel))
             (make-cmd-caret-rect (:cursor panel) panel-y font-size cmd-panel-h))])))))

;; Combined text geometry - derived from render ops + cmd panel
(defn <text-geometry [<editor-render-ops font-size cmd-panel-h]
  (m/ap
    (let [editor-ops (m/?< <editor-render-ops)
          panel (m/?< (m/watch !cmd-panel))
          viewport (m/?< (m/watch !viewport))
          scroll (m/?< (m/watch !scroll-y))]
      (if (:visible panel)
        (let [cmd-ops (make-cmd-text-ops panel scroll (:height viewport) cmd-panel-h font-size)]
          (vec (concat editor-ops cmd-ops)))
        editor-ops))))
```

### Layer 7: Terminal Consumer (Render Loop) - "Pull" Model

**CRITICAL FOR WEBGPU:** The render loop uses a "pull" model, not "push".

**The Problem with Push:**
```clojure
;; WRONG: Multiple independent consumers updating GPU state
(->> <text-ops (m/reduce (fn [_] (upload-to-gpu! ...))))
(->> <editor-rects (m/reduce (fn [_] (upload-to-gpu! ...))))
(->> >raf (m/reduce (fn [_] (draw! @gpu-state))))
```
This causes: wasted GPU uploads between frames, potential tearing, inconsistent snapshots.

**The Pull Model (Correct):**
```clojure
;; Derived flows are PURE - compute data, no GPU side effects
<text-data (m/ap ...)      ;; Returns render ops, not GPU buffers
<editor-rects (m/ap ...)   ;; Returns rect data, not GPU buffers
<cmd-rects (m/ap ...)      ;; Returns rect data, not GPU buffers

;; Combine into world snapshot
<world-snapshot (m/latest
                  (fn [text editor cmd viewport scroll]
                    {:text text :editor editor :cmd cmd :viewport viewport :scroll scroll})
                  <text-data <editor-rects <cmd-rects
                  (m/watch !viewport) (m/watch !scroll-y))

;; SINGLE terminal: sample on RAF, upload, draw
(m/reduce
  (fn [prev-state [_frame world]]
    ;; Only upload if data changed (structural comparison)
    (let [new-text-geo (if (= (:text world) (:prev-text prev-state))
                         (:text-geo prev-state)
                         (upload-text-to-gpu! (:text world)))]
      ;; Draw frame
      (draw-frame! new-text-geo ...)
      ;; Return state for next frame comparison
      {:text-geo new-text-geo :prev-text (:text world)}))
  initial-state
  (m/sample vector >raf <world-snapshot))
```

**Benefits:**
1. **Consistent snapshot** - All data from same logical moment
2. **No wasted uploads** - Only upload when drawing (and only if changed)
3. **Frame-synchronized** - GPU state changes aligned with vsync
4. **Single point of contact** - All GPU operations in one place

---

## Implementation Strategy

### Phase 1: Extract Event Stream Flows
- Move keyboard event parsing into pure flow
- Create `>keyboard`, `>mouse`, `>wheel` flows
- Keep current reducer temporarily

### Phase 2: Add Primary Source Atoms
- Create `!editor-doc`, `!cmd-panel` as unified document atoms
- Migrate state from `!state` to these atoms
- Keep derived atoms temporarily

### Phase 3: Replace Derived Atoms with Flows
- Create `<line-lengths`, `<fold-regions`, `<line-mapping` as flows
- Remove `!line-lengths`, `!fold-regions`, `!line-mapping` atoms
- Update consumers to use flows

### Phase 4: Implement Focus-Based Routing
- Create `<editor-keys`, `<cmd-panel-keys` routed flows
- Remove `if (= (:focus state) :command-panel)` branches
- Each component gets its own update flow

### Phase 5: Refactor Render Pipeline
- Create derived rect flows
- Create combined text geometry flow
- Simplify terminal consumer

### Phase 6: Cleanup
- Remove unused atoms
- Remove old reducer branches
- Test all functionality

---

## Files to Modify

| File | Changes |
|------|---------|
| `loop.cljs` | Complete rewrite following reactive patterns |
| `editor.cljs` | Minor cleanup (draw function already good) |
| `electric_flow.cljc` | Update `start-loop!` call signature |

---

## Testing Checklist

- [ ] Keyboard input works in editor
- [ ] Keyboard input works in command panel
- [ ] Focus switches correctly with Ctrl+K
- [ ] Escape closes command panel
- [ ] Caret blinks in focused component only
- [ ] Selection rendering works
- [ ] Code folding works
- [ ] Bracket matching works
- [ ] SCI evaluation works
- [ ] Undo/redo works
- [ ] Copy/cut/paste works
- [ ] Scroll works
- [ ] Window resize works

---

## Why This Matters

| Aspect | Codex's Reducer | Reactive-First |
|--------|-----------------|----------------|
| **Adding new component** | Add branches to reducer for every event type | Add one atom + one update flow |
| **Debugging state** | Check 13 atoms, trace reducer branches | Follow flow graph, each atom has one updater |
| **Testing** | Mock atoms, events, verify side effects | Test flows in isolation, pure transformations |
| **Performance** | Re-runs entire reducer for any event | Only affected flows recompute |
| **Cognitive load** | "When X happens, what branches execute?" | "What does this value depend on?" |

This is the **Electric way** - declare dependencies, let the runtime propagate changes.
