# Code-Atom Work Package — RETRO

2026-07-09 · Fable close session (the package's second full D-006 cycle after
relation-kernel). Written from the FULL trail: the baton NOW log, GROUNDS →
SPEC → CONTRACT, P0_PARSE_SPIKE, PHASE_P1/P2/P3, DIFF_FALSIFICATION, FIXWAVE,
GATE_REVIEW, and the committed source (1d823bc adapter · 9a2445b driver ·
6fbfd75 registry/deps/kernel). Adversarially re-checked by a fresh subagent
(§7) before it feeds any skill or binding doc.

## 0 · Outcome

Clojure code is addressable at form grain in the land: every `.clj/.cljc/.cljs`
blob under `src/`+`test/` across all of git history stores as a surface with
derived form-units + span anchors; `:supersedes` lineage and `:requires`/`:calls`
dependency edges land in the relation kernel. Two new files over proven seams
(no module/depot/PState), one authorized registry add (`:requires :calls`), one
authorized kernel branch (`imp:clj:` routing, G-F2). Definition of done met on
THIS repo: the §12 dogfood receipt runs end-to-end (895/895 blobs, 41s sync +
19s analyzer; whole-tree 458 requires / 5073 calls / 279 unmapped / 50 residual;
census + fixed-width-order-key callers + relation-outcome supersedes chain +
G11 tuple→marks all printed). Suites GREEN at committed HEAD `a146dc8` (after the
recheck-found fix): adapter 10t/91a/0f · driver `code-atoms-test` 9t/187a/0f ·
relation-kernel 2t/222a/0f. The recheck caught a green-pre-commit / red-post-commit
staleness in `analyzer-gates` (it read HEAD dynamically but pinned ground truth from
a fixed specimen; this package's OWN commit 6fbfd75 moved HEAD's blobs off the
specimen → 7f/3e post-commit), FIXED this session in commit 381c445 by pinning the
analyzer to the specimen via `:head-override` (resolves both the nil-source and the
row-shift staleness — §7 Resolution). **T11 resolved:** the concurrent block-kernel
session landed its `:grounds :assembled-from :refines` on top of 6fbfd75 (commit
a146dc8) and re-ran its suite green — both packages' kinds now coexist on
`relation-kinds`.

## 1 · Section-A verdict — the gate-authored G-F1 fix, re-checked fresh (this session)

The GATE both found and fixed G-F1; per the QC model its fix needed one
fresh-context falsification look before this session built on it. Verdict:
**PASS — the fix is sound; no stop-clause.** Traced against every consumer of a
cut, default-fail:

- **Guard is total over the fault boundary.** `clojure-form-v0` wraps only the
  parse step (`try (parser/parse-string-all …) (catch Exception e e)`,
  clojure_adapter.clj:232-237); rewrite-clj parses eagerly, so a throw lands
  inside the try. `parsed-form-cut` (the extracted happy path) only runs on a
  parsed forms node. Blast radius = one function.
- **Ingest lane** (`code-sync!`): a broken blob's request builds with raw text +
  0 units (no throw), ingests, never aborts; `:blobs-unparseable` counts it from
  cached text (no git re-hit), DISJOINT from `:git-failures` (a git-failed blob
  returns nil from the keep, never enters `ingest-entries`).
- **Lineage lane** (`commit-lineage`): a broken side cuts to `:units {}`, so
  empty name maps → no mech edges (empty intersection), no fake supersessions.
  healthy→broken counts every old name `:unmatched-vanished` (honest — the forms
  are genuinely unaddressable in a broken blob); broken→healthy counts
  `:unmatched-appeared`. No edge bridges the gap.
- **Analyzer lane** (the opening prompt's named falsifier): a parse-failed blob at
  HEAD → `head-file-context` cuts to `:units []` → `enclosing-unit-block-path`
  over `[]` is nil → EVERY var-usage in that file → `:usages-unmapped` (never
  crashes, never guesses). Confirmed exactly as predicted.
- **Regressions bite** (verified in the test source):
  `gf1-unparseable-text-degrades-loud` (adapter) proves the cut degrades AND the
  0-unit import passes `import-request-validation-errors`;
  `gf1-unparseable-blob-degrades-loud` (driver) proves `cut-named-units` carries
  `:parse-error`; the three exact-map pins (G5/G6/G9) carry `:blobs-unparseable 0`.
- **Live-confirmed**: this session's receipt reproduced `:blobs-unparseable 5`
  and completed 895/895 end-to-end.

## 2 · QC-layer scorecard (what each layer caught / missed / cost)

The pre-registered D-006 model is: (1) contract coherence, (2) plan validation,
(3) executable gates, (4) fresh-context adversarial falsification, (5) daily use.
This package ran a spike (P0) + three build phases with gates (P1-P3) + one
adversarial diff pass + a fix wave + a Fable gate (which added the §12 receipt).

| Layer | Caught | Missed | Cost |
|---|---|---|---|
| **Contract/SPEC authoring** (Fable) | Whole design; traps T1-T11; af0e0e2 G5 ground truth; codeq/Unison prior art held honestly | **T8 fired AT authoring** — raw NULs decoded into CONTRACT.md from the Write payload; caught by the standing `file(1)` gate. The "895/895" verb ambiguity was seeded here. | 0 (T8 auto-caught) |
| **P0 spike** (parse+analysis+enum) | rewrite-clj wins (tools.reader fails `.cljc` spans + drops comments); kondo needs Rama's shipped hooks (fabrication 193→6); blob enum = `log --all --full-history -m --raw` (895/895; naive `log` silently drops 2 — flags load-bearing) | **G-F1**: "895/895" was ENUMERATION; parse-fidelity was tested on HEAD's 96 FILES only, so 5 broken HISTORICAL blobs stayed invisible. Prose miscounts (3 reader-conds, 6 ns deps). | Low; miscounts cost a P1/P2 correction each |
| **P1 adapter gates** G1-G4 | census 103, byte-exact reassembly both fixtures, two-lane, deny; corrected P0's "3 reader-conds"→10 by MEASUREMENT | G-F1 (gates run on the pinned fixture, which parses) | 0 (first-run green) |
| **P2 driver/lineage gates** G5/G6/G9 | lineage exact on real history (af0e0e2 = 6 mech, 284 re-addressed mint ZERO); 45 hash-exact silver moves across `119f3f8`, zero false mech; byte-identical re-run; corrected the CONTRACT's "6 requires"→7 by MEASUREMENT (P0 §B.1 already had 7) | G-F1 (gates sync PINNED specimen commits, all parseable) | 0 (first-run green) |
| **P3 analyzer gates** G7/G8/G10 | 7 requires / 115 calls; continuant collapse (2 sites→1 edge, T6); T5 allowlist physical-negative; G10 retract via `:desired-override` | G-F1 (analyzer ran on the specimen); F1/F3 stale-edge & reassert bugs introduced here, NOT caught by own gates | 0 |
| **DIFF_FALSIFICATION** (fresh-context, default-fail) | **6 CONFIRMED seam bugs** — F1 stale-edge-on-deletion, F2 `#N` block-path collision (reproduced, compiler-legal), F3 constant-HEAD reassert, F4 ingest-timeout uncounted, F5 git-exit swallow, F6 deny granularity — + 2 PLAUSIBLE; **DEFENDED** the dangerous classes (human-edge custody, concurrency, clocks, deny-before-catfile, kernel-fit) | G-F1 (its lineage hunts used PARSEABLE synthetics — never a broken historical blob) | ~1 context; killed 8 real bugs |
| **FIXWAVE** (cheaper model, line-cited) | applied all 8 with biting regressions; realized F1 as an in-memory cluster-scoped basis, NOT the sketched durable file (footgun-avoidance judgment, flagged for redline) | G-F1 (worked the DIFF_FALSIFICATION list, which lacked it) | ~1 context |
| **GATE_REVIEW** (Fable, fresh + §12 receipt) | **G-F1** — the break every cheaper layer structurally missed, found in the receipt's first MINUTE (full real corpus: 5/895 committed-broken blobs abort the unguarded sync); fixed in-session with biting regressions; F1 in-memory basis ACCEPTED; **G-F2** (`imp:clj:` mis-route) + N3/N4/N5/N6 surfaced | nothing blocking; N5 history-sort nit left as a note | ~1 context; found the ONE break |
| **This close session** (fresh G-F1 review + fix window) | confirmed G-F1 sound (§1); applied G-F2/N3/N4/N6 with biting regressions; 4 code commits (T11 split clean); receipt reproduced EXACT | **initially the committed-HEAD suite run** — commit 3 (6fbfd75) re-touched relation_kernel.clj + object_container.clj, going stale against `analyzer-gates`' HEAD-dynamic ingest → driver 7f/3e at HEAD; green pre-commit, never re-run post-commit. Caught by the recheck, FIXED in 381c445 (§7 Resolution) | ~1 context |
| **Retro adversarial recheck** (fresh subagent, default-fail) | **the committed-HEAD driver staleness the close session missed** (7f/3e, deterministic, root-caused to HEAD-dynamic ingest vs the fixed specimen) + Correction 2 (the "6 requires" miscount was the CONTRACT's, not P0's — P0 §B.1 measured 7); verified the five-layer headline + G-F1 trace + G-F2 + T11 at source | nothing further | ~1 context; caught a red suite + a mis-attribution |

**Headline (the D-006 evidence):** G-F1 lived in the gap between "HEAD/specimen
parses" and "all of history parses." FIVE layers (P0 gates, P1, P2, P3,
DIFF_FALSIFICATION) structurally could not see it — each used pinned specimen
commits, HEAD files, or parseable synthetics. Only the GATE's §12 dogfood
receipt — the single layer that touches the FULL real corpus — found it. This is
the pre-registered proof that a whole-history processor's definition-of-done must
include a full-corpus receipt as a GATE, not only pinned-specimen gates.

## 3 · What the next contract should do differently (each traced to a failure HERE)

1. **A whole-corpus claim MUST name its VERB.** P0 said "895/895"; that was
   ENUMERATE but read as PARSE, and parse-fidelity was only ever checked on HEAD's
   96 files — seeding G-F1. → Any coverage claim ("N/N blobs/files") states the
   verb (enumerate | parse | ingest | analyze) in the same sentence, and the
   definition-of-done gates EACH verb the processor performs over the FULL corpus
   it claims. *(G-F1; GATE_REVIEW §5/§11.)*

2. **A whole-history processor's definition-of-done gates a full-real-corpus
   receipt at the FIRST phase that touches the full corpus — not only at review.**
   G1-G10 synced pinned commits or HEAD; none touched all 895 blobs, so G-F1
   surfaced only at the gate. Had the receipt been a P2 gate, it would have
   surfaced at P2. → Promote the dogfood receipt from review-time artifact to a
   phase gate for versioned-corpus processors. *(G-F1 found only at GATE.)*

3. **A new object-container import-key PREFIX must register its routing in
   `extract-object-key` (or ride a handled prefix) — with a foreign-read routing
   gate.** G-F2: `imp:clj:` fell to `:else` → a foreign `read-import-completion`
   mis-routes to nil on a multi-task cluster. The IDENTICAL class bit
   block-kernel's `imp:sense-block:`. Two packages, same latent bug, both latent
   because no package consumer called the foreign read. → A package that mints a
   new import-key prefix names the routing branch (or handled-prefix reuse) as an
   explicit deliverable + a routing gate. *(G-F2 + block-kernel F2.)*

4. **Prose counts are HYPOTHESES; a phase pins them by measurement.** P0 said
   "3 reader-conds" (really 10, corrected in P1); the CONTRACT said "6 requires"
   (really 7 — P0 §B.1 itself measured 7 — corrected in P2). So the miscount lived
   in BOTH a spike AND the contract-authoring layer. Harmless here, but the same
   over-confidence produced the G-F1 verb slip. → Every prose count (spike or
   contract) is provisional until a phase re-derives it from the real artifact and
   records the correction. *(P1 8f32281, P2 278ff1b.)*

5. **A suite that reads git HEAD dynamically MUST be re-run AFTER the commits that
   move HEAD — or pin to a fixed specimen.** `analyzer-gates` resolved the analyzer
   head via `resolve-head-sha` but pinned its ground truth from a fixed blob; the
   coupling held only while HEAD == specimen. This package's OWN closing commit moved
   HEAD's blobs → the suite went red POST-commit though it was green pre-commit, and
   only the adversarial recheck's committed-HEAD run caught it. → (a) the close
   protocol re-runs every HEAD-reading suite AFTER the code commits, not only before;
   (b) a test that pins ground truth to a blob analyzes that blob explicitly
   (`:head-override`), never the moving HEAD. *(Recheck CORRECTION 1; fixed 381c445.)*

## 4 · Mechanisms that earned their keep (do not drop)

- **The §12 dogfood receipt over the full real corpus.** The ONLY layer that found
  G-F1, in its first minute. THE mechanism of this package; it also reproduced the
  whole-tree pins a fourth time this session, live-exercising the N4 refactor.
- **Fresh-context adversarial falsification, default-fail.** DIFF_FALSIFICATION
  killed 8 seam bugs the author's gates missed; the GATE's pass found G-F1 + G-F2
  + N3-N6. "Author self-review never substitutes" held — there was no code-atom
  author self-review, and the fresh layers carried the whole load.
- **Traps ledger cited by number + the standing `file(1)` gate.** Every
  load-bearing choice in the diff cites its trap; the one trap that fired at
  authoring (T8, raw NULs) was auto-caught by `file(1)`.
- **Physical PState readers for negatives.** G5/G6/G10 read `$$relations-by-id` /
  `read-relations-for-targets` directly (edge-row, rows-under), never the query
  API — so a dedup could never mask a missing/false edge.
- **Version-free deriver actors (R4/T1).** Made retraction rights work end-to-end:
  G10 proves the analyzer retracts its OWN stale edges because asserter == actor;
  a versioned asserter would have forked every edge AND lost the retract right.

## 5 · Residue (non-blocking; carries into the next package's contract)

> **CLOSED 2026-07-12** (`f06f511`, driver `code-atoms-test` 9t/198a green):
> doubt (1) **F4** — a with-redefs falsifier drops one OC decision → `:blobs-unresolved 1`
> and the seen-identity balances at non-zero; doubt (2) **cross-pass race** — `analyzer-sync!`
> now settles at exit via one batched `rk/await-relation` over this pass's own appends
> (`reconcile-edges!` returns the `:settle` descriptor); doubt (4) **N5** — new public
> `relation-history-display` re-sorts history by the numeric `t<N>` index (count + authoritative
> row untouched), pinned by a ≥10-transition test. Still open: (3) the `:blobs-unparseable`
> CI-pin nit, (5) the two-IPC-launch note, and the durable analyzer-basis fork (Sid's call).

- **GATE_REVIEW §10 open doubts, all carried:** (1) F4 non-zero ingest paths
  tested at zero only (falsifier: fault-injected runtime dropping one decision →
  `:blobs-unresolved 1`, identity holds); (2) cross-pass reconcile race — cheap
  hardening is an `rk/await-relation` settle at `analyzer-sync!` exit, if Sid
  wants it; (3) `:blobs-unparseable` non-zero is receipt-proven, not CI-pinned (it
  drifts with history, same class as the whole-tree pins); (4) **N5** history-sort
  nit — at a fixed head with ≥10 transitions `t10` sorts before `t2` lexically;
  count (what F3 reads) and the authoritative row are unaffected, display order
  only — left as a note by ruling; (5) two IPC launches in code-atoms-test
  (sequential, try/finally-closed, empirically green).
- **Consumer disciplines the next contract MUST inherit:** (a) a new import-key
  prefix → `extract-object-key` branch + routing gate (G-F2 class — bit two
  packages); (b) a whole-corpus claim names its verb (G-F1); (c) a whole-history
  processor gets a full-corpus receipt gate (G-F1).
- **The durability fork remains Sid's.** The in-memory `:analyzer-basis` was
  ACCEPTED at gate; a durable basis slots behind the SAME seam when Sid rules
  durable-cluster vs spine-edge replay log. Revert-cheap either way.
- **T11 coordination:** block-kernel re-runs ITS gates after this package's commit
  3 (6fbfd75) lands — a trivial rebase of the `relation-kinds` literal (its
  `:grounds :assembled-from :refines` are still uncommitted in the working tree).

## 6 · Deviations from CONTRACT (recorded, all within authorization)

- **deps placement**: CONTRACT §2 said "dev/test-visible" deps; rewrite-clj +
  clj-kondo landed in top-level `:deps` — necessary because adapter + driver live
  under `src/` and must load on the server classpath. Additive, allowlist-clean.
- **Second kernel-file edit** (`object_container.clj` G-F2): outside the original
  STANDING allowlist, but Sid COUNTERSIGNED it as "this package's second and last
  kernel edit" (decisions.md gate entry). Landed with the registry commit.
- **F1 realized in-memory, not durable** (FIXWAVE judgment): accepted at gate;
  durability deferred to Sid's fork.

## 7 · Adversarial recheck

Recorded by the fresh recheck subagent (§below). Every scorecard claim was
verified against the artifacts, the git trail, and a fresh suite run; corrections
applied in place.

**Recheck — 2026-07-09, fresh-context subagent (did not write this retro; default-fail).**

### Fresh suite run (EXACT counts, at committed HEAD `7c2e040`)
One JVM, all three suites (the /work-package command):
`adapter 10t/91a/0f` · `code-atoms (driver) 9t/187a — 7 failures + 3 errors` ·
`relation-kernel 2t/222a/0f`. Re-ran the driver ALONE in its own JVM →
**identical `9t/187a, 7f/3e`** (`fn__80181` vs `fn__80639` compile ids differ,
failure content byte-identical). So the driver failure is **deterministic — NOT
the `rand-nth [2 4]` task count, NOT a one-JVM isolation artifact.**

### CORRECTION 1 (headline) — the driver suite is NOT green at committed HEAD
RETRO §0 claimed `driver 9t/187a … all 0f`. **Refuted.** All 7 failures + 3 errors
are in `analyzer-gates`, all downstream of ONE cause: the test's INGEST step
(`code_atoms_test.clj:407-408`) is hardcoded to `:commit-filter #{c-63202b0 c-f6257a9}`
— the commits that LAST-TOUCHED relation_kernel.clj (`63202b0`→blob `a002c89`) and
object_container.clj (`f6257a9`→blob `c54e8ac`) *at test-write time* (commit 9a2445b).
But this package's own closing commit **6fbfd75** (register `:requires/:calls` +
G-F2 `imp:clj:` routing) RE-TOUCHED both files, moving their HEAD blobs to `49b87b7`
/ `c68045d`. The test resolves `rk-head-sha` dynamically (= `49b87b7`) and checks
`read-source` for it (`:411`), but the ingest only stored `a002c89` → `read-source`
nil (`:411`, `:455`) → anchor `du` nil (`:457`) → NPE on `(long (:start-offset du))`
(`:458`, the 3 errors). Verified by git: `git log -1 -- relation_kernel.clj` =
6fbfd75; `git rev-parse f6257a9:…relation_kernel.clj` = `a002c89` ≠ HEAD `49b87b7`.
The G7 stats map (`:425`, `:calls-asserted 115` …) STILL passes, so the analyzer
LOGIC is intact — this is a **stale test fixture, not a product regression.** But the
package closed with a red driver suite that no layer re-ran post-commit: every prior
green run (P2/P3/DIFF/FIXWAVE/GATE) executed against the UNCOMMITTED working tree,
where HEAD's committed blobs were still `a002c89`/`c54e8ac` and the filter matched.
Fixing it needs a one-line test change (make the ingest filter cover HEAD's blobs /
resolve it dynamically) — out of this recheck's edit scope (RETRO-only). Corrected
§0, and the §2 "This close session" Missed cell (was "—").

### CORRECTION 2 — "6 ns deps" mis-attributed to P0
RETRO §2 (P2 row) and §3.4 credited P0 with a "6 ns deps" miscount. **P0 never said
6** — P0 §B.1 measured 7 ("All 7 dependencies appear, 7/7"). `git show 278ff1b`
proves the "6" lived in the **CONTRACT** (`-(specimen's 6 requires;` →
`+7 ns dependencies per P0 B.1`); P2 corrected the CONTRACT using P0's already-correct
7. Fixed the P2 row and rewrote §3.4 to attribute the "6 requires" to the
contract-authoring layer (P0 had it right).

### Independently VERIFIED (source/artifact/git, not inherited)
- **§1 G-F1 trace, exact:** `clojure-form-v0` wraps ONLY the parse step
  (clojure_adapter.clj:232-237: `try (parse-string-all) (catch Exception e e)`;
  degrade → `{:units [] … :parse-error}`; happy path `parsed-form-cut` runs only on
  a parsed node). Analyzer lane: `head-file-context` (:635-640) → `:units []` on a
  parse-failed blob → `enclosing-unit-block-path` over `[]` = nil (:646-654) → usage
  SKIPPED + `:usages-unmapped` (:677-678). The named regressions EXIST and assert as
  described: adapter `gf1-unparseable-text-degrades-loud` (:250), driver
  `gf1-unparseable-blob-degrades-loud` (:264); the G5/G6/G9 pins carry
  `:blobs-unparseable 0`.
- **G-F2 (§3.3/§6):** the `imp:clj:` branch IS in committed object_container.clj
  (`git show 6fbfd75`; extract-object-key:304-305 `(subs s 8)`); pre-fix the `imp:`
  branches were `imp:tr:`/`imp:md:` only (:291-295), `imp:clj:` fell to `:else`;
  `$$import-completions-by-key` is `:key-partitioner partition-by-object-key`
  (oc:1714); a `gf2-import-key-routes-to-object-key-partition` regression rides the
  (green) adapter suite (:328).
- **T11 §5:** working tree holds block-kernel's `:grounds :assembled-from :refines`
  UNCOMMITTED (relation_kernel.clj:69-73, blob `8071266`); commit 6fbfd75 staged
  ONLY `:requires :calls` (`git show 6fbfd75 -- relation_kernel.clj` = a002c89→49b87b7,
  3-line add). Confirmed.
- **§2 spot-checks:** DIFF_FALSIFICATION VERDICT = "6 CONFIRMED, 2 PLAUSIBLE"
  (F1-F6 map to the RETRO's list); P0 §c "895/895" is ENUMERATION (B-full; naive
  `git log` drops 2); P1 corrected reader-conds 3→10 (PHASE_P1 Deviations + commit
  8f32281); kondo hooks fabrication 193→6 (P0 §B.4).
- **The FIVE-layer headline (§2):** holds. Each layer's data scope confirmed to
  exclude broken HISTORICAL blobs — P0 parse-fidelity ran on HEAD's 96 files
  (P0 §a); P1 on pinned parseable fixtures; P2 G5/G6/G9 on pinned specimen commits
  (all parse); P3 on the specimen; DIFF on parseable synthetics + whole-HEAD-TREE
  (not history). P2's enumeration oracle touches all 895 blobs but only ENUMERATES
  (no parse), consistent with "895/895 = enumeration." Only the §12 receipt's full
  `code-sync!` parses all history → hits the 5. Accurate; no correction.
- **§0/§2 relation-kernel & adapter counts, receipt DoD structure:** adapter
  10t/91a/0f and relation-kernel 2t/222a/0f reproduced EXACT.

### Open doubts (could NOT independently verify — not silent passes)
- **The §12 dogfood receipt was NOT re-run** (expensive; outside the 3-suite
  mandate). Its quoted pins (895 blobs, 458/5073/279/50, 41s/19s) are gate-session
  snapshots; HEAD has since advanced (6fbfd75 + 7c2e040), so a re-run would drift.
  Note: the receipt does a FULL `code-sync!` (all blobs ingested), so it does NOT
  hit the analyzer-gates staleness above — the receipt likely still runs
  end-to-end, but this recheck did not confirm it.
- **Latent secondary staleness in analyzer-gates, currently masked:** the hardcoded
  call-site rows (167/457/843) and G7 count pins predate the +4-line registry
  insertion at relation_kernel.clj:65, so they too are stale for blob `49b87b7`; the
  span-contains check (:458) would likely fail on row-shift even if the source were
  ingested. Masked by the primary nil-source failure; not independently isolated.

### Resolution (close session, after the recheck — 2026-07-09)

The recheck's CORRECTION 1 (driver red at committed HEAD) and its "latent secondary
staleness" open doubt are BOTH resolved in commit **381c445**: `analyzer-gates` now
pins the analyzer to the specimen commit (`head c-63202b0`, rk@`a002c89`) and passes
`:head-override head` to the G7 `analyzer-sync!` — the fixed-specimen discipline G5/G6
already use (T3, "analyze the blob, not the checkout"). This fixes both staleness layers
at once: the ingest stores the analyzed blob (nil-source gone) AND the pinned call rows /
kondo stats / census are valid because the analyzed blob is `a002c89` — the very blob
they were derived from. Driver suite GREEN at HEAD `a146dc8`: **9t/187a/0f**, re-run
twice (deterministic). The lesson is now a §3 rule + a quirks entry: a suite that reads
git HEAD dynamically must be re-run AFTER the commits that move HEAD, or pin to a fixed
specimen. Correction 2 ("6 requires" = a CONTRACT miscount, not P0's — P0 §B.1 measured
7) stands as the recheck left it. The §12-receipt open doubt is also resolved: the
receipt WAS re-run this session (before the recheck) — 895/895 blobs,
`:blobs-unparseable 5`, whole-tree 458/5073/279/50, RECEIPT-DONE — exact vs GATE_REVIEW §8.
