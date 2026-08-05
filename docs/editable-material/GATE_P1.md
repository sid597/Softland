# P1 gate — provenance facet-master (Gate 1, anatomy) — PASS

2026-07-24 · Fable orchestration session (fresh-context falsification + gate,
work-package skill). Inputs: the full uncommitted diff read end-to-end (300
lines across 11 files + 596 lines in 3 new files), Codex's NOW.md trail as
prior-pass record (input, not authority), independent re-runs of every
receipt. Code commit `83aa5dd`; baseline repair `b6f3d75`.

## Independent receipts (all re-run this session, none taken on faith)

- Full suite, all 44 `*_test.clj` namespaces: **383 tests / 5,183 assertions /
  2 failures / 0 errors** — the ONLY red was `kernel-shape-test` (pre-P1
  baseline drift, see adjudication a). Codex's six in-suite
  `stale-approval-on-executor-death-test` failures did NOT reproduce (the
  namespace was green in-suite despite visible LeaderNotFoundException
  contention noise in the log).
- `dogfood-llm-test` standalone: 12t/114a green (second independent
  standalone green after Codex's).
- Focused P1: `provenance-material-test` + `face-projection-test` 12t/132a
  green. Kernel-shape standalone pre-repair: 2 deterministic failures, both
  `:face-arsenal` as the extra key — byte-matching Codex's report.
- CLJS `:dev` build in-process: **0 warnings**.
- HEAD-reading suites re-run AFTER the commits moved HEAD (skill rule):
  code-atoms + git-spine 16t/365a green.

## Falsification pass — held

- **Routing (the thrice-fired foreign-read class):** `fm:` two-segment in
  `leading-object-key` + `imp:fm:` branch in `extract-object-key`; `rev:fm:`
  strips to `leading-object-key` and lands correctly BECAUSE of the `fm:`
  addition there. Verified byte-level; gated live by four-task IPC foreign
  point reads in the suite (import-completion, source, container, revision).
- **No optimistic render:** the only client write surface is the
  FacePull-confirmed `!provenance-material-data` mirror; the drill fetch is
  console-log-only; activation has no client render path in P1.
- **Totality chain closed end-to-end:** `serve` wraps every projection in
  `catch Throwable` → error data-context → `resolved-wear`'s shape check
  fails → code floor. Cluster death renders the floor tint (byte-identical
  to v0 default), never black, never a dead websocket.
- **One truth:** `machine-tint` deleted; `code-floor` and the imported v0
  material derive from the same `default-form`; the suite asserts the byte
  identity.
- **Startup read-only:** `ensure-master!` has exactly two callers — the
  deploy-time ingest (exits) and the drill endpoint. No boot-path write.
- **Idempotency:** deterministic v0 request-ids + `time-ms 0`; drill
  idempotent per drill-id; epoch bumps only on accepted non-replay.
- **Lifecycle:** error card upsert/close both idempotent
  (`close-instance!` no-ops when absent); the ground watch keys on
  `material-render-state` — a slow-moving truth (the keying-source rule).
  `rebuild-material-sites!` covers exactly the consuming sites (headers are
  machine-only, ground.cljs:536).
- **Wall-clock discipline:** time enters at request construction, never
  in-topology.

## Adjudications (the three the handoff named)

**(a) tests-green fence → SATISFIED.** The 8 reported failures decompose:
- 6× `stale-approval-on-executor-death-test`: flake class, now clinched —
  standalone green twice independently AND in-suite green on this session's
  full rerun. Added to the known-flake registry
  (memory/implementation-quirks.md) with the registry's protocol (standalone
  rerun before diagnosing; two consecutive standalone failures = real).
- 2× `kernel-shape-test`: PRE-P1 deterministic baseline drift — framework W2
  (`1725f55`) added face-arsenal as kernel #6 to only 2 of the 4
  always-present example fields, against KERNEL-SHAPE's own evolution law
  ("add an :examples entry to every field you used"). Ruling: the fix
  belonged to a dedicated micro-repair, NOT P1 (Codex's refusal to expand was
  correct — the repair required reading face_arsenal.clj to write accurate
  entries, a small design act, not s/five/six/). **Repaired this session as
  its own commit `b6f3d75`** (KERNEL-SHAPE completed: interpret/materialize
  examples + pstate count 4 + projections :absent; test re-pins six);
  standalone 10/10 green. The suite is now honestly green with zero
  asterisks for P2–P8's fences.

**(b) `src/app/shared/` placement → ACCEPTED as the convention.** Ruling: a
compiler consumed by BOTH the server module side and the client render is
authority-neutral; `face_assembly.cljc` living under `client/workspace/`
while server-side assembly_adapter requires it is the inherited wart, not a
law worth propagating. `src/app/shared/` is now the sanctioned home for
genuinely both-sides `.cljc` (CLAUDE.md source-structure updated). Reversal
cost: one mechanical ns move. `face_assembly.cljc` migrates at a natural
P3 touchpoint, not before (LATER, board untouched).

**(c) Commit at Sid's standing 07-24 word → EXECUTED.** `83aa5dd`, code-only,
exact paths, 14 files. The stray empty `.wtest` probe file was deleted, not
committed. Veto anytime, any line.

## Open doubts (non-blocking, each with its cheap falsifier)

- **Cross-JVM activation staleness:** an activation issued from a foreign JVM
  (external REPL) would not bump the app server's ingest-epoch atom → clients
  stale until the next epoch move. No P1 surface does this (deploy ingest
  runs pre-clients; drill endpoint is in-JVM). Falsifier: when P6 lands
  activation requests, assert the write path routes through the app server's
  epoch bump — one-line check at P6 review.
- **Wear in every block's render-sig:** plain blocks eat one wasted (but
  byte-identical) rebuild after a wear change. Negligible at 103 blocks;
  becomes a narrow-invalidation data point when the echo gate ever complains.
- **Drill accretion:** each fresh drill-id durably retains a malformed
  candidate as latest — by design (retained trace; Gate 4's immune memory).
  Watch only that latest-vs-active confusion never leaks into a non-drill
  surface; the ordinary projection already strips candidate fields (tested).

## Residue for later packages

- The P1 wound trace (`?drill=p1-final-1784884925150`, rejected revision
  retained as latest) is Gate 4's first immune-memory entry — lands in P6.
- Platform issue OPEN (pre-existing): drains hang at depot-flush;
  `bin/land down` → dirty → `unwedge` → `up` is the sanctioned sequence.
- Flake registry now carries `stale-approval-on-executor-death-test`.
