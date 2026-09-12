# The Electric Way: Reactive-First Architecture

## What "Reactive-First" Actually Means

**Imperative-first** (what Codex built):
```
Events → Reducer → Mutate Atoms → Observe Atoms → Render
         ↑ (you write the orchestration logic)
```

**Reactive-first** (Electric way):
```
Sources (atoms/events) → Derived Flows → Derived Flows → Terminal Consumer
                         ↑ (the runtime handles propagation)
```

The difference: In reactive-first, you **declare what values depend on what**. You never write "when X changes, update Y" - that's implicit in the dependency graph.

---

## The Architecture From Scratch

Here's what this editor looks like built the Electric way:

### Layer 1: Primary Sources (The Only Atoms)

```clojure
;; These are the ONLY mutable sources in the entire system
(def !editor-doc (atom {:lines [""] :cursor {:line 0 :col 0} :selection nil :desired-col 0}))
(def !cmd-panel (atom {:text "" :cursor 0 :visible false}))
(def !focus (atom :editor))  ;; :editor | :command-panel
(def !scroll-y (atom 0))
(def !folded-lines (atom #{}))
(def !clipboard (atom nil))
```

Six atoms. That's it. Everything else is **derived**.

---

### Layer 2: Event Streams (Flows, Not Atoms)

```clojure
;; Keyboard events as a flow - produces values, doesn't store them
(defn >keyboard [node]
  (m/observe
    (fn [!]
      (let [handler (fn [e]
                      (.preventDefault e)
                      (! {:key (.-key e)
                          :ctrl (or (.-ctrlKey e) (.-metaKey e))
                          :shift (.-shiftKey e)}))]
        (.addEventListener node "keydown" handler)
        #(.removeEventListener node "keydown" handler)))))

;; Mouse, wheel, resize - same pattern
;; These are FLOWS, not atoms - they produce events that flow through the system
```

**★ Insight:** Events are **flows** (`m/observe`), not stored state. They flow through the system once and trigger derived updates. You never "read the current event" - you react to events as they arrive via `m/?<` or `m/?>`.

---

### Layer 3: Derived Flows (The Core of Reactive-First)

Here's where Electric thinking shines. Each derived value is a **flow** that automatically recomputes when its dependencies change:

```clojure
;; Line lengths - DERIVED from editor doc
(def <line-lengths
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))]
      (mapv count (:lines doc)))))

;; Fold regions - DERIVED from lines + line-lengths
(def <fold-regions
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          lengths (m/?< <line-lengths)]
      (detect-fold-regions (:lines doc) lengths))))

;; Line mapping (visual→logical) - DERIVED from folds + folded state
(def <line-mapping
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          regions (m/?< <fold-regions)
          folded (m/?< (m/watch !folded-lines))]
      (compute-line-mapping (count (:lines doc)) regions folded))))

;; Tokenized lines - DERIVED from lines
(def <tokenized-lines
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))]
      (mapv tokenize-line (:lines doc)))))

;; Editor render ops - DERIVED from tokens + layout params + folds
(def <editor-render-ops
  (m/ap
    (let [tokenized (m/?< <tokenized-lines)
          regions (m/?< <fold-regions)
          folded (m/?< (m/watch !folded-lines))]
      (layout-tokens tokenized layout-x layout-y font-size regions folded))))
```

Notice: **No `reset!` anywhere**. Each flow declares its dependencies, the runtime handles propagation.

---

### Layer 4: Event Routing (Focus as a Reactive Gate)

Instead of `if (= (:focus state) :command-panel)` in a reducer, focus **routes flows**:

```clojure
;; Keyboard events split by current focus
(def <editor-keys
  (m/ap
    (let [key-event (m/?< >keyboard)
          focus (m/?< (m/watch !focus))]
      (when (= focus :editor)
        key-event))))

(def <cmd-panel-keys
  (m/ap
    (let [key-event (m/?< >keyboard)
          focus (m/?< (m/watch !focus))]
      (when (= focus :command-panel)
        key-event))))
```

Now each component only sees **its own events**. No branching needed.

---

### Layer 5: State Updates (Atoms Updated by Flows)

Each component updates its own atom based on its own event flow:

```clojure
;; Editor state updates - consumes <editor-keys, updates !editor-doc
(def <editor-updates
  (m/ap
    (when-let [key-event (m/?< <editor-keys)]
      (swap! !editor-doc
        (fn [doc]
          (let [input {:lines (:lines doc)
                       :cursor (:cursor doc)
                       :selection (:selection doc)
                       :desired-col (:desired-col doc)}]
            (cond
              ;; Character input
              (and (= 1 (count (:key key-event)))
                   (not (:ctrl key-event)))
              (let [new-input (text-input/insert-char input (:key key-event) true)]
                (merge doc new-input))

              (= (:key key-event) "Backspace")
              (let [new-input (text-input/delete-backward input true)]
                (merge doc new-input))

              (= (:key key-event) "ArrowLeft")
              (let [new-input (text-input/move-cursor input :left true (mapv count (:lines doc)))]
                (merge doc new-input))

              ;; ... other keys
              :else doc)))))))

;; Command panel state updates - same pattern, updates !cmd-panel
(def <cmd-panel-updates
  (m/ap
    (when-let [key-event (m/?< <cmd-panel-keys)]
      (swap! !cmd-panel
        (fn [panel]
          (let [input {:text (:text panel) :cursor (:cursor panel)}]
            (cond
              (and (= 1 (count (:key key-event)))
                   (not (:ctrl key-event)))
              (let [new-input (text-input/insert-char input (:key key-event) false)]
                {:text (:text new-input)
                 :cursor (:cursor new-input)
                 :visible true})
              ;; ... other keys
              :else panel)))))))
```

**★ Insight:** Each component owns ONE atom and ONE update flow. The focus routing happens **before** events reach components, not inside a shared reducer. This is the Electric philosophy: **components are self-contained reactive units**.

---

### Layer 6: GPU State (Derived, Then Consumed)

The GPU state is derived from all the reactive flows, then consumed imperatively at the terminal:

```clojure
;; Editor rects - DERIVED from editor doc + fold state + line mapping
(def <editor-rects
  (m/ap
    (let [doc (m/?< (m/watch !editor-doc))
          mapping (m/?< <line-mapping)
          regions (m/?< <fold-regions)
          folded (m/?< (m/watch !folded-lines))
          blink (m/?< >blink-timer)]
      (let [cursor-rect (when (and blink (:cursor doc))
                          (calculate-caret-rect (:cursor doc) mapping))
            selection-rects (calculate-selection-rects (:selection doc) mapping)
            fold-rects (calculate-fold-indicators regions folded mapping)
            bracket-rects (calculate-bracket-match (:cursor doc) (:lines doc))]
        (vec (concat fold-rects bracket-rects selection-rects
                     (when cursor-rect [cursor-rect])))))))

;; Command panel rects - DERIVED from cmd-panel state
(def <cmd-rects
  (m/ap
    (let [panel (m/?< (m/watch !cmd-panel))
          focus (m/?< (m/watch !focus))
          blink (m/?< >blink-timer)
          scroll (m/?< (m/watch !scroll-y))
          height (m/?< <window-height)]
      (when (:visible panel)
        (let [panel-y (+ scroll (- height cmd-panel-h))]
          [{:type :background :x 0 :y panel-y :w width :h cmd-panel-h
            :r 0.15 :g 0.15 :b 0.2 :a 1.0}
           (when (and blink (= focus :command-panel))
             {:type :caret :x (+ 60 (* (:cursor panel) char-w)) :y (+ panel-y 8)
              :w 2 :h (- cmd-panel-h 16)
              :r 0.9 :g 0.9 :b 0.9 :a 1.0})])))))

;; Combined text geometry - DERIVED from render ops + cmd panel
(def <text-geometry
  (m/ap
    (let [editor-ops (m/?< <editor-render-ops)
          panel (m/?< (m/watch !cmd-panel))
          scroll (m/?< (m/watch !scroll-y))
          height (m/?< <window-height)]
      (let [cmd-ops (when (:visible panel)
                      (cmd-panel-text-ops panel scroll height))]
        (concat editor-ops cmd-ops)))))
```

---

### Layer 7: The Terminal (Imperative GPU Commands)

**Only here** do we have imperative code. The render loop consumes all derived flows:

```clojure
(defn start-render-loop! [device ctx geometry atlas]
  (m/reduce
    (fn [_ [frame-time text-ops editor-rects cmd-rects scroll-y width height]]
      ;; This is the ONLY imperative part - GPU commands
      (let [text-geo (editor/update-text-data device (:text geometry) text-ops atlas font-size)
            editor-rect-sys (editor/update-rects device (:rect geometry) editor-rects)
            cmd-rect-sys (editor/update-rects device (:cmd-rect geometry) (or cmd-rects []))]
        (editor/draw-frame! device ctx text-geo editor-rect-sys cmd-rect-sys
                            0 (- scroll-y) width height)))
    nil
    (m/latest vector
              >raf                           ;; Frame timing
              <text-geometry                 ;; Derived text ops
              <editor-rects                  ;; Derived editor rects
              <cmd-rects                     ;; Derived cmd rects
              (m/watch !scroll-y)            ;; Scroll position
              <window-width                  ;; Viewport
              <window-height)))
```

**★ Insight:** The render loop has **no logic**. It just consumes flows and issues GPU commands. All the "what should render" decisions happen in the derived flows above. This is the reactive-first pattern: **logic in flows, effects at terminals**.

---

## The Complete Flow Graph

```
                    ┌─────────────────┐
                    │   >keyboard     │ (event source)
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
              ▼                             ▼
    ┌─────────────────┐           ┌─────────────────┐
    │  <editor-keys   │           │ <cmd-panel-keys │ (focus-gated)
    └────────┬────────┘           └────────┬────────┘
             │                             │
             ▼                             ▼
    ┌─────────────────┐           ┌─────────────────┐
    │ <editor-updates │           │<cmd-panel-updates│ (state updaters)
    └────────┬────────┘           └────────┬────────┘
             │                             │
             ▼                             ▼
    ┌─────────────────┐           ┌─────────────────┐
    │  !editor-doc    │           │   !cmd-panel    │ (atoms - sources)
    └────────┬────────┘           └────────┬────────┘
             │                             │
    ┌────────┴────────┐           ┌────────┴────────┐
    │                 │           │                 │
    ▼                 ▼           ▼                 ▼
┌───────┐     ┌───────────┐  ┌─────────┐     ┌───────────┐
│<line- │     │<tokenized-│  │<cmd-text│     │<cmd-rects │
│lengths│     │  lines    │  │  ops    │     │           │
└───┬───┘     └─────┬─────┘  └────┬────┘     └─────┬─────┘
    │               │             │                 │
    ▼               ▼             │                 │
┌───────────────────────┐         │                 │
│   <editor-render-ops  │         │                 │
└───────────┬───────────┘         │                 │
            │                     │                 │
            ▼                     ▼                 ▼
    ┌───────────────────────────────────────────────────┐
    │              <text-geometry (combined)            │
    └───────────────────────┬───────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────────────────┐
    │         RENDER LOOP (terminal consumer)           │
    │    m/latest [>raf, <text-geo, <editor-rects, ...] │
    │         → GPU commands (imperative)               │
    └───────────────────────────────────────────────────┘
```

---

## Why This Is Better

| Aspect | Codex's Reducer | Reactive-First |
|--------|-----------------|----------------|
| **Adding new component** | Add branches to reducer for every event type | Add one atom + one update flow |
| **Debugging state** | Check 13 atoms, trace reducer branches | Follow flow graph, each atom has one updater |
| **Testing** | Mock atoms, events, verify side effects | Test flows in isolation, pure transformations |
| **Performance** | Re-runs entire reducer for any event | Only affected flows recompute |
| **Cognitive load** | "When X happens, what branches execute?" | "What does this value depend on?" |

---

## What Codex Built (The Problem)

### Monolithic Reducer (`loop.cljs:258-627`)

All events mixed into one stream, processed by one giant `case` statement:

```clojure
(->> (mx/mix
       window-metrics
       wheel-deltas
       >blink-timer
       mouse-events
       keyboard-events)
     (m/reduce
       (fn [_ [type value]]
         (swap! !state
                (fn [state]
                  (case type
                    :char-input
                    (if (= (:focus state) :command-panel)
                      ;; Command panel logic here
                      ;; Editor logic here)
                    :backspace
                    (if (= (:focus state) :command-panel)
                      ;; Command panel logic here
                      ;; Editor logic here)
                    ;; ... 20+ more branches, each with focus check
                    ))))))
```

### 13 Atoms (`loop.cljs:189-211`)

```clojure
!state, !editor-rect-sys, !cmd-rect-sys, !lines, !line-lengths,
!text-geo, !fold-regions, !line-mapping, !editor-line-count,
!clipboard, !undo-stack, !redo-stack, !editor-render-ops
```

Many of these should be **derived flows**, not atoms.

### Side Effects in Reducer

```clojure
(commit-editor-input (fn [state input new-input]
                       (let [old-lines @!lines  ;; Reading atom in reducer!
                             new-lines (:lines new-input)]
                         (reset! !lines new-lines)  ;; Writing atom in reducer!
                         (reset! !line-lengths ...)))) ;; More side effects!
```

---

## Ready to Rebuild

This architecture requires nuking the current `loop.cljs` and rewriting it as:

1. **Atom declarations** (6 primary sources)
2. **Event flows** (keyboard, mouse, wheel, resize, blink)
3. **Derived flows** (line-lengths, fold-regions, tokenized, render-ops, rects)
4. **Update flows** (editor-updates, cmd-panel-updates, focus-toggle)
5. **Render terminal** (single `m/reduce` consuming all derived flows)

The TextInputCore pure functions remain fully reusable - they're the right abstraction for the transformations inside update flows.
