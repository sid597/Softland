Codex implementation notes (audit trail)
=======================================

Scope and intent
This file records the reasoning, decisions, and implementation status for the Session 7 refactor plan in progressive_implementation.md. The goal is auditability: future maintainers should understand why we did each change, how it aligns with the plan, and what remains.

Context and constraints
- Rendering must be WebGPU-only. HTML overlays are not acceptable for UI.
- Input handling is driven by Electric/Missionary event streams in loop.cljs.
- The command panel was added by branching editor logic, causing duplication and brittle draw-order hacks.
- The editor already has working selection, caret, and editing behaviors that must be preserved during refactor.

Primary goal
Unify editor and command panel input logic by introducing a shared TextInputCore. This is a structural refactor to reduce duplication and make new inputs (search, palette) feasible without copy-pasting logic.

Why this approach (design rationale)
- Single source of truth for text editing behaviors reduces bugs and maintenance overhead.
- Pure functions allow deterministic testing without needing WebGPU or Missionary.
- The plan in progressive_implementation.md (Session 7) explicitly defines TextInputCore as the foundation for later render and UI separation.
- Incremental refactor (command panel first, editor later) minimizes risk by limiting scope in each step.

Alternatives considered (and why rejected)
- Minimal cleanup only: would leave duplication in place and make new inputs expensive.
- Full rewrite of the editor state model: too disruptive given the current working editor.
- Separate WebGPU scene per input: still leaves duplicated editing logic and increases render complexity.

Implementation approach (overall)
1) Create TextInputCore as a pure, reusable set of editing functions.
2) Wire command panel events to TextInputCore (single-line mode).
3) Wire editor events to TextInputCore (multi-line mode).
4) Split rendering layers to remove rect ordering hacks.
5) Cleanup logs and document the final architecture.

State model decisions
- Single-line state:
  {:text "..." :cursor N :selection {:start N :end N}}
- Multi-line state:
  {:lines [...] :cursor {:line L :col C} :selection {:start {:line ...} :end {...}} :desired-col N}
- The core normalizes cursor/selection positions to avoid out-of-bounds errors when state changes across event sources.
- Selection is normalized to always represent a non-empty range (start != end). Empty selections are represented as nil.

TextInputCore design details
- Pure functions: all operations return a new state and do not perform side effects.
- Insertion supports newline-aware text, enabling Enter and multiline paste with line splitting.
- Deletion:
  - Backspace deletes prior character, or merges with previous line at column 0.
  - Forward delete removes next character, or merges with next line at line end.
- Navigation:
  - Cursor movement supports left/right/up/down/home/end for multi-line and single-line.
  - Word movement uses \\w for word characters and whitespace skipping for boundaries.
- Clipboard helpers:
  - cut returns {:state new-state :text cut-text}
  - copy returns text only, leaving caller to store it
  - paste accepts arbitrary text and handles multiline insertion
- Render helpers:
  - calculate-caret-rect and calculate-selection-rects exist to support reuse later.

Alignment with progressive_implementation.md (Session 7)

Phase 1: Extract TextInputCore
Status: Mostly complete.
Implementation:
- Added `src/app/client/webgpu/text_input.cljs` with core editing ops, navigation, selection, clipboard, and render helpers.
- Added forward `declare` for delete-selection to resolve CLJS forward-reference error.
 - Removed duplicate caret/selection helpers from editor.cljs in favor of TextInputCore versions.
Remaining:
- Add unit tests for TextInputCore.

Phase 2: Refactor command panel to use TextInputCore
Status: In progress (partially completed).
Implementation:
- `src/app/client/webgpu/loop.cljs` now routes command panel actions to TextInputCore:
  - :char-input
  - :backspace
  - :delete (forward delete)
  - :keydown (left/right/home/end)
  - :word-nav (left/right)
  - :paste
- Copy/cut are no-ops while the command panel is focused, preventing accidental edits to the main buffer.
Rationale:
- The command panel does not expose selection yet, so only cursor-based operations are wired.
Remaining:
- If command panel selection is desired, add :cmd-selection state and route through TextInputCore selection APIs.

Phase 3: Refactor editor to use TextInputCore
Status: In progress.
Implementation:
- Added adapter helpers in `src/app/client/webgpu/loop.cljs`:
  - editor-input-state: builds TextInputCore-compatible state from editor state.
  - apply-editor-input: maps TextInputCore state back to :sel-start/:sel-end/:desired-col.
  - commit-editor-input: updates lines/line-lengths and saves undo when edits occur.
- Rewired editor edit operations to TextInputCore:
  - :char-input, :backspace, :delete, :enter, :keydown, :word-nav, :copy, :cut, :paste.
Notes:
- Undo is still owned by loop.cljs; commit-editor-input only records undo when text changes.
- Cursor movement preserves the existing auto-scroll behavior.
- Selection is now handled by TextInputCore, which means insert/delete now replace the active selection instead of ignoring it.
Remaining:
- Verify selection behavior matches desired UX.
- Remove any now-dead helper logic once confidence is high.

Phase 4: Separate rendering pipelines
Status: In progress.
Implementation:
- Added a separate command panel rect system (own GPU buffer) in `src/app/client/webgpu/loop.cljs`.
- Split rect computation into:
  - editor-rects (selection/brackets/caret/eval/folds)
  - cmd-rects (background + caret)
- Updated `src/app/client/webgpu/editor.cljs` draw-frame! to accept two rect systems and draw them in explicit layers:
  1) editor rects
  2) editor text
  3) command panel background
  4) command panel text
  5) command panel caret
Notes:
- Text rendering still uses the combined text buffer and editor-line-count split. This keeps the change scoped to rect ordering and avoids reallocating text systems.
Remaining:
- Consider splitting text systems if we want fully independent pipelines later.

Phase 5: Cleanup and polish
Status: In progress (partial).
Completed:
- Removed [CMD]/[DRAW] debug logs from loop.cljs and editor.cljs.
Remaining:
- Update documentation to reflect the final state shape and architecture.
- Consider extracting command panel as its own namespace.

Behavioral compatibility expectations
- Command panel text editing should behave exactly as before, with the added improvement that paste respects focus.
- Editor editing should be functionally equivalent, with improved handling of active selections.

Behavioral changes introduced so far
- Editor insert/delete now respect active selections (replace selection on insert, delete selection on backspace/delete).
- Multi-line paste now preserves trailing newlines (previous implementation used split-lines and dropped trailing empties).
- Command panel rects are no longer appended into the editor rect buffer; draw order is explicit.
- Rendering remains unchanged until Phase 4 refactor.

Known limitations and risks
- TextInputCore has no tests yet; cursor edge cases could regress during editor wiring.
- Command panel selection is not implemented; copy/cut are intentionally disabled in that focus.
- There is still duplication of caret/selection rendering logic until helpers are moved.
- Word boundary rules are simple (\\w and whitespace) and may need refinement later.
- Multi-line insert/paste currently splits only on "\\n". If pasted text uses "\\r\\n", carriage returns will be retained in the line content unless we normalize.
- Command panel rect buffer has a fixed capacity (16) and does not auto-resize; adding more rects (selection, hints) could silently drop geometry.

Self-review: Electric/Missionary/WebGPU alignment
Goal framing
- Missionary is the event backbone; Electric is not currently in the WebGPU editor loop, so "Electric way" here means respecting the Missionary flow/signal model, keeping pure transforms where possible, and limiting side effects to reduce stages.
- WebGPU forward means minimizing buffer churn, making draw ordering explicit, and keeping per-frame work predictable.

Missionary alignment (docs: docs/missionary-complete-reference.txt)
- The loop still follows the canonical "watch -> derive -> reduce" pattern described in the Missionary reference:
  m/watch feeds signals, m/latest combines streams, and m/reduce performs discrete effects in the reducer.
- We preserved this structure and concentrated state mutations into the reducer, rather than spreading side effects across helpers.
- This keeps the mental model consistent with Missionary's reactive dataflow and makes it easier to reason about recomputation.

Electric alignment (docs: docs/electric/electric3.cljc)
- We did not add Electric DOM or e/defn changes in this iteration; the WebGPU loop remains a Missionary-driven engine.
- The "Electric way" for UI logic would be to push more state into e/watch/e/input and keep UI composition reactive. That is a larger architectural shift than Session 7, so this refactor stays scoped to core text-editing logic.
- TextInputCore mirrors Electric's preference for pure, referentially transparent transforms, which is compatible with future e/input wrapping if we choose to lift it into Electric later.

WebGPU alignment
- Separating the command panel rect buffer is a step toward explicit render layering; it avoids the previous "append and pray" ordering in a shared buffer.
- We intentionally did not split the text pipeline yet to avoid reallocating text geometry and to keep GPU churn low while behavior stabilizes.

How this follows the progressive_implementation plan
- Phase 1 (TextInputCore) is largely complete and matches the plan's intended state shape and pure function approach.
- Phase 2 (command panel wiring) is done for key input actions; selection remains out of scope as planned.
- Phase 3 (editor wiring) is mostly done; the adapter pattern matches the plan's proposed dispatcher concept.
- Phase 4 (render separation) is partially complete: rects are layered, text is still shared.
- Phase 5 (polish) started with log removal; docstrings/tests remain.

Open questions
- Do we want selection support for the command panel?
- Should command panel use a separate clipboard or share the internal one?
- Should word boundaries treat punctuation as separators differently for code vs plain text?

Audit log (concrete changes so far)
- Added `src/app/client/webgpu/text_input.cljs` (TextInputCore).
- Added forward `declare` for delete-selection in the TextInputCore.
- Updated `src/app/client/webgpu/loop.cljs` to route command panel input to TextInputCore.
- Updated `src/app/client/webgpu/loop.cljs` to route editor input to TextInputCore via adapter helpers.
- Updated `src/app/client/webgpu/editor.cljs` draw-frame! to accept separate editor/cmd rect systems.
- Updated `src/app/client/webgpu/loop.cljs` to maintain a separate command panel rect buffer.
- Removed [CMD]/[DRAW] debug logs in `src/app/client/webgpu/loop.cljs` and `src/app/client/webgpu/editor.cljs`.
- Removed unused caret/selection helper definitions from `src/app/client/webgpu/editor.cljs`.
- Added this `codex_implementation` file for ongoing audit notes.

Testing plan (pending)
- Unit tests for TextInputCore:
  - insert at start/middle/end
  - backspace at col 0 merges lines
  - forward delete at line end merges lines
  - delete selection across lines
  - word navigation across whitespace and punctuation
- Manual UI checks:
  - command panel input does not affect editor while focused
  - caret blink and selection rendering unchanged

Next steps (ordered)
1) Phase 3: editor adapter and replacement of editor edit logic with TextInputCore.
2) Phase 4: render layer split to remove rect ordering hacks.
3) Phase 5: cleanup logs, migrate caret/selection helpers, update docs.
