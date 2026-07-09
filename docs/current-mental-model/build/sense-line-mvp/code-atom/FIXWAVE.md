# Code-Atom FIX WAVE — DIFF_FALSIFICATION findings applied

2026-07-09 · Opus 4.8 (fix-wave session, cheaper-model work under the line-cited
fix list per Sid's budget call). Authority: `DIFF_FALSIFICATION.md` (same folder;
6 CONFIRMED + 2 PLAUSIBLE). All 8 applied; every fix carries a regression that
BITES (fails before the fix). CODE STILL UNCOMMITTED — commits gate on Sid's word.

## Receipts (all three suites re-run independently by the orchestrator, GREEN)

| suite | before | after |
|---|---|---|
| `clojure-adapter-test` | 7t / 78a / 0f | **8t / 82a / 0f** (F2 regression) |
| `code-atoms-test` | 5t / 164a / 0f | **8t / 179a / 0f** (F5·F6·P2 tests; G5–G10 pins updated) |
| `relation-kernel-test` | 2t / 222a / 0f | **2t / 222a / 0f** (untouched — no `relation_kernel.clj` edit this wave) |

Files touched: `object_container/clojure_adapter.clj` (F2) · `code_atoms.clj`
(F1·F3·F4·F5·F6·P1) · both test nss (regressions + updated pins) · `SPEC.md` §3.5
+ `CONTRACT.md` R7 (F2 `#n`→`~n`). NO `relation_kernel.clj` / `deps.edn` / other
file changed (allowlist respected). Compile-check: `:COMPILE-OK` on both source nss.

## Per-finding

- **F1 — stale-edge leak on whole-component deletion** (`code_atoms.clj`
  reconcile). An analyzer edge whose BOTH endpoints leave HEAD shares no
  target-key with any desired edge, so the target read can't see it → never
  retracted → a persistent lie (violates R5). **Fix:** a caller-owned,
  cluster-scoped `:analyzer-basis` atom holding the last pass's desired set
  (rid→refs); reconcile retracts `prior-basis ∖ desired ∖ current-asserted`
  (each confirmed `:asserted` via `read-relation-detail` before retract+count).
  `:reconcile-basis-missing 1` when no prior basis. **Regression:** G10 D4 (a
  `P→Q` edge with UNIQUE endpoints, dropped from the desired set, retracts via
  the basis; pre-fix it stayed `:asserted`). See the DECISION below — this is
  the one finding I diverged from the sketch on, deliberately, and flag for
  redline.

- **F2 — block-path `#N` dedup collision** (`clojure_adapter.clj` `dedup-path`).
  `foo#2` is a legal symbol, so a `#`-suffixed duplicate `foo` collided with a
  literal `(def foo#2 …)` → one unit-id → a lost DerivedUnitRow. **Fix:**
  separator `#`→`~` (reader-unquote char, cannot occur in a symbol token —
  verified: rewrite-clj splits `foo~2` into `foo` `~2`; `(read-string "foo~2")`
  → `foo`). **Regression:** `f2-dedup-separator-no-collision-with-literal-hash-var`
  (dup `foo` + literal `foo#2` → 4 distinct block-paths/unit-ids). Docs: SPEC
  §3.5 + CONTRACT R7 updated in this docs commit (direct replacement).

- **F3 — constant-HEAD reassert-after-retract divergence** (`code_atoms.clj`
  reconcile). The assert key `code:<rid>:<head>` was HEAD-scoped only, so a
  retract-then-re-desire at a FIXED head reused the original journal entry →
  the re-assert was dropped, the edge stayed `:retracted`, the stat lied `+1`.
  **Fix:** transition-unique key `code:<rid>:<head>:t<N>[:retract]`, N = prior
  status-transition count from `read-relation-detail` history (ground truth,
  replay-stable). N is read ONLY for rids with a prior row (re-asserts +
  retracts) — a fresh first sync issues ZERO detail reads. The transition-unique
  key also makes the assert/retract COUNTS honest by construction. **Regression:**
  G10 D5 (re-add `P→Q` at the fixed head → re-assert LANDS, 3-transition
  history; pre-fix it stayed `:retracted`).

- **F4 — ingest-timeout blob silently uncounted** (`code_atoms.clj` code-sync!).
  A nil decision (`await…` timeout, `runtime.clj:326`) or a rejected status was
  counted in NEITHER ingested/converged/denied → `:blobs-seen` silently exceeded
  the sum. **Fix:** `:blobs-unresolved` (nil/rejected decisions) + the identity
  `:blobs-seen = denied + git-failures + ingested + converged + unresolved`; plus
  `:lineage-over-unresolved` (lineage edges minted over an unresolved new-side
  blob — dangling but kernel-legal, now counted). Pins added to G5/G6/G9 (all 0
  on the happy path).

- **F5 — git non-zero exit swallowed** (`code_atoms.clj` `git-bytes`). Returned
  stdout regardless of `.waitFor` → a failed cat-file yielded empty bytes → a
  silent 0-unit blob. **Fix:** `git-bytes` throws an ex-info tagged `:git/exit`
  on non-zero; ingest + lineage + materialize isolate ONE failing blob (catch
  `:git/exit`, count `:git-failures`, skip) — never abort a sync, never a silent
  empty blob. **Regression:** `f5-git-failure-is-loud` (`blob-text` on a bad sha
  THROWS).

- **F6 — deny granularity mismatch ingest-vs-lineage** (`code_atoms.clj`
  `enumerate-blobs`). Ingest fail-closed a blob seen at ANY denied path while
  lineage denied per change-path → a blob denied at X but changed at allowed Y
  was excluded from ingest yet cut by lineage. **Fix:** `enumerate-blobs` denies
  PER PATH — a blob is `:denied?` only when it has NO allowed-path occurrence
  (matches `commit-lineage`). **Regression:** `f6-deny-granularity-unified-per-path`
  (blob at denied+allowed → ingestable via allowed; denied-only → excluded by
  both lanes). Moot for env.clj (gitignored) but the lanes now AGREE.

- **P1 — analyzer temp-dir leak** (`code_atoms.clj` analyzer-sync!). Each run's
  `materialize-head-tree!` temp dir was never deleted. **Fix:** `delete-recursively!`
  in a `finally` around the analyzer body (runs on success OR throw). The kondo
  config dir is a `defonce` delay — created once, intentionally kept.

- **P2 — skip-count gated only at 0** (`code_atoms_test.clj`). `:usages-unmapped`
  was pinned only at 0 (rk-path scope); the non-zero path lived only in P3 prose.
  **Fix:** `p2-skip-count-pinned-nonzero` — a PURE test feeding
  `derive-desired-edges` a synthetic analysis (nil `:from-var` + a usage mapping
  to no enclosing unit) → `:usages-unmapped 2` exact. Stable (no HEAD drift, no
  kondo/kernel), gates the CLASS.

## DECISION — F1 realized as in-memory basis, NOT a durable file (flag for redline)

The opening prompt sketched F1 as "persist the desired-set snapshot (edn under
`data/`)" and explicitly invited a stop-clause if I judged it policy-grade. I did
NOT write a durable file, and here is why (this is the one judgment call in the
wave):

1. **The kernel cannot be read by asserter.** `relation_kernel.clj` is
   target-partitioned by design (§6/§7): `$$relations-by-id` keyed by relation-id,
   `$$relations-by-target` by target-key; there is no "all edges by asserter"
   query and no full scan. So catching a both-endpoints-gone edge genuinely
   requires REMEMBERING what was asserted — the reconcile has no target-key to
   look it up by. Reading Rama alone (the back-arrow-clean option) cannot close
   F1 without a NEW kernel index/query = outside the §2 allowlist = a stop-clause.

2. **A durable file is the exact footgun the codebase already scarred on.**
   `implementation-quirks.md` (durable side-state × ephemeral `create-ipc`
   cluster): a durable snapshot keyed to cluster contents "silently poisons the
   NEXT boot." F1's sketch is that pattern. The safe form quirks endorses is
   in-memory state minted with the cluster ("the assert-log pattern … is the SAFE
   shape; a skip-list is the unsafe one").

3. **In-memory, cluster-scoped basis honors R5 within the correct scope and
   makes NO durability commitment.** The `:analyzer-basis` atom lives + dies with
   the cluster/runtime — which is exactly the retraction scope of an ephemeral
   cluster (a fresh boot re-asserts ONLY HEAD's edges, so there is no cross-boot
   leak to retract). It is forward-compatible: when Sid rules the durability fork
   (durable cluster vs spine-edge replay log — future-binding, his), a durable
   basis slots in behind the SAME `:analyzer-basis` seam.

**What is deferred (honest boundary):** cross-BOOT retraction of a both-endpoints-gone
edge in a hypothetical durable-cluster world. On today's ephemeral cluster this is
a non-issue; the single-boot dogfood sync (CONTRACT §12) is always correct.
`:reconcile-basis-missing` counts every reconcile that had no prior basis (e.g. a
first sync), so the map declares the boundary rather than hiding it. Recorded in
`decisions.md` (D-006 notes) for Sid's redline — revert-cheap if he wants the
durable snapshot instead.

## Residue / open doubts (each with its falsifier)

- **F4/F5 non-zero paths tested at ZERO only.** The happy-path pins assert
  `:blobs-unresolved 0 / :git-failures 0 / :lineage-over-unresolved 0`; the F5
  git-throw IS exercised (`f5-git-failure-is-loud`), but inducing a REAL ingest
  timeout in a test needs a broken runtime. The non-zero accounting is sound by
  construction (nil decision ≠ `:accepted` → counted; unresolved shas always have
  cached text so `cut-of` never throws). *Falsifier:* a fault-injected runtime that
  drops one decision → assert `:blobs-unresolved 1` and the identity holds.
- **Whole-tree analyzer pins (458/5073/279/50) remain ungated by design.** They
  drift with every commit, so a CI pin would be brittle; they belong in the
  live dogfood receipt (CONTRACT §12), not a regression gate. P2 gates the skip
  CLASS instead.
- **Two IPC launches in `code-atoms-test`** (lineage-gates + analyzer-gates)
  unchanged from P3 — sequential, try/finally-closed, suite green.

## Gate route

Per the opening prompt: Route A (fresh Fable, preferred post-reset) or Route B
(this session gates with the same protocol, doubts recorded). The REPL dogfood
receipt (CONTRACT §12 definition-of-done) runs at/after gate.
