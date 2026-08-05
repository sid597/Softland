# DIFF FALSIFICATION — t4-spine working-tree diff vs HEAD (9552aa4)

Falsification pass (default-fail). A class HELDs only where a concrete break was
attempted and shown to be unreachable. Read against the current source, not just
the diff hunks. No repo file modified; env.clj never read. The serial suite was
running in another JVM during this pass — analysis + pure-function probes only,
no Rama cluster spun.

Scope: `git_spine.clj`, `trail_view.clj` (feed), `ingest_watchers.clj`,
`object_container.clj` (additive row field), `markdown_adapter.clj` (2nd ctor),
+ the two test namespaces.

**Bottom line: no BLOCKER. 1 SHOULD-FIX (a resilience regression the report
mislabels as equivalent), 6 DOUBTs (5 latent/out-of-scope, 1 pre-existing).**

---

## Per-class verdicts

### Class 1 — Fence carve-out custody · **HELD**

The additive trailing field `claimed-at-ms` on `SourceIngestCompletionRow`
(object_container.clj:97) is well-custodied.

- **Arity companions correct.** Record is now 11 positional fields. Both
  `->SourceIngestCompletionRow` sites pass exactly 11 args, verified by counting:
  - object_container.clj:604-615 (LIVE) — 11 args, last = `(:claimed/at-ms request)`.
  - markdown_adapter.clj:227-239 (DEAD) — 11 args, last = `(:claimed/at-ms request)`.
- **No positional destructuring anywhere.** Grep of `src/` + `test/` for
  `SourceIngestCompletionRow` / `->SourceIngestCompletionRow` /
  `map->SourceIngestCompletionRow` finds only: the `defrecord`, the two ctor
  sites, the PState schema type at 1712, and the topology write at 2122 (which
  calls the `source-ingest-completion-row` builder, not positional). Every read
  is keyword-based (`:claimed-at-ms`, `:completed-at-ms`, `:source-ref`,
  `:document-container-id`, `:derived-unit-count`). Adding a **trailing** field
  cannot shift keyword access. No `let [[a b …] row]` positional binding exists.
  A trailing add is the one safe shape and this is that shape.
- **Dead row truly dead.** `materialization-completion-row` (markdown_adapter.clj:372)
  has **zero callers** (grep, verbatim in probe ledger). Its only feeder,
  `:completion-row` in the materialization map (357), is read nowhere else. The
  live feed reader (`source-completion-ms` → `:completed-at-ms`, and
  `:claimed-at-ms` in `source-activity-entry`) consumes the LIVE row written by
  the topology at 2122 via the object_container builder — never the adapter row.
- **Live claim reaches the feed correctly.** Topology `*request` is bound once
  from the depot source at object_container.clj:1779 and used at 2122; for a
  commit that request is git_spine's `assoc`-augmented request carrying
  `:claimed/at-ms` = committer clock. So the LIVE completion row gets the real
  claim; md requests carry no key → nil.

**Strongest break attempted (→ DOUBT #1):** durable-cluster serialization. A
defrecord serialized positionally (Nippy/Fressian) breaks deserialization of
pre-existing rows when a field is appended across a deploy. HELD only because the
runtime is ephemeral per-JVM IPC — each boot starts empty, no old-format rows
coexist with the new class. Correct for today; a latent trap the moment the
cluster becomes durable. Named, not defended.

### Class 2 — claimed-at-ms replay determinism · **HELD**

No wall clock enters the topology through the new key.

- `claimed = (:committed-at-ms commit)` (git_spine.clj:201) — committer time is a
  fixed git fact, byte-identical across runs (this is why G1 request-equality
  stays green: the extra key has the same value on every run).
- The `(now-ms)` default lives ONLY on `:request/time-ms` (core.clj) and on
  `:completed-at-ms` (object_container.clj:611). `claimed-at-ms` reads
  `:claimed/at-ms`, a key **only** git_spine sets. An md request built WITHOUT
  the key → `(:claimed/at-ms request)` = nil, all the way to `:time/claimed-ms`
  = nil in the feed. Exactly nil, verified end-to-end (trail_view.clj:594 reads
  `(:claimed-at-ms row)`; a record built with nil in that slot returns nil).
- **Retry-idempotent.** The completion-row PState write (topology 2127-2130) is a
  `local-transform>` keyed `[source-ref-key source-version-key]` with value
  derived deterministically from `*request` (same claimed on any `:all-after`
  replay). No OS side effect, no ack/proxy, no fingerprint participation:
  `material/fingerprint` is computed inside `markdown-source-import-request`
  (markdown_adapter.clj:482) BEFORE git_spine's outer `assoc`, from
  `(object-key import-key payload)` — `:claimed/at-ms` is a top-level key the
  fingerprint never sees. So claimed cannot cause a false conflict, and a
  converged re-import (prior-audit branch, 1788-1789) does not rewrite the row —
  first-writer's claimed stands, and for deterministic commits it is the same
  value anyway.

### Class 3 — fresh/converged split truth · **HELD** (1 residual DOUBT)

Read `spine-sync!` (git_spine.clj:251-294) and the decision machinery in full.

- **Can a fresh ingest print `:converged`?** No. `converged?` is
  `(or (some? replayed-from) (< decided-at-ms pass-started-ms))`. A first-pass
  fresh accept stamps `decided-at-ms = (core/now-ms)` at audit time
  (accepted-decision-row, oc:433), which is captured AFTER `pass-started-ms`
  (git_spine.clj:252, before read-commits/append/await). So
  `decided-at-ms >= pass-started-ms` → NOT `<` → fresh. The granularity edge
  `decided-at-ms == pass-started-ms` yields `<` false → fresh, which is the
  CORRECT label. `replayed-from` is nil on fresh accepts.
- **Can a converged one print `:fresh`?** No, for spine's usage. spine requests
  are byte-identical across runs (G1), so a 2nd pass hits the prior-AUDIT branch
  (object_container.clj:1785-1789): `(<<if (some? *prior-audit-decision)
  (ack-return> *prior-audit-decision) …)` returns the ORIGINAL decision
  **untouched** — its `decided-at-ms` is pass-1 wall time, strictly `<` pass-2
  `pass-started-ms` (a real 2nd pass shells out to git again → many ms later).
  Verified in source that this branch does NOT restamp decided-at-ms and does NOT
  set replayed-from. Converged detected via the time clause. (A same-pass replay
  from an idempotency collision restamps decided-at-ms to `now` but sets
  replayed-from → still converged via the first clause. Two-clause OR is
  necessary and sufficient.)
- **No two spine commits collide on idempotency in one pass** → no first-pass
  replay masquerading. idempotency-key = import-key =
  `f(object-key, source-ref-key, source-hash)`; distinct shas → distinct
  `git-commit:<sha>` source-refs → distinct object-keys → distinct keys
  (markdown_adapter.clj:475-480). So first pass is all-fresh (test r1 asserts
  fresh=commits, converged=0), matching the code.
- **Buckets partition.** Import decisions are only `:accepted` or `:rejected`
  (conflict = a rejected variant, oc:456-459); await returns `nil` on timeout
  (runtime.clj:326). `fresh+converged = |accepted|`, `rejected = |rejected|`,
  `unresolved = |nil|` → sum = commits. `:ingested` keeps its G2 meaning
  (count of accepted). No third status leaks a decision out of all buckets.

**Residual DOUBT #4 (stat-cosmetic):** `System/currentTimeMillis` is not
monotonic. A backward NTP **step** between `pass-started-ms` capture and a fresh
accept's `decided-at-ms` within the same pass flips that fresh commit to
`:converged` in the printed stats. The ingest/edge is unaffected — label only.
Report doubt 4 covers cross-cluster skew; this is the intra-process variant.

### Class 4 — run-level dedup under-count · **HELD for collapse; SHOULD-FIX on healing**

- **No two distinct edges collapse to one key.** `seen-sha` holds
  `[session-id sha]`, `seen-doc` holds `[session-id watch-path]` — **separate
  volatiles**, so a sha and a path can never collide even with equal string
  values. Within `seen-sha`, two different shas → different keys → two edges (and
  distinct shas map to distinct `sha->doc` doc-ids: source-ref carries the sha).
  Within `seen-doc`, two different watch-paths → different keys. Two different
  sessions → different keys → separate edges (correct: relation-id is
  conversation=session scoped). The `[session-id x]` key **exactly mirrors** the
  conversation-scoped relation-id identity, so the count now equals the number of
  distinct edges — the ~300× over-count is genuinely removed without collapsing
  real edges. Two paths that rebase to the SAME watch-path DO collapse — correct,
  they are the same file (same canonical).

- **SHOULD-FIX — mid-append failure suppresses siblings *this boot*, and the
  report's "same healing shape" claim is inaccurate.** In `process-jsonl-file!`
  the key is marked seen BEFORE the append (git_spine.clj:465-466 and 476-477):
  `vswap! seen conj key` then `assert-edge!`. If `assert-edge!` throws after the
  mark (transient append/IPC error), the exception propagates, extract's per-file
  catch (558-564) marks the file failed, and — because `seen-sha`/`seen-doc` are
  now **run-level** volatiles shared across files — every sibling file of the
  same session **skips** that key → the edge is absent for the whole boot. The
  OLD per-file volatiles healed this WITHIN the same boot (a sibling file
  re-emitted independently); run-level dedup heals only on the NEXT boot's full
  reprocess. Report doubt 1 says "Same healing shape the per-file code had" —
  that equivalence is false for the within-boot window. Trivial fix that keeps
  the cost win: move `vswap! seen conj key` to AFTER `assert-edge!` returns
  normally (it returns, not throws, on the already-asserted pre-check, and the
  doseq is sequential so no duplicate slips in before the mark) — on an exception
  the key stays unmarked and a sibling retries same-boot. Non-blocking because the
  next-boot reprocess is genuine (cluster ephemeral, corpus fully re-read), but
  the claim in the report should be corrected either way.

### Class 5 — canonical-under-roots edge cases · **HELD** (3 latent DOUBTs)

Probed the exact rebase arithmetic against real symlinks (ledger below). Findings:

- Alias→real and real→alias BOTH join (rebase produces the textual form rooted at
  the cfg root's prefix; the md watcher mints `.getPath` under the SAME cfg root
  — ingest_watchers.clj:73 — so the strings match). Both directions confirmed.
- Out-of-root file → nil; symlinked file resolving OUTSIDE the roots → nil. G6
  honesty preserved: no fabricated edge, only the alias-string identity split is
  gone.
- The old bug is real: with the pre-diff code, an alias-recorded path returned the
  RAW alias string → a doc-id no watcher-minted doc matches → a dishonest dangler.
  The rebase removes exactly that.

**DOUBT #2 — trailing-slash root → double slash.** Probe: a root ending in `/`
produces `…/current-mental-model//note.md`, which would NOT match the watcher's
`.getPath`. Unreachable today — `land-doc-roots` (git_spine.clj:524) hardcodes no
trailing slash — but latent if that fn changes or a custom root is passed.

**DOUBT #3 — symlinked FILE into another root.** Probe: `vision/cross.md` →
`docs/…/note.md` rebases to the docs path, while a textual watcher following the
symlink would mint `vision/cross.md` → divergence. This is the file-granularity
concretization of report doubt 2 ("symlinked subdirectory"). None exist today
(`find -type l` clean per report); recorded, not defended.

**DOUBT #6 — `file_path` == a root dir exactly.** Probe: returns the root dir
string; `doc-document-id` would then `slurp` a directory and throw → file marked
failed. Unreachable via Edit/Write `file_path`s (always files). Minor robustness
note.

---

## Findings, ranked

**BLOCKER** — none.

**SHOULD-FIX**
- **F1 (Class 4)** git_spine.clj:465-466, 476-477 — mark-seen-BEFORE-append on a
  run-level volatile makes a transient `assert-edge!` failure suppress the edge in
  ALL same-session sibling files for the whole boot; heals only next boot, NOT
  same-boot as the old per-file code did. Report doubt 1's "same healing shape" is
  inaccurate. Fix: mark after `assert-edge!` returns normally.

**DOUBT** (ranked by latent blast radius)
- **D1 (Class 1)** object_container.clj:90/1712 — trailing field on a
  positionally-serialized defrecord is safe ONLY under ephemeral per-JVM IPC. A
  durable cluster spanning a deploy could fail to deserialize old rows into the
  11-arity class. Out of current scope; will bite at durability.
- **D5 (Class 4/5)** git_spine.clj:380-381 — `doc-document-id` mixes the path with
  the CURRENT file's content hash (`source-hash (slurp …)`). The rebase aligns the
  PATH, but the join still requires the file's content at extract time to equal
  what the watcher ingested. Content drift between watcher-ingest and extract
  re-dangles the edge despite the rebase. Pre-existing (not introduced here); the
  report's "joins hold in BOTH directions" is content-conditional.
- **D4 (Class 3)** git_spine.clj:284-286 — non-monotonic wall clock: a backward
  step within a pass can mislabel a fresh commit `:converged`. Stat-cosmetic only.
- **D2 (Class 5)** git_spine.clj:409-413 — trailing-slash root yields a
  double-slash rebase that would dangle. Not reachable via `land-doc-roots` today.
- **D3 (Class 5)** git_spine.clj:401-414 — symlinked file resolving into another
  root diverges from a textual watcher. None exist today (report doubt 2, file
  variant).
- **D6 (Class 5)** git_spine.clj:406 — `file_path` == root dir returns the dir →
  downstream slurp-of-directory throw → file failed. Unreachable via tool paths.

---

## Probes-attempted ledger

Pure-function probe of `canonical-under-roots` arithmetic, replicated in Python
(`os.path.realpath` ≈ `getCanonicalPath`; `canon[len(rc):]` ≈ `(subs canon
(count rc))`) against REAL symlinks under scratchpad. No JVM/Rama touched.

Setup (`scratchpad/setup_and_run.sh`, target `…/spine-falsify2`):
```
real/docs/current-mental-model/note.md          (real file)
real/vision/                                     (real dir)
outside/secret.md                                (real file, out of roots)
alias -> real                                    (symlink, the live dual-cwd case)
real/vision/leak.md  -> outside/secret.md        (symlinked file, out of roots)
real/vision/cross.md -> real/docs/.../note.md    (symlinked file, into other root)
```
Command: `bash …/setup_and_run.sh …/spine-falsify2`  → results (verbatim):
```
alias path, real-rooted (fwd join)     -> …/real/docs/current-mental-model/note.md
real path, alias-rooted (rev join)     -> …/alias/docs/current-mental-model/note.md
real path, real-rooted (identity)      -> …/real/docs/current-mental-model/note.md
out-of-root file                       -> None
symlink file -> OUTSIDE roots          -> None
symlink file -> OTHER root (cross)     -> …/real/docs/current-mental-model/note.md
file_path == root dir exactly          -> …/real/docs/current-mental-model
nonexistent path                       -> None
trailing-slash root form               -> …/real/docs/current-mental-model//note.md   <-- DOUBT D2
dot-segment in input                   -> …/real/docs/current-mental-model/note.md    (./ normalized away)
```
Interpretation: fwd+rev alias joins produce the textual form rooted at the cfg
root (matches watcher `.getPath`); honesty gates (out-of-root, symlink-outside,
nonexistent) return nil (no edge); the double-slash and cross-root-symlink rows
substantiate D2/D3; the root-dir row substantiates D6.

Grep probes (verbatim):
- `grep -rn "SourceIngestCompletionRow|->SourceIngestCompletionRow|map->…|source-ingest-completion-row|materialization-completion-row" src/ test/` → only the defrecord, 2 ctor sites, PState schema (1712), topology builder call (2122-2129), and the dead accessor (372). No positional destructuring, no `map->`, no 10-arity ctor.
- `grep -rn "materialization-completion-row" src/ test/ | grep -v defn` → **empty** (zero callers; dead row confirmed).
- `grep -rn ":claimed/at-ms|:claimed-at-ms|claimed-at-ms" src/ test/` → writers: git_spine:201 (set), oc:615 + md_adapter:239 (copy to row); readers: trail_view:594 (feed). No stray reader of `:request/time-ms`-as-claim.

**UNEXECUTED (needs a Rama cluster; suite was running):**
- Report falsifier D1/doubt1: kill extraction mid-file, assert the edge is present
  after a second boot. Steps: run `extract-session-joins!` with two same-session
  jsonl files where `assert-edge!` is stubbed to throw on file-a's first append;
  assert edge absent post-run; re-run with a clean stub; assert edge present. This
  directly exercises F1's within-boot gap and the next-boot heal.
- Report doubt 3: a slow boot where a decision lands after the 20 s await window,
  to observe `:unresolved` then a later `:rejected`. Needs a throttled cluster.
- Full serial suite receipts (git-spine-test 7/167, the 13/295 serial group) — the
  main session owns these; not re-run here to avoid JVM contention.

---

## The six report doubts — attempted-falsifier results

1. **Mid-append failure vs run-level dedup.** Falsifier UNEXECUTED (needs
   cluster). Static read CONFIRMS the gap AND corrects the report: the mark
   precedes the append on a run-level volatile, so a sibling file skips the key
   and the edge is absent *for the whole boot* — the per-file code healed
   same-boot, so "same healing shape" is inaccurate. Next-boot heal is real
   (ephemeral cluster, full reprocess). → SHOULD-FIX F1.
2. **Rebase covers root-level aliases only.** Falsifier EXECUTED at file
   granularity (`vision/cross.md`): a symlinked file into another root rebases to
   the target root and diverges from a textual watcher. Confirms the doubt and
   sharpens it from "subdirectory" to "any symlinked path"; none exist today. →
   DOUBT D3.
3. **`:unresolved` is an unknown, not an outcome.** Not independently falsifiable
   without a slow cluster (UNEXECUTED). Static read agrees it is honest by
   construction: `unresolved = |nil decisions|`, disjoint from accepted/rejected;
   a late-landing rejection is simply unknown that pass. No stat lies, it just
   under-resolves. Accept as designed.
4. **`pass-started-ms` assumes one clock.** In-process the JVM clock is shared, so
   cross-node skew is moot today (agreed). Added intra-process failure mode: a
   backward wall-clock STEP within one pass mislabels fresh→converged
   (stat-only). → DOUBT D4.
5. **Adapter's dead completion row reads legacy `:claimed/at-ms` (nil).**
   EXECUTED by source read: `source-materialization` is called with
   `legacy-request` (md_adapter:470), and git_spine `assoc`s `:claimed/at-ms` onto
   the OUTER request only — so line 239 reads nil even for commits. Confirmed
   harmless: the accessor has zero callers (grep). The doubt is exactly right and
   must be threaded if anyone wires the accessor.
6. **Claimed coverage is commits-only.** Confirmed by construction — only
   git_spine sets `:claimed/at-ms`; transcript-conversation source rows read
   claimed-nil at the source-ingested branch. A future additive use of the same
   key, not a defect. No falsifier needed.

---

## Verdict

The t4-spine diff HOLDS under falsification: no BLOCKER. The additive
`claimed-at-ms` carve-out is correctly custodied — trailing field, both ctor
arities updated, no positional consumer, the live topology row reads the real
committer clock from the full depot request while md stays exactly nil-honest,
and the field is inert to fingerprint/routing/retry (deterministic from the
commit, idempotent on replay). The fresh/converged split is sound for spine's
G1-deterministic requests (fresh accepts stamp `decided-at-ms >= pass-started`;
converged re-runs return the untouched prior decision with an earlier
`decided-at-ms`), and the run-level dedup key exactly mirrors the
conversation-scoped relation-id so no two distinct edges collapse. The one
correction the diff owes its own report is F1: run-level dedup with a
mark-before-append on a shared volatile does NOT heal a mid-append failure
same-boot the way the per-file code did — it heals only on the next boot's full
reprocess; a one-line reorder restores same-boot resilience without losing the
~300× count fix. The remaining items are latent (durable-cluster record
serialization D1, trailing-slash/cross-root-symlink path edges D2/D3),
pre-existing (content-hash join dependency D5), or stat-cosmetic (non-monotonic
clock D4) — all non-blocking, but D1 and D5 should be carried forward as named
traps because they will bite the moment the cluster becomes durable or a watched
file's content drifts between ingest and extract.
