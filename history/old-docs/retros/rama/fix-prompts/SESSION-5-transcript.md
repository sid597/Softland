# Fix Session 5 — Transcript Kernel (May-era remnant)

Status: DONE (2026-06-12) — postfix R4 verdict: pass. Fixed: TR-01/02 (per-write-site idempotency, no front gate, write-if-absent + monotonic cursor fold + seen-marked per-run counters; fold-request/fold-claim with fingerprints, sticky terminals via authorize-mutation), TR-03 (redact-text + redact-payload-with-redactions, redact-before-truncate previews, :transcript/redactions populated), TR-04/06 (bounded reduce-jsonl-observations, read-complete-appended-lines TOCTOU gone, harvest newline-gated + streaming, watch inode-keyed cursors advancing only after appends, per-file fail isolation, fatal → :failed + stop), TR-05 (claim ownership arbitration + counter barrier before :complete), TR-07 (all tool_use blocks exploded, redacted inputs stored). Deferred with flags (in module header + FIX_PLAN.md): F4 obs-depot partitioning, TR-08 conversation/ledger reshaping, :since, empty-file note, parse-error fallback visibility, A4. Probes: test/app/server/rama/dogfood_transcript_probe_test.clj (12 blocks, 37 assertions). Artifacts: 05-transcript-remnant/{FIX_PLAN.md, IMPLEMENTATION_VALIDATION-postfix.md}.

Self-contained prompt. Hardening the May-era paths of `src/app/server/rama/dogfood/transcript.clj`. CRITICAL CONSTRAINT: the June 2026 skill-era object-container layer (`object_container*`, `transcript_ingest.clj`) sits ON TOP of this file — fixes must not break it. Run `git blame` to know which lines are May-era; run the object-container + transcript-ingest test namespaces after every change.

## Load first
- Skills `/rama` + `rama-retro`.
- Read: `docs/retros/rama/05-transcript-remnant/FINDINGS.md` (TR-01..TR-09, TT-01..04) + `05-transcript-remnant/IMPLEMENTATION_VALIDATION.md` + `TEST_VALIDATION.md`. NOTE: no validated plan exists for this track — fixes sketch against `IMPLICIT_SPEC.md` + skill rules; TR-03 and TR-08 explicitly need a small Phase-1 plan first (do it in plan mode as part of this session). Cross-check prior retro: `build/rama-retro-review/05-transcript-capture/RAMA_REVIEW.md` — F1 unredacted parse-error previews (runtime-proven), F2 UTF-8 byte-identity corruption (replace `RandomAccessFile.readLine` with raw-byte reads + explicit UTF-8 decode), F3 accumulate-vs-stream harvest, F4 obs depot partitioned by request not source file.

## Plan first (plan mode)
Correctness scope: (a) real redaction before persistence — parse-error previews redacted or hash-only; populate `:transcript/redactions` (TR-03 ≡ prior F1); (b) byte-correct reading — raw-byte line reads, UTF-8-safe offsets, newline-gated cursor advancement on BOTH harvest and watch paths (TR-04/06 ≡ prior F2); (c) idempotent folds + sticky terminals via Session 0 helpers (TR-01/02); (d) streaming harvest appends instead of accumulate-then-append (prior F3). Then: all tool_use blocks indexed (TR-07 ≡ prior F5), claim arbitration (TR-05 ≡ prior F6). Defer with flags: partitioning realignment (prior F4 — document the deviation if not fixed), conversation-view/ledger reshaping (TR-08).

## Protocol
1. `(require 'app.server.rama.dogfood.transcript 'app.server.rama.dogfood.transcript-ingest)` first.
2. Failing probes first: multi-byte UTF-8 fixture round-trip (byte offsets + payload text), malformed JSON line carrying a fake secret (must persist redacted), duplicate submit after `:complete` (no regression), partial trailing line (not ingested, cursor not advanced), multi-tool_use line (all blocks indexed).
3. Fix until probes pass. After EVERY change: run transcript, transcript-ingest, AND object-container test namespaces.
4. Re-run Phase 4 (blame-scoped) → `05-transcript-remnant/IMPLEMENTATION_VALIDATION-postfix.md` with verdict.

## Hard rules
Commit only code files; never read env.clj / codex_implementation / .agents. Do not modify `object_container*` or `transcript_ingest.clj` in this session — if a fix requires it, stop and record the contract change needed in `docs/sessions/next-prompt.md`. On completion: Status → DONE; note in next-prompt.
