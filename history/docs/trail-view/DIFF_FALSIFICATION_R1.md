# Batched diff-falsification R1 — WP1 A1/A2/A3 + Phase B, and WP-B2 (2026-07-05)

Layer-4 fresh-context adversarial review (work-package skill), run as ONE
Opus subagent from the marathon (Track-B-origin) Fable session, covering the
whole uncommitted working tree vs HEAD: the Track-A kernel phases A1/A2/A3
(the review these diffs owed), WP1 Phase B (`trail_view.clj` + tests), and
all of WP-B2 (trail_face core, fixtures, wiring, watchers, renderer shape
fns, atlas regen, Electric bridge). Suites at review time: 28 tests / 909
assertions / 0 failures; shadow `:dev` compiles.

Consumers: Track-A gate review (parallel Fable session) and Track-B gate
review (marathon session, `build/view-mvp/GATE_REVIEW_B2.md`).

## Verdict

- **BLOCKERS: none.**
- **Kernel A1/A2/A3: SOUND** (custody is metadata not identity; edge carries
  the latest transition's writer with full trail in events/decisions; stance
  registry one-line and bounded; activity rows client-stamped only, written
  on the accepted branch AFTER the journal gate — replay adds zero rows).
- **SHOULD-FIX (1): APPLIED same session.** `editor_compute.cljs`
  `<trail-face` compared cache inputs by `hash` — a collision would freeze a
  stale scene ("state stuck masking future truth"). Fixed to value equality
  (`!last-trail-struct` holds the input vector, compared with `=`).

## Notes (first-light / scale watch items — none gate-blocking)

1. `renderer.cljs` shape fns iterate UTF-16 units: a RAW astral codepoint
   bypassing the sanitizer renders TWO fallbacks and advances twice. Correct
   only because the cljc sanitizer collapses astral -> one U+FFFD upstream
   (the contract's intended layering). BMP behavior byte-identical to before.
2. `trail_view.clj` subselect/`|all$$` reads carry no `:allow-yield?` —
   Rama 1.6.0 REJECTS it on mirror selects (platform finding; quirks
   updated). Accepted at phase-1 corpus scale; watch at corpus growth.
3. `file_viewer.cljc` `trail-view-runtime` defonce delay boots an in-process
   4-module IPC cluster SYNCHRONOUSLY inside `e/server` on first pull:
   reactor stall, boot latency, never closed, and the watcher-handle sharing
   is not yet wired. All P6/first-light per contract; the top risk to watch.
4. `ingest_watchers.clj`: `.reset` return ignored (stale key on deleted dir,
   minor leak); debounce cancel/re-arm race can cause a redundant import,
   never a dropped one (idempotent re-import absorbs it).
5. `<trail-face` performs cache `reset!`s inside `m/latest` — follows the
   sidebar precedent the contract cites; not the RAF-death class. Possible
   one-frame staleness between builder and text consumer; transient.
6. `render-bundle-text` over a `rel:*` bundle target renders degenerate
   empty sections without crashing; path untested by the gate fixture.
7. `cards.cljc` docstring claimed ASCII-escaped glyphs while using raw
   printable UTF-8 — docstring corrected same session (S4 bans control
   bytes only; glyphs still pass sanitize).

## Probes attempted and HELD (one line each)

- Trap 1/gate 13: render flatten, text ops, and click hit-test all read ONE
  cached tree object; no rebuild-at-click; no non-reactive deref in m/latest.
- Trap 3/gate 2: no `:clip?` on the text face; `tree->text-ops` called
  clip-less -> truncate/wrap paths are no-ops; `split-projection-lines`
  preserves trailing newlines (round-trips).
- Trap 5/13: `sanitize-tree` covers every node's ops at the tail of BOTH
  builders; no op-emission path escapes.
- Surrogate math: `codepoint-segments` never lands on a trailing surrogate;
  lone surrogates count 1; cljs astral branch correct.
- Renderer: present-glyph geometry uses pre-advance x0 (byte-identical to
  old); advance unconditional exactly once; no double-advance.
- Kernel replay: bucket/order-key from client stamps only; 4th hop inside
  `(<<if outcome-accepted?)` after the journal filter.
- Constructor arities: no positional `->Relation*Row` call site outside
  `relation_kernel.clj` in src/ or test/.
- `trail_view` gate 14: zero depot/append/local-write forms; read-only by
  construction.
- Conversation cursor `last-order-key + (char 1)`: skip/dup-safe on
  fixed-width order-keys (which the OC projection provides).
- Electric bridge: no try/catch in any e/defn; `case` mounts only the taken
  branch; expanded entry-keys map to target ids correctly.
- Arg-order: combined_text 19 flows <-> 19 fn args; `<trail-face` 6<->6;
  `<editor-rects` 6<->6; render.cljs call sites match. No swapped pair.
- Scroll zone sits after flow, before chat (trap 9); `:panes` case covers
  both new modes (no-default case satisfied).
