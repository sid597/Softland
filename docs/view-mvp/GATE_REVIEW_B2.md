# WP-B2 gate review — Fable, 2026-07-05 (marathon session)

Reviewer: Fable (the orchestrating session; the fresh-eyes layers below it
were independent Opus subagents — implementation receipts + the layer-4
falsification at `../trail-view/DIFF_FALSIFICATION_R1.md`). Per the
2026-07-05 process ruling, session boundaries are gone but every QC layer
that judged this work ran in a context that did not author it. The parallel
Track-A Fable session gates WP1 independently (proposed split in the baton).

## Gate ledger (CONTRACT §11)

| Gate | Verdict | Evidence |
|---|---|---|
| 1–4, 6–13 | **GREEN** | `trail_face_test.clj` — 14 deftests, 403 assertions incl. gate 16; full-suite run 28 tests / 909 assertions / 0 failures |
| 5 atlas regression | **GREEN** | merged Ubuntu+DejaVu MSDF atlas: 588 glyphs, must-have set + U+FFFD present, ASCII intact, uniform 0.56 advance |
| 14 watcher loop | **GREEN** | `ingest_watchers_test.clj` 15 assertions: decision-latched (no polling), epoch +1 exactly, idempotent rewrite converges, loop survives poisoned file |
| 16 fixture fidelity | **GREEN — and it FIRED** | live capture caught 4 real drifts (verdicts are full edge rows + `:current`; `:omission/kind :feed/uncovered` + string reason; no `:display-name`; anchors carry `:block-path`); fixtures + cards re-derived same session |
| 17 regression | **GREEN (mechanical half)** | kernel + WP1 + OC + probe suites green unmodified; shadow `:dev` compiles (248 files, 0 warnings). Byte-identical existing-mode claim rests on the falsification pass's additive-wiring + arg-order checks (held). The `:prod` build is pre-broken (prod.cljc requires Electric v2) — pre-existing, out of scope |
| 15 windowing evidence | **DEFERRED to first light** | `MEASUREMENT_RAF.md` can only exist after Sid drives real material |

Traps 1, 3, 5, 13 spot-checked in the diff: all held (falsification report,
probes section). Style gates S1–S5: S1 seam clean (Trail* e/defns call only
WP1 §7 wrappers; the epoch atom is the sanctioned S1a carve-out); S2 pure
(one named shim); S3 no 0.56 under `trail_face/`; S4 control-byte-clean
(file(1) text everywhere); S5 `:trail-face/*` ids.

## Verdict

**PASS, conditional on first light.** Everything executable is green; the
one should-fix (hash-equality cache) was applied and recompiled. The package
does not CLOSE until the first-light session records `FIRST_LIGHT.md` +
`MEASUREMENT_RAF.md` (gate 15) and the visual halves (OP-27 fallback
rendering, live pane parity) are eyeballed.

## First-light risk register (read before driving)

1. **`trail-rt` defonce delay boots a 4-module in-process IPC cluster
   synchronously on the FIRST trail pull** — expect seconds of stall on the
   first `/trail` command; the cluster is never closed; watcher wiring must
   pass this same handle (`ingest-watchers/start-ingest-watchers!` takes
   `{:runtime rt}` — wire it where the server boots, or start watchers
   lazily from the same delay).
2. OI-3: an EDITED `.md` mints a NEW object (content-hash key) — the feed
   shows one `:source-ingested` entry per real edit; only byte-identical
   rewrites converge silently. Expected, not a bug.
3. Sid's OI-2 ruling: the manifest default is now the merged MSDF atlas —
   TEMPORARY for verification; **slug remains the destination**; slug
   glyph-set expansion is the named follow-up (needs slug meta/curve/band
   regeneration toolchain, unnamed by the contract).
4. Raw astral input that bypasses the sanitizer double-advances in the
   shaper (falsification note 1) — only reachable if an op skips
   `sanitize-tree`; none does today.
5. First `/trail timeline` with no address uses a rolling last-24h arrival
   window stamped client-side at request time; re-stamped on each epoch
   re-pull (debounced ≥ 1s).
