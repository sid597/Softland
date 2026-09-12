# write-echo-2 — opening prompt (2026-07-12)

**Session name:** `write-echo-2` · **Model: Opus 4.8** · ~half a session.
Continuation of write-echo (board thread 10): SAME pre-registered criterion,
SAME method, DIFFERENT topology class. Probe code stays UNCOMMITTED.

## The question

write-echo proved the ~210ms echo floor is the MICROBATCH iteration cadence —
read `build/write-echo/NOW.md` FIRST; its method, isolation guards, leg
definitions, and deviations discipline carry over verbatim. D-013 (decisions.md,
PROPOSED) closed the microbatch route and pre-registered this follow-up:
**does the same loop against a STREAM topology pass p95 ≤ 50ms AND ≤1 stall
(>100ms) per sustained minute?** Rama's guidance says stream is the class for
single-digit-ms write-then-read-back; append-ack (~5ms) already fits budget.

## How

- Build a MINIMAL probe module (uncommitted, probe-only — do NOT edit product
  kernels): one request depot → ONE stream topology → a keyed PState write
  mirroring the text-kernel signal shape (head/status row). Preserve per-key
  serialization (hash-by routing key). Same IPC launch opts as write-echo
  (`{:tasks 4 :threads 2}`) — today's real substrate.
- Reuse the harness (`src/app/probe/write_echo_probe.clj`, untracked): same
  open-loop loads (12/s and 3/s, 60s each), same leg-1/leg-2 measurement, same
  isolation discipline — the stale-head fake-instant trap is pre-named; fresh
  artifact + tag-scoped ids per scenario.
- Report the same tables + leg decomposition. If stream passes with margin,
  ALSO characterize leg-3 (the S40 mirror-atom + e/watch bump) with a bound,
  so the D-013 follow-up ruling can box full E2E.
- Contract input to NAME in the report (not probe scope, do not build):
  stream drops microbatch's cross-PState exactly-once — the eventual
  block-write design owes idempotency by op-id instead.
- Boot `.claude/skills/rama/` (+ pitfalls) before writing the topology.

## Output & duties

- Append a dated entry with numbers + method + deviations to
  `build/write-echo/NOW.md` (same file, same thread); flip board thread 10.
- **The verdict is NOT yours:** it fills D-013 ruling-2's evidence slot →
  Fable ruling → Sid's countersign. Sid's fingers stay final both directions.
- Docs commit on the docs branch with EXPLICIT paths (`git commit -- <paths>`)
  — parallel sessions share this tree's index and have cross-swept staged
  files before (see implementation-quirks).
- Fences: no product code, no kernel edits, probe uncommitted, never read
  `src/app/server/env.clj`.
