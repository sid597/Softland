# editor-feel — opening prompt (2026-07-11)

**Session name:** `editor-feel` · **Model: Opus 4.8** · one session, repair lane
(no contract — substance-not-ceremony; this is hot-path repair, not runtime growth).

## What this is

The zoom-100 editor has felt typing lag (Sid, daily). A Fable session traced the
keystroke path end-to-end on 2026-07-11 and pinned the causes at file:line. Your
job: remove them, verify with the existing instrumentation, keep the suite green.

## Boot

1. Read `docs/sessions/next-prompt.md` (board) — you own ONLY the `editor-feel`
   thread line. Keep session notes in `build/editor-feel/NOW.md` (create it).
2. `git status` FIRST. If Wave-2 working-tree code (face_* files, faces EDN) is
   still uncommitted, do NOT commit or revert it — flag on the board (T11 class)
   and work around it. Never commit another thread's diff.
3. Run the suite before touching anything; record the baseline (expect 46t/804a
   green as of W2 close).

## The verified causes (2026-07-11 Fable trace)

1. **Phase-4B mirror on the hot path** — `keyboard.cljs:154-158` (also the
   `:cut`/`:undo`/`:redo` sites ~199-227): every keystroke `pr-str`s the FULL
   `:lines` vector and POSTs it (`sidebar_io.cljs:18-38 save-editor-doc!`),
   plus a `console.log` per response. It was measurement scaffolding ("Phase 4B
   measurement" — the 7.5ms number is recorded; `get-editor-doc` has ZERO
   callers; the loop never closes). **Remove it** behind a flag default-OFF
   (`editor-rama-mirror?`); nothing is lost — the disk file was never written
   back anyway (the `:save` key is a browser Blob download, `keyboard.cljs:79-88`).
2. **O(file) per key event** — `keyboard.cljs:146` `(mapv count (:lines doc))`
   runs for EVERY event type. Compute it only for the event types that need it,
   or cache line-lengths alongside the doc.
3. **Whole-file fold scan per text change** — `editor_compute.cljs:130`
   `<fold-state` dedupes on `:lines` (good) but then runs `detect-folds-fn`
   over ALL lines per typed char. Debounce it (e.g. ~150ms) or make it
   incremental. OBEY the Missionary laws: no side effects in `m/latest`;
   event filtering via `m/eduction` (electric-docs skill: L1/R2/R3/L8).
4. **`<bracket-match` (`editor_compute.cljs:140`)** re-does `(mapv count lines)`
   O(file) on every doc change incl. cursor moves — reuse the cached lengths
   from (2).
5. Remove the per-keystroke `[EDITOR-RAMA]` console noise (goes with (1)).

## Fences

- Touch ONLY: `keyboard.cljs`, `editor_compute.cljs`, `sidebar_io.cljs`,
  `runtime.cljs` (wiring only if needed).
- Do NOT touch: `face_*` (fresh W2 code), `combined_text.cljs` windowing,
  `renderer.cljs`, server code, any kernel.
- NEVER read `src/app/server/env.clj`.

## Acceptance (record numbers in NOW.md)

- Before/after typing-burst capture using the EXISTING `[RAF]` log
  (`render.cljs:484-487`, fires when frame prep >5ms with a 4-way breakdown):
  open a mid-size file, type a sustained burst, save both logs.
- After: no `[RAF]` >5ms lines attributable to typing on a mid-size file;
  no per-keystroke network requests in devtools.
- Suite green (same counts as baseline). Dev flow: `clj -A:dev -X dev/-main`
  (NEVER standalone shadow-cljs).
- Final subjective check belongs to Sid's fingers — note it as pending his wear.

## Close duties

- Code commits: code files ONLY, never mixed with docs, never on the docs
  branch. Docs (NOW.md, board line) commit separately on
  `docs/current-mental-model-local`.
- Flip your board line (pointer + status only); details in
  `build/editor-feel/NOW.md`; end with an assertion-grade entry.
