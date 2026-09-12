# Lane A — the driver (`machine_cut.clj`) — RESULTS

Verdict: **GREEN**. All Lane-A gates (G1, G2, G3, G5, G6, G7, G8, G9, G11) run
to green IN THIS CONTEXT with the fake adapter (`llm.clj:2179-2184` canned
`:lines`) — no live LLM anywhere. `:pairs-with` was already in the tree
(`relation_kernel.clj`, commit `f864c74`; countersigned `656b697`), so the kind
was used DIRECTLY — no indirection constant was built (per dispatch amendment).

## Suite run (in-context, exact)

Command (lane discipline — my ns + the two dep suites I ride, serial, one JVM):

```
clojure -M:test -e "(require '[clojure.test :as t] 'app.machine-cut-test
  'app.server.rama.relation-kernel-test 'app.server.rama.dogfood-llm-test)
  (t/run-tests 'app.machine-cut-test 'app.server.rama.relation-kernel-test
               'app.server.rama.dogfood-llm-test)"
```

Final line (re-run on the exact committed file, after the file(1) fix below):

```
Ran 18 tests containing 424 assertions.
0 failures, 0 errors.
:FINAL-SUMMARY {:test 18, :pass 424, :fail 0, :error 0, :type :summary}
```

`app.machine-cut-test` alone: **Ran 4 tests containing 88 assertions. 0
failures, 0 errors.** The other 336 assertions are the dep suites
(relation-kernel 2 deftests, dogfood-llm 12 deftests) — both green, unaltered by
this lane. First-run green (the upstream layers' receipt); the only in-run
diagnostic is the EXPECTED torn-line drop: `[MACHINE-CUT] replay: line 4 failed,
skipping: EOF while reading` (G11's honest torn-trailing-line handling).

## Per-gate results

PURE (no cluster) — `g1/g2/g3` deftests:
- **G1 input determinism** — GREEN. same synthetic data-context → identical
  input-hash + all synthetic ids; one changed block text → hash AND every id
  change; salt separates re-guess from idempotent re-run; MC-T9 bundle
  rebuild-verify matches on same input, fails on drift.
- **G2 validation totality** — GREEN. canned outputs valid / unknown-uuid /
  missing-uuid / duplicate-uuid / non-human-prompt / malformed-JSON → exact
  accepted sets + rejection counts; malformed → `parse-output :ok? false`
  (never throws); totality invariant asserted on every case.
- **G3 pair-plan diff** — GREEN. desired vs existing fixtures → exact
  `{asserts, retracts, unchanged}`; retracted existing row re-asserts; empty
  desired retracts every asserted edge.

IPC (fake adapter) — `machine-cut-ipc-test` deftest (1 llm launch + 1 rk
launch for G5–G9, 1 fresh rk launch for G11):
- **G5 end-to-end assert** — GREEN. corpus → llm run row under the synthetic
  `llm-run-mc:` id; 2 rk edges land with §4 identity (`:pairs-with`, actor
  `llm:machine-cut/v1`, `:asserter-type :llm`), evidence = response event's
  first-block source-id, note grammar v1; from=response / to=prompt; both
  endpoint copies (o:/i:, 4 raw entries) under the ONE conversation key deduped
  to 2 edges (MC-T14); authoritative row agrees with the target read.
- **G6 idempotent re-run** — GREEN. same input+version → `:noop-complete`, ZERO
  adapter calls, run row + edge set value-identical, no epoch bump.
- **G7 never-drop** — GREEN. `$$llm-dead-letters` empty for the run.
- **G8 reconcile** — GREEN. a salted re-guess moves a pairing → stale edge
  `:retracted` (history shows `[:asserted :retracted]`), moved edge asserted,
  unchanged edge untouched.
- **G9 epoch** — GREEN. epoch bumps by exactly 1 after edge acks on a
  successful write; a malformed-output run writes 0 edges and does NOT bump.
- **G11 WAL replay** — GREEN. fresh cluster + existing WAL → edges re-asserted
  with identical ids, reconcile re-applied in file order (the v2 move
  reproduced: stale edge retracted), ZERO adapter calls (replay-wal! takes no
  adapter); a hand-appended torn trailing line counted + dropped, the rest
  replayed.

Style (mechanical, in-context):
- **G16 share** — `env.clj` unread/untouched (no `env` reference in the diff);
  no new `imp:` prefix (G15 share); `file(1)` says *Clojure module source,
  Unicode text, UTF-8 text* for both new files (fixed — see below); no raw
  control bytes remain. No `try` inside any `e/defn` (none touched).

## rama-pitfalls verdict (driver = foreign client, NO new module)

The driver declares no depots/topologies/PStates — most sections are N/A (no
new Rama surface). The load-bearing ones:
- SIDE-EFFECT RETRY (MC-T5): PASS — the LLM spawn is in the driver
  (`run-one-pending-with-claude!`), never a topology event; a topology retry
  cannot re-fork.
- RETRY IDEMPOTENCE OF IDS (MC-T1/T2): PASS — actor is version-scoped
  (`llm:machine-cut/v1`), never run-scoped; idempotency-key = `"mc:"+relation-id`,
  stable across runs (no per-run volatile).
- ACK LEVEL: PASS — rk + llm appends use `:append-ack`, every read behind a
  materialized barrier (`rk/await-relation`, `llm/await-run`); no ack-vs-visibility race.
- BACK-ARROW: PASS — driver writes intent (turn-run-request) + reads Rama
  truth; edges are foreign appends AFTER validation.
- PSTATE OWNERSHIP: PASS — no new PStates; edges owned by the rk topology, run
  rows by the llm topology.

## Files created (fence-clean — NEW files only)

- `src/app/server/rama/machine_cut.clj` — the driver (pure core + IPC shell + WAL replay).
- `test/app/machine_cut_test.clj` — the suite (G1–G3 pure, G5–G9/G11 IPC).
- (No `test/app/fixtures/machine_cut/*` files — synthetic corpus is inline in
  the test for readability; the fixtures dir was allowed, not required.)

Verified: `git status` shows ONLY these two as my additions; no kernel /
face_projection / file_viewer / existing-test edits. (Lane B's parallel files
appear in the same tree but were not touched by this lane.)

## Judgment calls

1. **Conversation serve is INJECTED (`ctx :load-river-blocks`), not a hard dep
   on block-distiller/face_projection.** MC-T4 (input = the shared river-page
   read the projection uses) is honored by injecting river-page's OUTPUT; the
   default loader lazily `requiring-resolve`s `block-distiller/river-page` so
   the driver's ns/suite never loads the block-kernel runtime (Lane A depends on
   relation_kernel + llm only, per the lane prompt).
2. **The driver consumes river-page BLOCKS, not shape-conversation turns.**
   `evidence-source-id` (§4.3) = the response event's first-block `:source-id`,
   which `shape-conversation`/`blocks->turns` STRIPS (it keeps only id/kind/
   text/order/time). river-page IS the shared substrate the projection reads
   (`face_projection.clj:194`), so grouping river-page blocks myself preserves
   MC-T4 coverage parity AND yields source-id. §5.1's literal phrase
   "conversation-projection output" is read as "the river-page serve the
   projection is built on" — the honest reading that makes §4.3 deliverable.
3. **MC-T9 rebuild-verify** is a hash-equality check in the injected
   `load-context-bundle` closure: recompute input-hash from a fresh load,
   compare to the run-hash embedded in the bundle-id; mismatch throws
   failed-stale. §3's "rebuilds from the recipe encoded in the bundle id" is
   satisfied by the closure carrying the recipe + the hash-in-id as verifier (a
   sha-256 cannot be decoded back to a recipe).
4. **Idempotent re-run (G6) = detect-and-skip**, not re-derive-and-re-append:
   a terminal-succeeded run-id returns `:noop-complete` (zero adapter, zero
   writes). Self-healing of a partial-append failure is deferred to WAL replay
   at boot (MC-T7), not to re-annotation — a v0 scope choice.
5. **`file(1)` fix (self-caught):** my first write of `id-part-separator`
   emitted a raw NUL byte in the string literal (`file` → *data*). Replaced with
   the `"<NUL>"` ESCAPE (ASCII source, same compiled NUL value, matching
   `relation_kernel.clj:90`); `file` now says *text*. Behavior identical (both
   compile to a one-char NUL string); the final green run is on the fixed file.
6. **Minor duplication:** the live path (`annotate-run!`) inlines the
   assert/retract/barrier while `replay-wal!` uses the shared
   `apply-edge-reconcile!`. Both correct; a DRY pass could unify them.

## INT flags (for the orchestrating session)

- **[INT-1] Production loader wiring.** file_viewer boot must pass a
  `:load-river-blocks` that returns river-page BLOCKS (carrying `:source-id`),
  NOT `shape-conversation` turns. `default-load-river-blocks` already resolves
  `block-distiller/river-page` lazily; confirm the boot passes `oc-rt` (or the
  loader) and the clamped limit (default 64).
- **[INT-2] Shared actor constant.** `mc/machine-cut-actor-id` is the canonical
  def; Lane B defines its own copy with an INT flag. INT checklist item 3: one
  def, two requires — re-home + point both nss at it.
- **[INT-3] observed-model is nil in v0.** The stream-json parser captures
  `model` only into token-usage's absence (it discards `message_start` model);
  the driver reads `get-in run-row [:token-usage :model]` → nil → note records
  `model=unknown`. For G13's receipt (real corpus), plumb observed-model from a
  real message observation (honest recording, §10.8) — v0 is `unknown`.
- **[INT-4] G5's "projection serves the pairs"** is out of Lane A's fence (needs
  Lane B's additive `:conversation` projection keys). Lane A proved up through
  rk edges landing + dual-readable; INT/Lane-B closes the projection-serve
  assertion end-to-end (G14 wearing).
- **[INT-5] §5.1 literal-vs-intent (INT/gate-review classify).** The
  "input = conversation-projection output" phrasing vs the source-id need
  (judgment call 2) — classify as implementer-fixable (the river-page reading)
  or record a contract-text erratum for the retro (§14 Criterion-3 expects ≥1).

## Stop-clauses

None fired. The `:pairs-with` enum was already landed + countersigned (dispatch
confirmed; verified `relation_kernel.clj:79`), so no lane-vs-enum stop-clause; no
second kernel edit needed; no assembly-grammar or projection-totality block; no
binding-doc conflict beyond the INT-5 literal-vs-intent note above (classified,
not improvised).
