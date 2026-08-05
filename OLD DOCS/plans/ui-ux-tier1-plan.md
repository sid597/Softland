# Plan: Tier 1 UI/UX Fixes

## Context

The command panel + agent output flow has 4 broken behaviors:
1. Caret renders inside the `[CLAUDE]>` prompt instead of after it (text-x mismatch between caret flow and text flow)
2. Agent output text has no background rect — green text overlaps editor code, unreadable
3. Pressing Enter hides the panel with no visual replacement — agent status invisible without logs
4. Panel sometimes loses focus while typing (focus race condition)

These are all in `loop.cljs` except the agent background rendering order which also touches `editor.cljs:draw-frame!`.

---

## Fix 1: Command Panel Cursor Alignment

**Root cause**: `text-x` is hardcoded to `60` in two places, but `<combined-text-ops` calculates it dynamically as `24 + (count "[CLAUDE]> ") * char-advance`.

**Changes in `loop.cljs`:**

### 1a. Add shared helper (near line 379, before `<combined-text-ops`)
```clojure
(defn cmd-prompt-text [provider]
  (str "[" (-> (or provider :claude) name str/upper-case) "]> "))

(defn cmd-text-start-x [provider font-size char-width dpr snap?]
  (let [prompt (cmd-prompt-text provider)
        char-advance (maybe-snap (* font-size char-width) dpr snap?)
        prompt-x (maybe-snap 24 dpr snap?)]
    (maybe-snap (+ prompt-x (* (count prompt) char-advance)) dpr snap?)))
```

### 1b. Update `<cmd-panel-rects` (line 620)
- Add `!ai-provider` as input parameter
- Replace `text-x (maybe-snap 60 dpr snap?)` with:
  ```clojure
  text-x (cmd-text-start-x @!ai-provider font-size (:char-width active-font) dpr snap?)
  ```
  Note: use `@!ai-provider` (deref, not watch) since this is inside `m/latest` fn body and we don't want an extra watch source — provider changes rarely.

### 1c. Update `<combined-text-ops` (lines 1001-1004)
- Replace inline prompt-text/prompt-x/text-x calculation with:
  ```clojure
  prompt-text (cmd-prompt-text provider)
  prompt-x (maybe-snap 24 dpr snap?)
  text-x (cmd-text-start-x provider font-size (:char-width active-font) dpr snap?)
  ```

### 1d. Update mouse click handler (line 1717)
- Replace `text-x (maybe-snap 60 dpr snap?)` with:
  ```clojure
  text-x (cmd-text-start-x @!ai-provider font-size char-width dpr snap?)
  ```

---

## Fix 2: Agent Output Background Rect

**Root cause**: Agent text is rendered in `<combined-text-ops` as text-only, no background rect exists anywhere.

**Approach**: Extend `<cmd-panel-rects` to also produce an agent background rect. Use 3-slot output: `[agent-bg, cmd-bg, caret]` with zero-size rects for absent elements.

### 2a. Extend `<cmd-panel-rects` (line 620)
- Add `!agent-output` as input parameter
- Add `!ai-provider` (from Fix 1)
- New logic:

```clojure
(defn <cmd-panel-rects
  [!cmd-panel !focus !caret-visible !scroll-y !viewport !settings !active-font
   !ai-provider !agent-output cmd-panel-h]
  (m/latest
    (fn [panel focus caret-visible scroll-y viewport settings active-font
         agent-output]
      (let [dpr (:dpr viewport)
            snap? (:snap-to-pixel? settings)
            font-size (:font-size settings)
            char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
            invisible {:x 0 :y 0 :w 0 :h 0 :r 0 :g 0 :b 0 :a 0}

            ;; Agent output background
            agent-panel-h 180
            agent-visible? (some? (:status agent-output))
            agent-bg (if agent-visible?
                       (let [agent-y0 (maybe-snap
                                        (+ scroll-y (- (:height viewport)
                                                       cmd-panel-h agent-panel-h 12))
                                        dpr snap?)]
                         {:x 0 :y agent-y0
                          :w (:width viewport) :h (+ agent-panel-h 12)
                          :r 0.10 :g 0.10 :b 0.13 :a 0.95})
                       invisible)

            ;; Command panel background
            panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h)) dpr snap?)
            cmd-bg (if (:visible panel)
                     {:x 0 :y panel-y :w (:width viewport) :h cmd-panel-h
                      :r 0.15 :g 0.15 :b 0.2 :a 1.0}
                     invisible)

            ;; Caret
            text-x (cmd-text-start-x @!ai-provider font-size (:char-width active-font) dpr snap?)
            caret (if (and (:visible panel) caret-visible (= focus :command-panel))
                    {:x (+ text-x (* (:cursor panel) char-advance))
                     :y (maybe-snap (+ panel-y 8) dpr snap?)
                     :w 2
                     :h (maybe-snap (- cmd-panel-h 16) dpr snap?)
                     :r 0.9 :g 0.9 :b 0.9 :a 1.0}
                    invisible)]

        ;; Always 3 instances: [agent-bg, cmd-bg, caret]
        [agent-bg cmd-bg caret]))

    (m/watch !cmd-panel)
    (m/watch !focus)
    (m/watch !caret-visible)
    (m/watch !scroll-y)
    (m/watch !viewport)
    (m/watch !settings)
    (m/watch !active-font)
    (m/watch !agent-output)))
```

### 2b. Update `draw-frame!` in `editor.cljs` (lines 389-415)
- Add `:agent-visible` keyword arg
- Draw agent-bg BEFORE text, cmd-bg before cmd text, caret AFTER text:

```clojure
;; Agent output background (instance 0) — draw after editor text, before agent text
(when (and agent-visible cmd-rect-sys (>= (:num-instances cmd-rect-sys) 1))
  (.setPipeline pass (:pipeline cmd-rect-sys))
  (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
  (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
  (.draw pass 6 1 0 0))

;; Command panel background (instance 1)
(when (and cmd-panel-visible cmd-rect-sys (>= (:num-instances cmd-rect-sys) 2))
  (.setPipeline pass (:pipeline cmd-rect-sys))
  (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
  (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
  (.draw pass 6 1 0 1))

;; Command + agent text
(when (< editor-lines total-lines)
  ...)

;; Caret (instance 2)
(when (and cmd-panel-visible cmd-rect-sys (>= (:num-instances cmd-rect-sys) 3))
  (.setPipeline pass (:pipeline cmd-rect-sys))
  (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
  (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
  (.draw pass 6 1 0 2))
```

### 2c. Update call sites
- `<world-snapshot` (line 2172): pass `agent-visible` through world map
  - Add `:agent-visible (some? (:status @!agent-output))` — derive from agent-output atom
- `draw-frame!` call (line 2332): add `:agent-visible` kwarg
- `<cmd-panel-rects` call (line 2163): add `!ai-provider !agent-output` args

---

## Fix 3: Enter Feedback (agent status visibility)

**Root cause**: Enter handler hides cmd panel, `submit-agent-run!` sets `!agent-output {:status :running}`, but without a background rect, the running status is invisible.

**Fix**: Mostly solved by Fix 2. Once agent output has a background, the `[CLAUDE] running: ...` text is immediately visible with a dark panel behind it.

Agent text rendering in `<combined-text-ops` is NOT gated by `(:visible panel)` — it runs unconditionally based on `!agent-output` status. **This is already correct. No code changes needed beyond Fix 2.**

---

## Fix 4: Focus Race Condition (typing bug)

**Root cause**: Two scenarios can steal focus from `:command-panel`:

### 4a. File load completion (line 1901)
When a file finishes loading from sidebar, it unconditionally resets `!focus` to `:editor`, even if the command panel is open.

**Fix** (line 1901):
```clojure
;; Before:
(reset! !focus :editor)

;; After:
(when-not (:visible @!cmd-panel)
  (reset! !focus :editor))
```

### 4b. Click in editor while command panel is open (lines 1754, 1774)
Clicking in the editor area resets focus to `:editor` but does NOT close the command panel. This leaves the panel visible but non-functional (keystrokes go to editor). User sees panel but can't type in it.

**Fix** (in the `(if clicked-in-cmd? ...)` else branch, around line 1726):
```clojure
;; Before editor click handling, close cmd panel if open:
(when (:visible @!cmd-panel)
  (swap! !cmd-panel assoc :visible false))
;; Then proceed with normal editor click (reset! !focus :editor) etc.
```

This closes the panel when you click away — standard UI behavior.

---

## Files Modified

| File | Changes |
|------|---------|
| `src/app/client/webgpu/loop.cljs` | Fixes 1-4: helper fns, `<cmd-panel-rects` rewrite, text-x fixes, focus guards |
| `src/app/client/webgpu/editor.cljs` | Fix 2: `draw-frame!` agent-bg rendering order, `:agent-visible` kwarg |

---

## Verification

1. **Build**: `clj -M:dev -m dev` — should compile without errors
2. **Cursor alignment**: Open Ctrl-K, type text — caret should blink at the end of typed text, after `[CLAUDE]> `
3. **Agent background**: Submit a prompt — dark panel should appear behind agent output text, covering editor code
4. **Enter feedback**: Press Enter after typing — `[CLAUDE] running: ...` visible with dark background immediately
5. **Focus stability**: Open Ctrl-K while a file is loading — panel should stay functional. Click in editor while panel is open — panel should close.
