# Phase P2 — the driver + lineage lane (`code_atoms.clj`)

2026-07-09 · code-atom work package · implementation phase (fresh context, Opus) ·
CONTRACT §12 P2: "driver, lineage lane (`code_atoms.clj` + test): blob sync + R3
lineage → G5, G6, G9 green." Built + gated GREEN in this same context. Parse tool,
enumeration, and analysis were ruled in `P0_PARSE_SPIKE.md`; the adapter it consumes
was built in `PHASE_P1.md`. No git commits (orchestrator commits); no code edited
outside the file allowlist; `relation_kernel.clj` NOT touched (`:supersedes` is
already registered — P3 owns the registry edit).

## STOP-CLAUSE STATUS: NONE TRIGGERED

No §9 stop-clause fired. rewrite-clj/B-full/the adapter API were all sufficient; no
allowlist breach; no binding-doc conflict; `env.clj` never opened (its path never even
appears in git history — see Deviations). One BUILD-SPEC anticipation ("if the split
reflowed the moved text so hash-exact matches are few…") did NOT come to pass — the
derivation shows **45** real hash-exact silver moves, so G6 asserts silver DOES surface
(stronger than the fallback). Details below.

## Files created / touched

| file | status | what |
|---|---|---|
| `src/app/server/rama/code_atoms.clj` | NEW | driver: git blob reading, B-full enumeration, deny-filter, batch ingest, pure `commit-lineage` (R3 three-outcome law), `code-sync!` |
| `test/app/server/rama/code_atoms_test.clj` | NEW | G5/G6/G9 IPC gates + R6/T10 deny + enumeration oracle + synthetic silver-lane; ONE deftest family, ONE IPC launch |
| `docs/.../code-atom/PHASE_P2.md` | NEW | this artifact |

No fixtures were needed (the specimen IS this repo's real history; the synthetic silver
test uses in-memory strings). No `deps.edn` change (rewrite-clj already present from P1).
No adapter/relation-kernel/git_spine edit. `env.clj` never opened.

## Design decisions inside the CONTRACT's freedom (trap citations)

1. **One B-full pass feeds BOTH lanes (P0 §c).** `code-log-args` =
   `git log --all --full-history -m --raw --no-abbrev --no-renames --format=%x1e%H%x1f%ct%x1f%P -- src test`.
   Machine format: RS(`0x1e`) starts each record, US(`0x1f`) separates
   `<sha> <committer-epoch> <parents>`, then the `--raw` lines. `-m` **repeats the
   header once per non-empty parent-diff**, so a merge yields one block per parent —
   exactly R3's `(parent-commit, commit)` grain. **The parent sha is never needed as a
   value**: each `--raw` line's own `old-sha`/`new-sha` already encode the
   against-that-parent lineage, and the mech/silver edges are keyed on child-sha +
   unit-ids, never the parent. `--full-history -m` are load-bearing (P0 recovered 895
   blobs == the ls-tree oracle; naive `git log --raw` drops 2). **Verified in-test**
   (`enumeration-matches-ls-tree-oracle`): the driver's post-image set == the oracle, 895/895.

2. **Blob text from `git cat-file blob <sha>` (T3), never the checkout.** A per-sync
   `sha→text` + `sha→cut` cache fetches/cuts each distinct blob at most once (old-of-N ==
   new-of-N-1 across a file's chain, so the cache halves work). Per-blob cat-file over
   `--batch` (BUILD SPEC sanctioned either — "just be correct"); scoped gates are instant,
   whole-repo receipt is O(distinct-blobs) processes (falsifier below).

3. **Deny by PATH before cat-file (R6/T10).** `enumerate-blobs` marks a blob denied from
   its path alone (fail-closed: denied at ANY path → denied); denied shas are excluded
   from the ingest set BEFORE `text-of` is ever called, and `commit-lineage` filters denied
   paths before any cut. Proven in-test: a denied blob's OC source row is ABSENT (its text
   never left git). `deny-list-override` (tests only) swaps the adapter's real deny-list.

4. **Lineage tracks NAMED units only** (`:name` non-nil — def-family + ns + electric-fn).
   Unnamed positional forms (`:clj/other`, reader-conds, rich-comments) have drifting
   `%06d` block-paths (PHASE_P1 doubt 3), so including them would fake supersessions on a
   top-level insert (T2). Named-unit block-paths are name-derived and re-cut-stable, so
   matching by block-path within a file is safe.

5. **`commit-lineage` is PURE** (given `cut-fn` + `deny?`) → `{:mech-edges :silver-edges
   :re-addressed :unmatched-vanished :unmatched-appeared}`. The whole SPEC §4.3 law with no
   I/O, so the break→silver lane is provable on synthetic in-memory blobs
   (`silver-lane-on-synthetic-move`). `code-sync!` executes the plan's edges via the
   git_spine `assert-edge!` idiom and counts APPENDED (a re-run appends 0).

6. **Version-free asserter (R4/T1); version in `note`.** `import:code-lineage` / `:import`;
   mech note `clj-atoms-v1|mech|<child-sha>`, silver note
   `clj-atoms-v1|silver-move|hash-exact|<child-sha>`. Idempotency + request key
   `code:<relation-id>:<child-sha>` (RELATION-scoped, git_spine trap-4 pattern) + a
   `read-relation-detail` pre-check cost guard + a run-level `seen-edges` set (honest
   counts). `:code-form` target-kind → `rk/->target-ref` verbatim fallthrough
   (relation_kernel.clj:840-846, verified), so target-key == unit-id.

7. **All times from commit clocks (T4).** Ingest `:time-ms`/`:claimed/at-ms` = the blob's
   FIRST-seen committer ms (min over commits — order-independent). Edge `asserted-at-ms` /
   `sent-at-ms` = the child commit's committer ms → lands as the edge's
   `first-asserted-at-ms` (relation_kernel.clj:367,421). Byte-identical re-runs (G9).

8. **Batch ingest (T9).** Append ALL import requests, then await ALL decisions
   (git_spine.clj:244-256) — never a serial 5s await per blob. Fresh-vs-converged split via
   git_spine's convergence test (`:replayed-from-decision-id` OR `decided-at-ms < pass-start`).

## Pinned ground-truth derivations (verbatim; == the driver's pure `commit-lineage`)

Derived ONCE at phase time from the REAL adapter cut (`clojure-form-v0`) + `oc/source-hash`
over the git blobs named in each commit's `--raw` lines, keyed on block-path for NAMED
units. Cross-checked: a standalone reduce over `ca/commit-lineage` reproduces every count
below, and the IPC gates assert the full stat maps.

### Specimen commits (`git log -1 --format="%H %ct" <short>`)
```
2796044 = 27960446538dca556565d35b1d052139190c8f50   (parent 052d9b5; A: relation_kernel.clj born)
af0e0e2 = af0e0e23b538031ac1f73bcbad52bf48cc848f6c   %ct 1783185197  (custody rows)
fd59b78 = fd59b788bc26c65bebf4906049f8121c3671af9f                   (stance kinds in registry)
63202b0 = 63202b038a0670e93f12f3757ffa426995e06f77   %ct 1783191497  (activity projection)
119f3f8 = 119f3f8586b717dd3ba6fbdfbb1989260bfb9bb9   %ct 1780942527  (split object-container adapters)
```
relation_kernel.clj blob chain (each commit's OLD sha == the previous NEW sha):
`1dfea68 (2796044/A) → 79b6ae3 (af0e0e2) → f102b76 (fd59b78) → a002c89 (63202b0)`.

### G5 filter `#{2796044 af0e0e2 fd59b78 63202b0}` — both relation_kernel.clj + _test.clj
```
2796044: mech=0 re-addressed=0   silver=0 unmatched-van=0  unmatched-app=100  (A: 89 src + 11 test named forms, all appeared)
af0e0e2: mech=7 re-addressed=93  silver=0 unmatched-van=0  unmatched-app=0
fd59b78: mech=2 re-addressed=98  silver=0 unmatched-van=0  unmatched-app=0
63202b0: mech=7 re-addressed=93  silver=0 unmatched-van=0  unmatched-app=14
TOTALS : mech=16 re-addressed=284 silver=0 unmatched-van=0 unmatched-app=114
```
**af0e0e2 `relation_kernel.clj` (SRC) superseded vars — EXACTLY 6** (`git show af0e0e2 --
src/app/server/rama/relation_kernel.clj`; the custody fields on 3 rows + the 3 fns that
write them; every OTHER named form is byte-identical 1dfea68→79b6ae3 → re-addressed):
```
RelationDecisionRow  RelationEdgeRow  RelationEventRow  rejected-decision-row  relation-outcome  transition-row
```
(The 7th af0e0e2 mech overall is the TEST file's `relation-kernel-write-and-read-test`.)
This matches the BUILD SPEC's hint list precisely. 3 sampled re-addressed vars asserted to
have ZERO edge: `relation-id-for`, `sha1-hex`, `well-formed-target?`.

### G6 filter `#{119f3f8}` — full block, all 6 changed code paths pooled
```
119f3f8: mech=18 re-addressed=309 silver=45 unmatched-vanished=21 unmatched-appeared=22
```
**45 hash-exact SILVER moves**, all `object_container.clj` (old blob `4585a02`, 289 named
units) → the 3 NEW adapter files: **34 → markdown_adapter (07d7fe7)**, **5 →
transcript_adapter (0e44c32)**, **6 → transcript_identity (e0b8a63)**. 18 within-file mech
(incl. the `ns` form of all three M-files). Note: `object_container.clj` GREW 289→223…
no — it SHRANK 289→223 named units as code moved out; `dogfood/transcript.clj`'s old blob is
`eead1c8` (NOT object_container.clj's — a swap I caught during derivation and corrected).

## Gate receipts (verbatim)

```
Testing app.server.rama.code-atoms-test
Ran 3 tests containing 78 assertions.
0 failures, 0 errors.
{:test 3, :pass 78, :fail 0, :error 0, :type :summary}
```
- **G5** — `code-atoms-lineage-gates` block 2: full stats map pinned
  (`{:commits-seen 4 :blobs-seen 8 :blobs-ingested 8 :blobs-denied 0 :blobs-converged 0
  :supersedes-mech 16 :supersedes-silver 0 :re-addressed 284 :unmatched-vanished 0
  :unmatched-appeared 114}`); all **6** af0e0e2 mech edges physically read from
  `$$relations-by-id` (kind `:supersedes`, status `:asserted`, note `clj-atoms-v1|mech|af0e0e2…`,
  asserter `import:code-lineage`, `first-asserted-at-ms` = 1783185197000); **3** re-addressed
  vars physically absent; the new blob's source-id == the edge evidence-source-id and the
  source artifact is present (dogfood join). **PASS.**
- **G6** — block 3: full stats pinned (`:supersedes-silver 45`, `:supersedes-mech 18`,
  `:re-addressed 309`, `:unmatched-vanished 21`, `:unmatched-appeared 22`, `:blobs-ingested 6`);
  3 sampled silver moves (one per adapter file) carry the `silver-move|hash-exact` marker and
  NOT `|mech|`; every edge under a moved var's unit-id is silver, never mech (no false
  cross-file mech); a genuine within-file change (object_container.clj `ns`) IS marked mech.
  **PASS.**
- **G9** — block 4: byte-identical re-run of the G5 filter → `{… :blobs-ingested 0
  :blobs-converged 8 :supersedes-mech 0 :supersedes-silver 0 :re-addressed 284 …}` (all
  converged, 0 new appends, classification deterministic); a re-derived edge's
  `first-asserted-at-ms` == the pinned committer ms, note unchanged (no wall clock). **PASS.**
- **R6/T10 deny** — block 1: denying both af0e0e2 code paths → `:blobs-denied 2
  :blobs-ingested 0 :supersedes-mech 0`; the denied blob's OC source is ABSENT (text never
  left git); no af0e0e2 edge while denied. **PASS.**
- **Enumeration oracle** — `enumeration-matches-ls-tree-oracle`: driver post-image set == the
  ls-tree union (895/895, both-way diff empty). **PASS.**
- **Synthetic silver lane** — `silver-lane-on-synthetic-move`: a fn moved VERBATIM across two
  in-memory files → exactly one silver edge (marker `silver-move|hash-exact`, from=appeared,
  to=vanished), ZERO mech. Proves the break→silver law independent of git/kernel. **PASS.**

**Regression** — P1 adapter suite still green with P2 present:
```
Testing app.server.rama.object-container.clojure-adapter-test
Ran 7 tests containing 78 assertions.
0 failures, 0 errors.
```

Invocation (BUILD SPEC):
`clojure -M:test -e "(require '[clojure.test :as t] 'app.server.rama.code-atoms-test)
(t/run-tests 'app.server.rama.code-atoms-test)"` and the same for
`'app.server.rama.object-container.clojure-adapter-test`.

## Stats from the scoped sync (live dogfood receipt)

`code-sync!` on the G5 filter against THIS repo:
```
STATS {:commits-seen 4 :blobs-seen 8 :blobs-ingested 8 :blobs-denied 0 :blobs-converged 0
       :supersedes-mech 16 :supersedes-silver 0 :re-addressed 284
       :unmatched-vanished 0 :unmatched-appeared 114}

relation-outcome supersedes chain (new-form → old-form):
  [af0e0e2] present=true kind=:supersedes note=clj-atoms-v1|mech|af0e0e2…  asserted-at=1783185197000
  [63202b0] present=true kind=:supersedes note=clj-atoms-v1|mech|63202b0…  asserted-at=1783191497000
```
(relation-outcome was superseded in af0e0e2 and 63202b0 but re-addressed in fd59b78 — its
63202b0 edge points at the fd59b78 blob's unit, the immediate predecessor, honestly.)

## Deviations from CONTRACT/SPEC (flagged loudly)

- **NONE from binding docs.** The driver conforms to CONTRACT §2/§3(R1,R3,R4,R6)/§4/§5/§7 and
  SPEC §4.3/§5.4 as written. `:supersedes` was already registered → no registry edit (correct;
  P3's job for `:requires`/`:calls`).
- **G6 silver count is 45, not "few"/0** (BUILD SPEC anticipated a reflow-to-zero fallback).
  The split moved 45 forms VERBATIM into the 3 adapter files, so G6 asserts silver edges DO
  surface — the stronger reading. Honesty note: my first spot-check reported 0 because I
  hardcoded `object_container.clj`'s old blob as `eead1c8`, which is actually
  `dogfood/transcript.clj`'s old blob (the real OC-old is `4585a02`); the faithful full-block
  reduce (which reads each `--raw` line's own shas) always said 45. Corrected before pinning.
- **`env.clj` never appears in git history** (0 commits, 0 blobs under `src/app/server/env.clj`
  — it is gitignored). So the adapter's real deny-list can never fire on this repo's history;
  the R6/T10 deny gate necessarily uses `:deny-list-override`. Not a code deviation — the
  fail-closed guard is exercised, just against a synthesized denied path.

## Open doubts + cheap falsifiers (for P3 / gate review)

1. **Per-blob `cat-file` on a whole-repo sync spawns O(distinct-blobs ≈ 895) processes.**
   Scoped gates are instant; a full sync would take tens of seconds. Falsifier: if the DoD
   whole-repo receipt is too slow, swap `blob-text` for a single `git cat-file --batch` pump —
   the `cut-named-units` / `commit-lineage` pure boundary is unchanged, so only the fetch fn moves.
2. **Silver tie-break = min unit-id when a text-hash matches multiple appeared units.**
   Deterministic, but if a helper's text is duplicated across two adapter files the move points
   at one arbitrarily. Falsifier: none observed on the specimen (all 45 moves are unique-hash);
   if a real duplicate-text move reads wrong in use (D-001), the tie-break policy is one line.
3. **Merge commits are evaluated per parent via `-m` block repetition, but NONE of the 5
   specimen commits is a merge** — merge lineage is exercised only by the enumeration oracle
   (which spans 28 merge commits and matches). Falsifier: a targeted test on a both-parent merge
   (e.g. `379114c`) asserting one block per non-empty parent — cheap to add if P3/gate wants it.
4. **Evidence anchors point at the NEW/appeared unit's `sa:<unit-id>`**, which resolves only
   after that blob is ingested. The self-contained G5 chain ingests every referenced blob; a
   partial-history sync could leave an edge whose evidence anchor's blob is un-ingested (edge
   still valid — dangling targets are legal, CONTRACT §8). Falsifier: sync a single mid-chain
   commit and confirm the edge lands with the anchor id set (present) even if the OLD blob is absent.
