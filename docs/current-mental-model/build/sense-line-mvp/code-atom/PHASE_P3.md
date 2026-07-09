# Phase P3 — the analyzer lane + the one registry edit (`code_atoms.clj`)

2026-07-09 · code-atom work package · implementation phase (fresh context, Opus) ·
CONTRACT §12 P3: "analyzer lane + registry edit: `:requires :calls` + kondo pass +
retraction → G7, G8, G10 green." Built + gated GREEN in this same context. Parse
tool / enumeration / analysis were ruled in `P0_PARSE_SPIKE.md`; the adapter (P1)
and driver+lineage lane (P2) it extends were built in `PHASE_P1.md`/`PHASE_P2.md`.
No git commits (orchestrator commits); no code edited outside the §2 allowlist.

## STOP-CLAUSE STATUS: NONE TRIGGERED

No §9 stop-clause fired. clj-kondo recovered the G7 floor with Rama's shipped hooks
(no descope to `:requires`-only); no allowlist breach (exactly the §2-authorized
registry edit + the P0-ruled deps line + additive fns/tests); no binding-doc
conflict; `env.clj` never opened (it is not in git history — deny-list is exercised
via `:deny-list-override`, P2 precedent).

## Files created / touched (allowlist-exact)

| file | status | what |
|---|---|---|
| `src/app/server/rama/code_atoms.clj` | ADDITIVE | analyzer lane appended AFTER P2's `code-unit-id`; P2's fns byte-untouched (proven by G5/G6/G9 still green). One additive ns require: `[clj-kondo.core :as kondo]`. |
| `src/app/server/rama/relation_kernel.clj` | 1 edit | the `relation-kinds` set literal (:58-65) gains `:requires :calls` + a 2-line attribution comment. Nothing else. |
| `deps.edn` | +1 line | `clj-kondo/clj-kondo {:mvn/version "2025.06.05"}` (P0-ruled). |
| `test/app/server/rama/code_atoms_test.clj` | ADDITIVE | `g8-registry-additive` (pure) + `analyzer-gates` (G7+G10, ONE new IPC launch); P2's three deftests untouched. |
| `docs/.../code-atom/PHASE_P3.md` | NEW | this artifact |

The block-distiller sibling-package files (untracked) were NOT touched.

### Registry diff (verbatim)

```diff
     ;; idempotency, retraction, history, and evidence anchoring for free. Binary
     ;; directed (from = judgment carrier, to = judged thing); :supersedes is belief
     ;; displacement, distinct from :built-over / :new-direction construction lineage.
-    :confirms :refutes :supersedes})
+    :confirms :refutes :supersedes
+    ;; code-atom mechanical dependency edges (code-atom CONTRACT §2, SPEC §5.1):
+    ;; :requires (ns→ns, from the ns form) + :calls (var→var continuant, clj-kondo).
+    :requires :calls})
```

T11 coordination: the addition is a self-contained line appended to the set. The
block-kernel package adds `:grounds :assembled-from :refines` on its own line, so
the two land in separate commits and 3-way merge trivially (both insert distinct
lines before `}`). No reader of `relation-kinds` other than `registered-kind?`
exists (grep-verified src+test), and no test pins the set/count, so widening the
registry is safe (`relation_kernel_test.clj:556`'s `≤ 2 × registry` bound only
grows).

## clj-kondo config recipe as executed (P0 §B.4, robustified)

Rama 1.6.0 ships its own clj-kondo hooks INSIDE the jar under
`clj-kondo.exports/com.rpl/rama/` (`config.edn` + `com/rpl/{utils,errors,rama_hooks}.clj`).
The analyzer reads them **from the classpath via `io/resource`** (not a hardcoded
`~/.m2` jar path — more robust than P0's manual `unzip`) into a fresh temp config
dir laid out as:

```
<cfg>/config.edn                    -> {:config-paths ["com.rpl/rama"]}
<cfg>/com.rpl/rama/config.edn       -> (Rama's shipped config)
<cfg>/com.rpl/rama/com/rpl/{utils,errors,rama_hooks}.clj
```

then `(clj-kondo.core/run! {:lint [head-tree-dir] :config {:output {:analysis true}}
:config-dir <cfg>})`. The config dir is built once per JVM (`defonce` + `delay`).
HEAD is materialized to a temp dir as RAW blob bytes via `git cat-file blob`
(T3 — never the dirty checkout; the working tree carries this session's own
uncommitted edits). Measured effect (relation_kernel.clj@HEAD): findings 125→5,
fabricated dataflow var-usages 193→6, **G7 floor preserved 4/4 + 7/7**; the 5
residual `:unresolved-symbol` are the `$$`-PState refs inside `<<query-topology`
bodies (ignorable, counted as `:unresolved-residual`).

## Desired-set sizes at HEAD (`b7e00e9`)

| scope | files | `:requires` | `:calls` | total | derive time |
|---|---:|---:|---:|---:|---:|
| **specimen** (relation_kernel.clj@HEAD, G7 `:path-filter`) | 1 | **7** | **115** | 122 | — |
| **whole HEAD tree** (production default, `:path-filter` nil = R5 scope) | 94 | **458** | **5073** | 5531 | ~3.8 s |

Specimen derivation stats (G7, pinned): `{:ns-usages-seen 7 :var-usages-seen 837
:var-usages-dropped-nonapp 695 :usages-unmapped 0 :requires-asserted 7
:calls-asserted 115 :retracted 0 :converged 0 :unresolved-residual 5}`. The
`relation-outcome → fixed-width-order-key` continuant collapses its 2 call sites
(rows 457,479) into exactly ONE edge (T6, verified). Whole-tree stats:
`{:ns-usages-seen 485 :var-usages-seen 46934 :var-usages-dropped-nonapp 38082
:usages-unmapped 279 …}` — the 279 unmapped are top-level / rich-comment / def-time
calls with nil `:from-var` or a row in a gap; SKIPPED + COUNTED, never guessed
(no silent caps). Whole-tree residual unresolved-symbol: 50.

## Design decisions inside the CONTRACT's freedom (trap citations)

1. **Version-free analyzer actor (R4/T1).** `import:code-analyzer` / `:import`;
   version rides `note` = `clj-atoms-v1|analyzer|<head-sha>`. Every retract's
   envelope `:actor` == the stored asserter (`import:code-analyzer`), so the
   retraction-rights guard (relation_kernel.clj:441-448, actor-id == stored
   asserter) passes — the whole point of R4: an analyzer run retracts its OWN
   stale edges.
2. **`:requires` (SPEC §5.2)** — one edge per distinct (app.* from-ns → to-ns)
   from `:namespace-usages`; self-ns skipped; target-kind `:ns` both ends,
   verbatim target-key (rk/->target-ref fallthrough, relation_kernel.clj:840-846).
   The app.* allowlist is **FROM-side only** — targets include the non-app deps
   (`com.rpl.rama`, `clojure.string`, …), exactly SPEC §5.2. Evidence = the ns
   form's anchor (best-effort; a `:requires` edge is still emitted if the anchor is
   unmappable — mechanical/near-certain, never skipped).
3. **`:calls` (SPEC §5.3, T5/T6)** — from `:var-usages`, **KEEP ONLY app.* `:to`**
   (the P0 §B.5 allowlist; untreated baseline = 193 garbage `:calls` on the
   specimen alone). CONTINUANT grain: one edge per distinct (ns-qualified
   `:from-var` → ns-qualified `:to/:name`); self-var (recursion) skipped;
   within-file cross-var KEPT; per-version detail rides the evidence anchor. Rows
   with nil `:from-var` or a row that maps to no enclosing top-level unit are
   SKIPPED + counted in `:usages-unmapped`.
4. **Evidence anchors (SPEC §5.3).** `evidence-source-id` = the calling FILE's HEAD
   blob source-id (the object-key math JOINS the P2-ingested `SourceArtifactRow`);
   `evidence-anchor-id` = the calling FORM unit's anchor (`oc/source-anchor-id` of
   the enclosing top-level unit-id). The usage's `:row` maps to the enclosing unit
   by **line-start offset** (col-independent → robust to astral-column drift; the
   repo's one astral char is in server_jetty.clj). The representative anchor for a
   continuant edge is the lowest-row call site — deterministic and, since all
   call sites of one `:from-var` live in one top-level form, well-defined.
5. **Current-status reconciliation (R5).** Read the analyzer's currently-asserted
   `:requires`/`:calls` edges over the involved target-keys (spine idiom), assert
   desired-not-present, RETRACT present-not-desired. Idempotency = request-id =
   `code:<relation-id>:<head-sha>` (assert) / `…:retract` (retract) — HEAD-scoped,
   so a byte-identical re-run at the same HEAD converges (journal replays, 0
   events; G10 third run).
6. **HEAD is the analyzer scope (R5).** `:path-filter` nil = the whole HEAD tree.
   The tests scope it to the specimen exactly as `code-sync!`'s `:commit-filter`
   scopes ingest — a test affordance, not a semantics change.
7. **All times from the HEAD committer clock (T4).** `asserted-at-ms`/`sent-at-ms`
   = HEAD commit's committer ms; a re-run at the same HEAD is byte-identical.

## Gate receipts (verbatim)

```
Testing app.server.rama.code-atoms-test
Ran 5 tests containing 164 assertions.
0 failures, 0 errors.
```
(P2's 3 deftests + `g8-registry-additive` + `analyzer-gates` = 5; P2 was 3/78.)

- **G7** — `analyzer-gates` INGEST + G7 blocks: the full stats map pinned
  (`{:ns-usages-seen 7 … :requires-asserted 7 :calls-asserted 115 :retracted 0
  :converged 0 :unresolved-residual 5}`); a `:requires` edge physically read from
  `$$relations-by-id` for each of the 7 ns deps (kind `:requires`, status
  `:asserted`, asserter `import:code-analyzer`, note `clj-atoms-v1|analyzer|<head>`);
  the 3 pinned `:calls` ground-truth edges present with evidence-anchor ==
  `oc/source-anchor-id` of the calling form's unit, the anchor row resolving with a
  span that CONTAINS the call site row (167/457/843), and evidence-source-id
  resolving to the ingested calling blob; `relation-outcome → fixed-width-order-key`
  is EXACTLY one edge (continuant, T6); physical negative — no `:calls` edge on
  `com.rpl.rama.ops/explode` / `clojure.core/str` / `com.rpl.rama/keypath`. **PASS.**
- **G8** — `g8-registry-additive` (pure): `registered-kind?` true for `:requires`
  and `:calls`, false for `:relates-to`; the stance + base kinds untouched. PLUS
  the FULL existing `relation-kernel-test` ns runs green with its assertion count
  UNCHANGED after the registry edit (below). **PASS.**
- **G10** — `analyzer-gates` G10 blocks (via `:desired-override`, deterministic):
  D1 asserts 3 edges (`{:calls-asserted 3 :retracted 0 :converged 0}`); D2 drops
  A→D → `{:calls-asserted 0 :retracted 1 :converged 2}`, A→D `:relation-status`
  flips to `:retracted` with history `[:asserted :retracted]` preserved, the other
  two converge untouched (single `[:asserted]` transition); D2-again →
  `{:calls-asserted 0 :retracted 0 :converged 2}` and A→D history stays
  `[:asserted :retracted]` (journal converged — zero third event). Proves
  version-free retraction rights end-to-end (R4). **PASS.**

**Regression — the two prior suites still green:**
```
Testing app.server.rama.relation-kernel-test
Testing app.server.rama.object-container.clojure-adapter-test
Ran 9 tests containing 300 assertions.
0 failures, 0 errors.
```
(relation-kernel-test 2 deftests + clojure-adapter-test 7 deftests. The
relation-kernel-test count is unchanged by the additive registry edit — G8.)

Invocation (BUILD SPEC):
`clojure -M:test -e "(require '[clojure.test :as t] 'app.server.rama.code-atoms-test)
(t/run-tests 'app.server.rama.code-atoms-test)"` and the same for
`'app.server.rama.relation-kernel-test` + `'app.server.rama.object-container.clojure-adapter-test`.

## Deviations from CONTRACT/SPEC (flagged loudly)

- **`clj-kondo` is in MAIN `:deps`, not a test-only alias.** CONTRACT §2 says
  "ADDITIVE dev/test-visible deps"; but the analyzer lane is a genuine RUNTIME
  feature (CONTRACT §5 step 4 of the sync loop), so `code_atoms.clj` requires
  `clj-kondo.core` at load. This mirrors P1's `rewrite-clj` (also a main dep for
  the same reason — the adapter needs it at runtime). If shipping an analysis tool
  in the app image is unwanted, the fix is a one-line move to a lazily-resolved dep
  (the analyzer is the only consumer). Recorded, not silently chosen.
- **`analyzer-sync!` gained two test-scoping params beyond the BUILD-SPEC signature:**
  `:path-filter` (repo-rel-path predicate; nil = whole HEAD tree = R5 scope) and
  `:deny-list-override` (R6, mirrors `code-sync!`). Both are optional affordances
  exactly analogous to `code-sync!`'s `:commit-filter`/`:deny-list-override`; the
  default behavior is the whole HEAD tree. G7 uses `:path-filter` to keep the gate
  fast + focused on the specimen (122 edges, not the 5531 whole-tree set).
- **Kondo config built from the CLASSPATH (`io/resource`), not the jar path.**
  Stronger than P0's manual extraction — no `~/.m2` layout assumption. Same result.
- **`env.clj` is not in git history** (gitignored) — so the deny-list can't fire on
  real history; R6/T10 is exercised elsewhere (P2's deny gate). Not a P3 concern
  (the analyzer applies `deny?` to the HEAD ls-tree before any cat-file anyway).
- **NONE from the binding rulings.** The lane conforms to CONTRACT §2/§3
  (R4,R5,R6)/§4/§5 step 4/§7 (T1,T4,T5,T6,T10,T11) and SPEC §5.1-§5.4 as written.

## Open doubts + cheap falsifiers (for the gate review)

1. **Constant-HEAD reassert-after-retract.** The assert idempotency key
   `code:<rid>:<head>` would REPLAY (not re-assert) an edge that was retracted then
   reappears in the desired set **at the same HEAD** — the journal already holds the
   assert decision under that key. Mitigated because real retracts come from HEAD
   changes (a call removed at a NEW commit → different head → different key), so the
   assert/retract keys never collide across a real sync. G10 does not exercise this
   (it only drops). Falsifier: at a fixed head, retract an edge then re-add it to
   desired and re-run — the assert replays. If daily use needs constant-head
   re-assertion, fold a status generation into the key.
2. **Retract-detection read scope.** Reconcile reads current analyzer edges only
   over the DESIRED endpoints' target-keys (the spine idiom — the kernel has no
   "edges-by-asserter" index). A stale edge whose BOTH endpoints vanish entirely
   from the desired set is not found (stays asserted-but-stale). For a connected
   code graph a caller/callee almost always survives as some edge's endpoint; G10's
   3 edges share endpoint A so a dropped edge is always found. Falsifier: a desired
   set where a dropped edge's two endpoints both disappear; the edge is not
   retracted. If daily use surfaces stuck stale edges, broaden the read to a tracked
   previous-endpoint set.
3. **Continuant merge on duplicate `:from-var`.** clj-kondo's `:from-var` is the
   bare name, so two same-named top-level defs (our block-path `#2` dedup) both
   calling X merge into one continuant edge whose representative anchor points at one
   arbitrarily. No such duplicate in relation_kernel.clj. Falsifier: a file with two
   same-named defs both calling the same var; the merged edge's anchor is one of the
   two.
4. **Whole-tree `:usages-unmapped` = 279 (~3% of app→app usages).** Top-level /
   rich-comment / def-time calls (nil `:from-var` or row-in-gap). Correctly skipped
   + counted, never guessed. Falsifier: if a consumer needs those as edges, the
   commentary/def-time lane is the named extension point — the spans are already
   addressable.
5. **G7 pins content-derived counts** (115 calls, 837 var-usages, 5 residual) from
   relation_kernel.clj@`a002c89` (== HEAD, clean this session). If the specimen file
   changes and is committed, these re-pin exactly as P2's lineage counts do — the
   test tracks HEAD's ground truth, not a frozen fixture.
