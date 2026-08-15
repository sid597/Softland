# editor-feel — NOW

> **CLOSED — subject deleted (2026-08-16).** The editor world this board
> repaired (`editor_compute.cljs`, `sidebar_io.cljs`, the keyboard editor
> path) was removed whole in the dead-path pass (Sid's ruling C, census
> items 5–17; commits `59c1d4e` + `941a0bb` — see
> `docs/dead-path-census.md` § Outcome). Everything below is the record of
> the repair as it ran; none of its fences or steps are executable now.

Repair lane (no contract; substance-not-ceremony). Model Opus 4.8. Owns board
thread #9 only. Fences: touch ONLY `keyboard.cljs`, `editor_compute.cljs`,
`sidebar_io.cljs`, `runtime.cljs`. Do NOT touch `face_*`, `combined_text.cljs`,
`renderer.cljs`, server, kernels. Never read `env.clj`.

## STANDING (frozen at open)

Goal: remove verified typing-lag causes from the zoom-100 editor hot path,
keep the suite green, leave the `[RAF]`/`[EDITOR-RAMA]` measurement capability
in place (gated), verify by code + compile + probe. Sid's fingers are the
final acceptance (his frame: optimize every dimension while keeping the editor
live/composable/lineage-bearing). Prefer REMOVING work over adding cleverness;
kill tail spikes (a swallowed mid-word keystroke), not just the mean.

Baseline suite (this box, pre-change): recorded below once the run lands.

## The five verified causes (2026-07-11 Fable trace) → fix decisions

1. **Phase-4B mirror on the hot path** (`keyboard.cljs` char/cut/undo/redo +
   `mouse.cljs:43`, via `sidebar_io/save-editor-doc!`): every keystroke
   `pr-str`s the FULL `:lines` vector + POSTs it + logs per response. Loop
   never closes (`get-editor-doc` has zero callers; `:save` is a Blob
   download). **Fix:** `(def editor-rama-mirror? false)` in sidebar_io.cljs;
   gate the body of `save-editor-doc!` on it (single switch → covers the
   out-of-fence `mouse.cljs` caller too). In keyboard.cljs, DRY the 4
   duplicated call sites into one private `mirror-editor-doc!` that also gates,
   so the hot path skips even the deref+alloc.
5. **Per-keystroke `[EDITOR-RAMA]` console noise** — same gate handles it (no
   fetch → no `.then` log).

2. **O(file) `(mapv count (:lines doc))` per key event** (`keyboard.cljs:146`,
   runs for every event incl. char). +
4. **`<bracket-match` re-does `(mapv count lines)`** per doc change incl.
   cursor moves (`editor_compute.cljs:150`).
   **Fix (one design, covers both):** cache `:lengths` alongside the doc.
   `doc-with-lengths` helper in editor_compute.cljs is the SOLE deriver;
   `editor-apply-event` wraps its 5 line-changing branches with it (so the
   out-of-fence `mouse.cljs` paste path gets it free). Readers use
   `(or (:lengths doc) (mapv count …))` — absent-safe (initial doc from
   out-of-fence state.cljs), and stale-safe because every `:lines` writer
   refreshes `:lengths` (see falsification trace).

3. **Whole-file `detect-folds-fn` per typed char** (`editor_compute.cljs:130`
   `<fold-state`). **Fix:** inline a leading `throttle` (~150ms) on the
   deduped `:lines` input. Leading (not trailing debounce) is REQUIRED:
   `<fold-data` feeds two `m/latest`s (`<editor-content` + combined_text) that
   gate TEXT rects, so the flow must stay initially ready — a trailing
   debounce would blank the editor for 150ms on every file open.

## Falsification trace — the `:lengths`-on-doc cache (highest-risk change)

Every writer that changes `:lines` (traced across all src):
- char/backspace/delete/enter/paste → INSIDE `editor-apply-event` → wrapped.
  (keyboard AND mouse both call it → both correct, mouse.cljs untouched.)
- `:cut` / `:undo` / `:redo` / file-load → keyboard.cljs (in fence) → wrapped.
- initial doc → state.cljs (OUT of fence) → has no `:lengths` → read-fallback
  `(or (:lengths doc) …)` covers it (absent, not stale).
Cursor-only writers (movement, mouse click/drag 118/128/312/352,
selection-clear) `assoc :cursor/:selection` → `:lines` unchanged → `:lengths`
preserved & valid. Reader shape = vector of ints = exactly what
move-cursor/find-bracket/find-form already receive. No `:lengths` collision on
the doc (only the separate fold-state map used it before).

## Verification RESULTS
- CLJS compile — CLEAN. Drove the ALREADY-RUNNING dev server's shadow watch
  (nREPL :9002 → incremental `compile :dev`, NOT a cold standalone compile per
  the dev-workflow rule): "Build completed. (253 files, 3 compiled, 0 warnings,
  1.20s)" + 0 build-info warnings. The 3 compiled = exactly my 3 files. This is
  the load-bearing gate (a CLJS resolution/paren error can't show in the CLJ
  suite). The live app now serves the change — Sid can reload and type.
- Deterministic suite — GREEN. face suite + missionary-claims + trail-face =
  85t / 1625a / 0f / 0e (includes missionary-claims, the regression guard for
  the throttle's Missionary laws). Superset of the board's 46t/804a.
  Caveat (honest): the brute-force run-ALL-namespaces run hit environmental
  Rama-cluster failures (LeaderNotFound / stale-approval / kernel-shape) from
  booting every dogfood module in one JVM — a known multi-module hazard, NOT
  this change (CLJS-only edits are not in any CLJ test's causal path).
- Throttle initial-readiness — JVM probe CONFIRMED (first `m/latest` value in
  3ms, < the 100ms window). Fold throttle won't break initial render.
- Corroboration: the parallel write-echo session (thread #10) re-measured this
  same editor→Rama path at ~210ms round-trip floor (the old "7.5ms" was only
  the optimistic one-way write) — i.e. cause 1 was a ~210ms/keystroke mirror
  on the main thread. Gating it off is exactly right.
- `[RAF]`/typing-burst capture + subjective feel: PENDING Sid's fingers
  (WebGPU browser app; can't drive headlessly). Instrumentation left in place.

## NOW (≤15-line entries, newest last)
- 2026-07-11 · boot: board read (own #9), tree clean (no W2 residue → no T11),
  all 5 causes traced to file:line, `:lengths` writer-set closed, throttle
  initial-readiness probed green. Editing next.
- 2026-07-11 (into 07-12) · CODE DONE + verified. editor_compute.cljs
  (doc-with-lengths deriver + 5 wrapped branches; local leading `throttle` +
  `<fold-state` throttled ~150ms; `<bracket-match` reuses `:lengths`),
  keyboard.cljs (cached-lengths read; 4 mirror sites DRY'd into gated
  `mirror-editor-doc!`; cut/undo/redo/file-load wrapped), sidebar_io.cljs
  (`editor-rama-mirror?` def false + `save-editor-doc!` gated → covers the
  out-of-fence mouse.cljs caller + kills [EDITOR-RAMA] log). Compile clean in
  the live dev server (3 files, 0 warn); 85t/1625a green. `get-editor-doc`
  zero-callers claim verified. Assertion-grade close: the five verified causes
  are removed or gated; the two touched hot flows (`<fold-state`,
  `<bracket-match`) stay initially-ready and the `:lengths` cache cannot go
  stale (single deriver + every `:lines` writer routed through it, read-side
  fallback for the initial doc). NOT done, by design: Sid's typing-feel +
  `[RAF]` capture (his fingers are the pre-registered acceptance). Instrumentation
  left in place. Code/docs committed separately.
