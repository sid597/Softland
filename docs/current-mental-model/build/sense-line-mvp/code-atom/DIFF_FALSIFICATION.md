# Code-Atom Diff — Falsification Pass (gate-review layer)

2026-07-09 · fresh-context adversarial review of the uncommitted code-atom
working tree (clojure_adapter.clj P1 · code_atoms.clj P2+P3 · relation_kernel.clj
registry edit · deps.edn · tests + fixtures). Default-FAIL posture: the job was
to BREAK the diff, not appreciate it. Every finding carries a line cite; every
break carries a concrete scenario. Records under review (PHASE_P1/P2/P3) were
re-derived, not trusted.

**What I ran (receipts):**
- `clojure_adapter_test` — **GREEN 7 tests / 78 assertions** (P1 pins: 103 units,
  census, determinism reproduce).
- `code_atoms_test` (P2+P3) — **GREEN 5 tests / 164 assertions** (G5 16 mech / 0
  silver / 284 re-addressed; G6 45 silver / 18 mech / 309 re-addressed; G9
  0-append re-run; G7 7 requires / 115 calls / 0 unmapped / 5 residual; G8; G10
  version-free retraction). The pinned numbers are REAL.
- Whole-HEAD-tree analyzer (production default `path-filter nil`, gated by NO
  test) — reproduced P3's pins **EXACT**: 94 files, 458 requires, 5073 calls,
  279 unmapped, 50 residual, `{:ns-usages-seen 485 :var-usages-seen 46934
  :var-usages-dropped-nonapp 38082}`. Sampled skip = nil-`:from-var` top-level
  protocol ref `IObjectContainerRequestPayload` (object_container.clj:43) —
  honestly counted.
- `foo#2` / `x#2` are **compiler-legal** symbols (`#'user/foo#2`, evaluates);
  the block-path collision below is reachable from a real committed blob.
- `decided-at-ms = (core/now-ms)` (object_container.clj:472) = wall clock → the
  convergence split is sound. git_spine precedents cited by the CONTRACT all
  exist (version-free asserter :41-45, batch-await :244-256, convergence
  :283-286, `read-commits` :140).

---

## Architecture

The diff is a faithful two-file extension of proven seams: `clojure_adapter.clj`
mirrors `markdown_adapter.clj` stage-for-stage (cut → `source-materialization` →
import-request builder) over the EXISTING object-container rows, and
`code_atoms.clj` is a git_spine-shaped driver (`code-sync!` / `analyzer-sync!`)
that calls only public OC + relation-kernel APIs — no new module, depot, or
PState. The one authorized kernel edit (`:requires :calls` into `relation-kinds`,
relation_kernel.clj:66-68) is additive. Identity math is pure over (blob-sha,
text): unit-ids, import-keys, edge relation-ids, and the stable idempotency keys
(`imp:clj:…`, `code:<rid>:<basis>`) are byte-reproducible, and all stored stamps
ride committer clocks (T4). This spine is sound; the gates pass because the
happy-path math is correct.

The breaks live in the SEAMS the gates don't exercise: (a) block-path uniqueness
is asserted "per blob" but the `#N` dedup suffix collides with a literal symbol
named `x#N`; (b) the analyzer's current-status reconciliation reads only the
DESIRED endpoints' target-keys, so an edge both of whose endpoints leave HEAD
(delete a self-contained file) is never retracted — a persistent lie, the one
thing Softland's map forbids; (c) several optimistic counts (`:blobs-ingested`,
`:calls-asserted`) are computed from request-side intent, not reconciled against
the materialized truth, so timeouts, journal-replays at constant HEAD, and
swallowed git errors go silently un-accounted. None sink the package; all are
recordable pre-close.

---

## Failure modes attempted (per hunt class)

### Class 1 — Reconcile custody (highest value) → CANNOT-HAPPEN (defended)
**Scenario:** Sid hand-asserts a `:calls` edge between the same two vars the
analyzer derives; a sync then tries to retract his edge → a
`:relation/retraction-forbidden` rejection every run.
**Verdict: CANNOT-HAPPEN.** Two independent barriers. (1) Asserter-scoped
identity: `relation-id-for` folds `asserter-actor-id` into the hash
(relation_kernel.clj:107-114), so Sid's edge and the analyzer's edge are
DIFFERENT relation-ids — the analyzer's desired/current sets never name his rid.
(2) The reconcile explicitly filters the current-set to its own asserter:
`(filter #(= analyzer (:asserter-actor-id %)))` (code_atoms.clj:666) — even
though `read-relations-for-targets` (code_atoms.clj:664) pulls ALL asserters'
edges touching the shared target-keys, Sid's row is dropped before `to-retract`
is computed (code_atoms.clj:672). No retract request is ever built against a
foreign asserter. **Reassert-after-retract** (analyzer edge retracted, call
reappears at a NEW head): `include-retracted? false` (code_atoms.clj:664) means
the retracted rid is absent from `current` → it lands in `to-assert`
(code_atoms.clj:671) with key `code:<rid>:<NEW-head>` → journal miss → re-assert
→ status flips `:retracted → :asserted` (legal, relation-outcome:453-456). Works
**only because the head moved** (see Class-5 constant-head break).

### Class 2 — Ordering / async → 1 CONFIRMED gap, rest defended
**Ingest timeout (CONFIRMED):** `append` ALL then `await` ALL
(code_atoms.clj:349-353). `await-object-container-decision` **returns nil on
timeout** (runtime.clj:326 — `(>= now deadline) decision` with `decision`=nil).
A nil decision fails `(= :accepted (:status %))` (code_atoms.clj:354) → counted
in NEITHER `:blobs-ingested` NOR `:blobs-converged` NOR `:blobs-denied`. There is
NO `:blobs-failed` counter, so `:blobs-seen` silently exceeds the sum — a silent
drop the CONTRACT §5.5 "no silent caps" forbids. Meanwhile the lineage lane cuts
that blob anyway (pure `cut-of`, independent of ingest) and mints `:supersedes`
edges at its unit-ids → **dangling edges for a blob that never landed** (legal
per kernel trap 3, but uncounted-as-failed). Inherited verbatim from git_spine
(spine-sync!:244-256) — established idiom, still a gap.
**Two concurrent syncs → CANNOT-HAPPEN (store):** stable keys
`code:<rid>:<child-sha>` / `code:<rid>:<head>` + microbatch exactly-once make the
journal idempotent; `seen-edges` (code_atoms.clj:359) dedups intra-sync. Counts
per-call stay internally consistent.
**Stats thread-safety → CANNOT-HAPPEN:** there are NO volatiles; per-sync state
is `reduce`-accumulated counts + three single-threaded atoms (text-cache
:326, cut-cache :330, seen-edges :359). `pass-started-ms` (:318) is a wall-clock
READ used only to classify fresh-vs-converged, never stored.

### Class 3 — Lineage law edges → CONFIRMED collision, rest defended
**Block-path `#N` collision (CONFIRMED-BREAK):** `dedup-path` suffixes a repeated
name with `#2`,`#3`… (clojure_adapter.clj:142-148). But `foo#2` is a legal symbol
(verified: compiles + evaluates). A blob with a duplicated `foo` (2nd → block-path
`foo#2`) AND a literal `(def foo#2 …)` (block-path `foo#2`) yields TWO units with
the identical block-path → identical `derived-unit-id`
(clojure_adapter.clj:264-266). **Reproduced against the real adapter:**
```
BLOCK-PATHS: [ns foo foo#2 foo#2]      UNIT-IDS distinct? false
IDS: [… du:OK:clojure-form-v0:foo  du:OK:clojure-form-v0:foo#2  du:OK:clojure-form-v0:foo#2]
```
Consequences, all silent, all untested: (a) `source-materialization` `unit-rows`
is a `mapv` (clojure_adapter.clj:375-393) → two DerivedUnitRow share one unit-id →
the kernel keys by unit-id → the second OVERWRITES the first (one form's text /
hash / anchor lost); (b) `cut-named-units` folds `(into {} …)` keyed by block-path
(code_atoms.clj:172-177) → one `foo#2` shadows the other → lineage tracks only one,
so the other's supersession is missed or misattributed. G2 reassembly still passes
(spans still tile; the collision is in IDENTITY, not offsets), so no gate catches
it. The R7 "block-path uniqueness is PER BLOB" claim (CONTRACT §7) has this hole.
**Merges / duplicate rids → CANNOT-HAPPEN:** `-m` per-parent blocks give one
`(parent,commit)` grain; same-rid edges from two blocks are deduped by `seen-edges`
(code_atoms.clj:368) intra-sync and by the stable journal key inter-sync;
re-assert at a different commit hits `edge-already-asserted?` (:369) → counted 0.
count-deltas on reassert = {total 0, asserted 0} (relation_kernel.clj:346-348) —
descriptor counts don't double.
**Rename+modify / var-deleted-then-different-var-same-name → COUNTED honestly:**
`--no-renames` (code_atoms.clj:90) makes a move delete+add; block-level break
matching is HASH-exact (code_atoms.clj:280-283), so a modified move mismatches →
`:unmatched-vanished`/`:unmatched-appeared` (counted, never a fake edge).
**Positional `%06d` drift → CANNOT-HAPPEN cross-blob:** lineage tracks named
units only (code_atoms.clj:174 `:when (some? name)`); the analyzer recomputes
per-blob. No cross-blob logic assumes positional stability.
**`(defn ns …)` vs `(ns …)` → CANNOT-HAPPEN:** both dedup through the SHARED
`seen` map (clojure_adapter.clj:150-162), so the second gets `ns#2`.

### Class 4 — Analyzer honesty → verified honest (one coverage gap)
Evidence anchors map a usage `:row` to its enclosing top-level unit by
LINE-START offset (code_atoms.clj:549-561), robust to first/last-line call sites
(form starts at col 0 so `start-offset == line-start`; a gap/comment row →
`nil` → SKIPPED + counted in `:usages-unmapped`, code_atoms.clj:623-624). Anchors
use the block-path (unit identity) not the bare-name continuant, so a defmethod
call site resolves through the same naming law the adapter stored. The skip
counter EXISTS and I confirmed it non-zero on the whole tree (279, honest). The
**continuant merge** of two same-named defs into one `:calls` edge with an
arbitrary representative anchor is real but BY DESIGN (bare-name grain, T6) and
acknowledged (P3 doubt 3). **Coverage gap (not a break):** the `:calls`
skip-count invariant is gated only at value 0 (G7 specimen); the non-zero path is
proven by my whole-tree run, not by CI.

### Class 5 — Determinism / clocks → 1 CONFIRMED divergence, stored-data clean
**Constant-HEAD reassert stat/truth divergence (CONFIRMED-BREAK):** the assert
key is `code:<rid>:<head>` (code_atoms.clj:674) — HEAD-scoped, NOT
status-generation-scoped. At a FIXED head, if an analyzer edge is retracted
(key `…:<head>:retract`) then the call reappears in `desired`, the reconcile puts
it in `to-assert` and re-appends key `code:<rid>:<head>` — which the journal
ALREADY holds from the ORIGINAL assert → `(filter> (nil? *prior-decision))`
(relation_kernel.clj:679) drops it → **the edge STAYS `:retracted`**, while the
reconcile reports `:calls-asserted +1` (code_atoms.clj:694). Optimistic count vs
materialized truth. Trigger needs a fixed HEAD with retract-then-reappear (kondo
nondeterminism, a `path-filter` change, or a hand override) — production HEADs
move on real edits, giving a fresh key, so it's rare. Acknowledged (P3 doubt 1).
**min-committer-ms → deterministic:** `(update :min-ms min committer-ms)`
(code_atoms.clj:223) is order-independent. **No stored wall clock:** committer
clocks throughout; `now-ms`/`pass-started-ms` only classify, never persist. G9
green.

### Class 6 — Deny-list reach → 1 CONFIRMED granularity mismatch (moot today)
**Analyzer lane → DEFENDED:** `head-code-blobs` excludes denied paths
(code_atoms.clj:470-471) BEFORE `materialize-head-tree!` cat-files only
`path->sha` (:486-489) — a denied path's bytes never reach kondo's temp tree.
**Lineage vs ingest granularity mismatch (CONFIRMED, moot for env.clj):**
`enumerate-blobs` fail-closes a blob denied at ANY path (code_atoms.clj:223-224),
but `commit-lineage` denies per CHANGE-path (code_atoms.clj:241). A blob denied at
path X but changed at ALLOWED path Y is EXCLUDED from ingest yet CUT by lineage
via Y → its text IS cat-filed (T10 "deny before text leaves git" broken for that
blob) and lineage mints edges at un-ingested unit-ids. Reachable via a git-TRACKED
deny entry renamed to an allowed path (rename preserves blob sha; the delete side
has null new-sha so it never re-flags the blob). Moot NOW: env.clj is gitignored,
never a git blob (P2 Deviations). `/mnt` vs `/home` root aliasing is a non-issue —
deny matches repo-relative paths from `ls-tree` (clojure_adapter.clj:38-46).

### Class 7 — Test masking → verified sound (one deviation, one unasserted pin)
Negatives read PHYSICAL PStates where a dedup could mask: `read-source`
(runtime.clj:133-135 `foreign-one`) for the denied-blob-absent check,
`read-relation-row` (relation_kernel.clj:968-970 `foreign-select`) for G5 edge
presence/absence, `physical-derived-units-for-source` (adapter test:104-106) for
the no-duplication check. The query-API negatives (G6/G7) are sound because the
edges they'd catch are always visible (mech edges are never retracted; a wrong
`:calls` edge would be stored under its target and surfaced with
`include-retracted? true`). **Two IPC launches per ns** (`code-atoms-lineage-gates`
:83 + `analyzer-gates` :301) deviates from the one-launch rule, but they are
SEQUENTIAL with try/finally close each (:213, :436) and empirically coexist (suite
green). **Pins asserted nowhere:** the whole-tree numbers (458/5073/279/50) live
only in P3 prose — no CI gate; I reproduced them exact, so honest-but-ungated.

### Class 8 — Kernel contract fit → DEFENDED
The import request passes `import-request-validation-errors`
(object_container.clj:705-795): partition-key/import-key/object-key/idempotency-
key/fingerprint all present (clojure_adapter.clj:602-609); the source-hash
mismatch check is `:markdown`-ONLY (object_container.clj:724-733), so `:clojure`
sails through (the adapter computes the hash correctly regardless). Target-keys
are non-blank for every code endpoint: `->target-ref` falls through to verbatim
`(str target-id)` for `:code-form`/`:var`/`:ns` (relation_kernel.clj:848), and
target-ids are `du:…` unit-ids / `ns/name` continuants / ns strings — never
nil/blank (nil `:from-var` and non-app namespaces are dropped upstream,
code_atoms.clj:614-616). No trap-8 global hotspot. Nil evidence fields are
accepted (validation ignores them; G10 override edges carry nil evidence and land).

---

## Writers / readers / clearers per changed state

**Object-container PStates** (`$$source-artifacts-by-id`, `$$containers-by-id`,
`$$derived-units-by-id`, `$$source-derived-units-by-source`,
`$$source-anchors-by-target`, revisions/versions/composition-edges):
- WRITER: `ocr/append-object-container-request!` with the adapter's
  import-material request (code_atoms.clj:350). Keyed by object-key / unit-id.
- READER: `ocr/read-source`, `read-unit`, `read-common-material-for-source`,
  `read-source-anchors` (analyzer evidence joins + tests).
- CLEARER: **none** — material is append-only; a superseded blob's rows are
  retained inert. RE-WRITE converges by import-key (idempotent). **The one hazard:
  the `#N` collision makes two units contend for ONE unit-id → last-write-wins
  overwrite with no clearer and no error.**

**Relation-kernel PStates** (`$$relations-by-id`, `$$relations-by-target`,
`$$relation-target-descriptors`, status-log, events, decisions):
- WRITER: `rk/append-relation-request!` — asserts by `import:code-lineage`
  (`:supersedes`) and `import:code-analyzer` (`:requires`/`:calls`); both
  VERSION-FREE (code_atoms.clj:42-48, 428-431).
- READER: `edge-already-asserted?` pre-check (code_atoms.clj:195-196);
  `read-relations-for-targets` in reconcile (code_atoms.clj:664).
- CLEARER: the reconcile RETRACT path (code_atoms.clj:684-692) flips analyzer
  edges to `:retracted` — but ONLY edges whose endpoints stay in the desired set
  (the Class-5/stale-edge gap). Lineage `:supersedes` edges are NEVER cleared
  (append-only history). Human edges: different asserter → different rid → the
  analyzer never writes or clears them (the Class-1 defense).

**Per-sync mutable state:** `text-cache`/`cut-cache`/`seen-edges` atoms
(code_atoms.clj:326/330/359), single-threaded within a sync; stat counts are
`reduce` returns. No volatiles, no cross-thread sharing, no clearer needed
(GC'd with the call).

## Async ordering risks
- append-ALL/await-ALL: a per-blob timeout (nil) silently drops the blob from
  accounting; lineage proceeds regardless → dangling edges (Class 2).
- `edge-already-asserted?` races the `:append-ack` microbatch lag, but the STABLE
  journal key makes it a cost-guard only (correctness lives in the journal +
  `seen-edges`).
- reconcile is read-then-write with no lock; `analyzer-sync!` resolves HEAD ONCE
  (code_atoms.clj:714) and threads that snapshot, so an A-starts/B-starts/A-late
  interleave at different HEADs uses different keys and cannot clobber. Same HEAD
  → identical keys → journal exactly-once. `transition-row` preserves
  `first-asserted-at-ms` (relation_kernel.clj:360-367), so an older reassert
  cannot rewrite the sort-key of a newer row.

## Error-path cleanup
- **git non-zero exit swallowed (CONFIRMED, low):** `git-bytes` discards the
  `.waitFor` result and returns stdout regardless (code_atoms.clj:60-71). A failed
  `cat-file` (bad sha, corrupt repo) → empty bytes → `blob-text ""` → 0-unit cut,
  NO exception. Happy path is safe (shas come from git's own `read-code-log`);
  latent robustness gap only.
- **kondo failure:** `run-analysis` throws → `analyzer-sync!` propagates before any
  edge is appended (derive precedes reconcile) → no partial writes. But the temp
  dirs from `materialize-head-tree!` (code_atoms.clj:485) and
  `build-kondo-config-dir!` (:501) are `createTempDirectory` with NO deletion →
  they accumulate per analyzer run (resource leak, low).
- **ingest timeout:** nil decision, silently uncounted (Class 2).
- **retraction rights:** every reconcile retract sets `:actor` = the analyzer =
  the stored asserter (code_atoms.clj:690), so the rights guard
  (relation_kernel.clj:445-451) passes — no self-inflicted rejections.

## Open doubts (each with its cheap falsifier)
1. **Stale-edge on component deletion** (the top break). *Falsifier:* `code-sync!`
   + `analyzer-sync!` a HEAD; then delete a self-contained file whose helpers are
   called only within it, re-`analyzer-sync!` the new HEAD; assert those `:calls`
   edges are STILL `:asserted` (never read, never retracted — code_atoms.clj:663).
2. **`#N` block-path collision.** *Falsifier:* DONE — reproduced (`foo#2` twice →
   one unit-id). Confirm downstream loss: import a blob with dup `foo` + literal
   `(def foo#2 …)` and read `$$derived-units-by-id` count < form count.
3. **Constant-HEAD reassert divergence.** *Falsifier:* at a fixed head, override
   D1 asserts A→B, D2 drops it (retract), D3 re-adds A→B; observe
   `:calls-asserted 1` on D3 but `read-relation-row` still `:retracted`.
4. **Deny granularity (ingest vs lineage).** *Falsifier:* add a git-tracked path to
   the deny-list, commit a rename of it to an allowed path, `code-sync!`; observe
   the blob cut/ingested via the allowed path (its bytes cat-filed).
5. **git exit-code swallowed.** *Falsifier:* `(cut-named-units "deadbeef…" (blob-text
   repo "deadbeef…"))` returns `{:units {} …}`, not an error.
6. **Temp-dir accumulation.** *Falsifier:* run `analyzer-sync!` N times, `ls
   /tmp/code-atoms-head* /tmp/code-atoms-kondo-cfg*` grows (kondo-cfg is `defonce`,
   so 1; head-trees are N).

---

## VERDICT

**6 CONFIRMED breaks, 2 PLAUSIBLE, the rest defended.** No stop-clause; the
package's happy-path math is correct and every pinned gate reproduces (adapter
7/78, driver+analyzer 5/164, whole-tree 458/5073/279/50 exact). The breaks are
all seam/optimistic-state gaps, all recordable pre-close, none blocking.

CONFIRMED: (1) reconcile stale-edge leak on whole-component deletion —
code_atoms.clj:663-668, violates R5, map lies persistently; (2) block-path `#N`
collision → duplicate unit-id / silent identity loss — clojure_adapter.clj:142-162
(reproduced, compiler-legal, untested); (3) constant-HEAD reassert-after-retract
stat/truth divergence — code_atoms.clj:674 vs journal; (4) ingest-timeout blob
silently uncounted (no `:blobs-failed`) — code_atoms.clj:354 + runtime.clj:326;
(5) `git-bytes` swallows non-zero exit → silent empty blob — code_atoms.clj:70;
(6) deny-granularity mismatch ingest-vs-lineage — code_atoms.clj:223 vs :241
(moot for env.clj today).
PLAUSIBLE: temp-dir leak per analyzer run (code_atoms.clj:485/501); `:calls`
skip-count gated only at 0.
DEFENDED (CANNOT-HAPPEN): human-edge retraction (asserter filter :666 +
asserter-scoped identity); concurrent-sync store corruption; stored wall-clock;
analyzer deny-before-catfile; kernel validation fit; merge/duplicate-rid double
transitions.

**Top-3 by severity:**
1. **Stale-edge leak on deletion** (code_atoms.clj:663-668) — persistent false
   `:calls`/`:requires` edges after a self-contained file is deleted; the "map must
   not lie" invariant. Acknowledged P3-2; plausible trigger.
2. **`#N` block-path collision** (clojure_adapter.clj:142-162) — silent unit-id
   collapse → lost DerivedUnitRow + lineage miscount; novel/unacknowledged,
   untested; low likelihood (needs a `x#N`-named var).
3. **Constant-HEAD reassert divergence** (code_atoms.clj:674) — optimistic
   `:calls-asserted` count vs a `:retracted` truth. Acknowledged P3-1; rare.
