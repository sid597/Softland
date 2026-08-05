# Code-Atom Work Package — GATE REVIEW

2026-07-09 · Fable (fresh-context gate session per the opening prompt; the QC
layer's required fresh context — the fix wave deliberately did not self-gate).
Judged: the CODE in full (clojure_adapter.clj 651 ln · code_atoms.clj 900 ln ·
both test nss · the relation_kernel.clj registry line · deps.edn), against
CONTRACT v1 + SPEC v0 (binding). Phase records + FIXWAVE used as input, not
authority. Default-fail posture; every kernel-behavior claim below was verified
at source this session (file:line cites), not inherited.

## VERDICT — PASS, with one gate-session fix applied (G-F1) and one finding routed to Sid (G-F2)

- All contract gates G1–G11 hold. All three suites re-run independently THIS
  session, twice: **pre-fix exact reproduction** of the fix wave's pins
  (adapter 8t/82a/0f · code-atoms 8t/179a/0f · relation-kernel 2t/222a/0f),
  then **post-G-F1 green** (adapter 9t/88a/0f · code-atoms 9t/181a/0f; rk
  untouched).
- The falsification pass found **one CONFIRMED break every prior layer missed
  (G-F1: a committed-broken historical blob aborts the whole sync)** — caught
  by running the CONTRACT §12 dogfood receipt, fixed in-session with biting
  regressions (details §5), flagged for Sid's redline, still uncommitted.
- **F1 adjudication (the #1 judgment sent to this gate): the in-memory,
  caller-owned, cluster-scoped `:analyzer-basis` atom is ACCEPTED** (§4).
- One latent finding routed to Sid as PROPOSED (G-F2: `imp:clj:` import-key
  unrouted by `extract-object-key` — the exact class Sid just ruled on for
  block-kernel's `imp:sense-block:`), plus four minor recorded items (§6).
- The §12 dogfood receipt ran end-to-end on THIS repo post-fix (§8).

## 1 · Receipts

| suite | independent re-run (pre-fix) | post-G-F1 |
|---|---|---|
| `clojure-adapter-test` | 8t / 82a / 0f — exact | **9t / 88a / 0f** |
| `code-atoms-test` | 8t / 179a / 0f — exact | **9t / 181a / 0f** |
| `relation-kernel-test` | 2t / 222a / 0f — exact | untouched |

Byte-level trap checks: `file(1)` reports clean UTF-8 text for all four package
files and both fixtures (T8 — no raw NUL entered any source; the U+0000 test
literal is built from escape chars at runtime, adapter test :36). Compile check
`:COMPILE-OK` on both source nss after the gate fix.

## 2 · Architecture (what was judged)

Two-file extension over proven seams, exactly the CONTRACT §2 placement: the
adapter mirrors `markdown_adapter.clj` stage-for-stage (pure cut →
`source-materialization` → import-request builder; zero I/O), the driver is
git_spine-shaped (pure enumeration/lineage fns + `code-sync!`/`analyzer-sync!`
over public OC + rk APIs; no module/depot/PState). Identity math is pure over
(blob-sha, text); every stored clock is a committer clock (T4); the deriver
actors are version-free with the version riding `note` (R4/T1 — verified
against the retraction-rights guard, relation_kernel.clj:452-458: retract is
rejected unless envelope actor == stored asserter, which is exactly why the
same version-free actor must both assert and retract). The one registry edit is
additive (`:requires :calls`, relation_kernel.clj diff) — the closed-set
rejection still holds (G8 test + `registered-kind?` :294).

Kernel-behavior claims re-verified at source this session:

- **Journal gate**: `(local-select> [(keypath *relation-id *journal-key)] …)`
  + `(filter> (nil? *prior-decision))` (rk:684-686) — a reused
  (relation-id, idempotency-key) is silently dropped. This is what makes F3's
  transition-unique keys load-bearing.
- **Status-log/history**: every accepted transition writes one log row keyed
  `fixed-width-order-key(ts, request-id)` (rk:739-742 write; :803-816 the
  relation-detail query reads the WHOLE submap via `(subselect MAP-VALS)`,
  unpaged) — so `count(history)` = exact transition count, and at a FIXED head
  (constant ts) the `t0`/`t1:retract`/`t2` request-ids keep the rows distinct.
- **No read-by-asserter**: the runtime map exposes by-id, by-target,
  descriptors, status-log, activity — nothing keyed by asserter (rk:908-924).
  This is the physical ground of the F1 ruling (§4).
- **`->target-ref` fallthrough**: unknown kinds → verbatim target-key
  (rk:843-854), so `:code-form`/`:var`/`:ns` target-keys are the ids
  themselves, never nil/blank (object-keys are colon-free sha-256 hex,
  object_container.clj:206-208, so the `du:`/`sa:` extraction idiom is sound).

## 3 · Trap spot-checks (T1–T11, in the diff)

- T1/R4 version-free asserters ✓ (constants :42-48, :484-487; notes carry
  `clj-atoms-v1|…`). T2 hash short-circuit ✓ (commit-lineage :284-293; G5's 284
  re-addressed + 3-var negative sample). T3 cat-file only ✓ (blob-text :92-97;
  fixtures are pinned .txt, tests never slurp live src). T4 ✓ committer clocks
  throughout; `pass-started-ms` and the await deadline are classification-only
  wall reads, never stored; G9 re-run byte-identical. T5 ✓ Rama kondo hooks
  from classpath (:574-588), `:calls` allowlisted to `app.*` `:to`, drops
  counted (:691). T6 ✓ continuant grain, lowest-row representative; G7's
  two-call-sites→one-edge pin. T7 ✓ top-level only, reader-cond atomic.
  T8 ✓ (§1). T9 ✓ append-ALL/await-ALL (:388-392). T10 ✓ deny before cat-file
  in BOTH lanes (enumerate excludes denied before text-of; head-code-blobs
  filters before materialize; G4 + the R6 IPC test read the physical absence).
  T11 ⚠ live: the working tree holds BOTH packages' registry kinds in ONE
  hunk (`:requires :calls` + `:grounds :assembled-from :refines`) — commit-time
  manual split required; re-run both packages' gates after either lands.

## 4 · F1 adjudication — the in-memory `:analyzer-basis` is ACCEPTED

The question sent to this gate: is the caller-owned, cluster-scoped atom sound,
is the cross-boot deferral honest, and is `:reconcile-basis-missing` truthful?

**Ruling: accept the in-memory form.** Grounds, each verified at source:

1. **The kernel genuinely cannot close F1 by reads alone.** No by-asserter
   index or scan exists (§2 above); a both-endpoints-gone edge shares no
   target-key with any desired edge, so remembering the last desired set is the
   only in-allowlist mechanism. A new kernel index would be a stop-clause.
2. **Disjointness is by construction, not luck.** `basis-candidates` removes
   both `desired-rids` and `asserted-rids` (code_atoms.clj:780), so the basis
   lane can never double-retract an edge the target read already handles, and
   each rid gets at most one append per pass. Each basis retract is confirmed
   `:asserted` via `read-relation-detail` before it is issued (:781-785) — an
   already-retracted or never-landed rid is skipped, so the count stays honest.
3. **The scope is the correct one.** On the ephemeral `create-ipc` cluster a
   fresh boot re-asserts only HEAD's edges — there IS no cross-boot leak to
   retract; the atom's lifetime equals the retraction scope. A durable edn file
   is the quirks-file footgun shape and buys nothing today; when Sid rules the
   durability fork, a durable basis slots behind the same `:analyzer-basis`
   seam. Revert-cheap either way (Sid may still redline to the durable file).
4. **The boundary signal is truthful with one wrinkle** (N3, §6): missing-basis
   is computed as `(if (empty? prior) 1 0)` (:821), which conflates "no basis
   provided" with "the prior pass legitimately desired ZERO edges" — after a
   zero-edge pass the next run reports `:reconcile-basis-missing 1` falsely.
   One-line fix (`nil?` vs `{}`); queue for the coordinated pass.
5. **G10 physically exercises the lane**: D4 retracts a unique-endpoint edge
   reachable ONLY via the basis; D5 re-asserts at the same head through the
   F3 key. I traced both against the journal gate and the status-log semantics
   (§2) — the mechanism is real, not test-shaped.

Scope caveat recorded (N6, §6): the basis atom is scope-blind — reusing one
atom across DIFFERENT `path-filter` scopes over-retracts edges that merely
left the analyzed scope, not HEAD. Production default (whole tree, one atom
minted with the runtime) is consistent; the docstring should bind basis ↔
scope explicitly. Docstring-only fix.

## 5 · G-F1 (NEW, CONFIRMED, fixed in-session) — committed-broken blobs aborted the whole sync

**The break**: `clojure-form-v0` parsed unguarded; `code-sync!` isolates only
`:git/exit` failures. Git history contains blobs that were committed BROKEN —
in THIS repo, 5 of 895 code blobs (4 old `electric_flow.cljc` states with an
unmatched `)`, 1 old `ttf.clj` with a reader-hostile keyword). The first one
reached by a full-history sync threw out of rewrite-clj and killed the entire
run — the CONTRACT §12 definition-of-done was physically unmeetable on this
repo. Every prior layer missed it: P0's "895/895" was blob ENUMERATION; its
parse-fidelity check ran on HEAD's 96 FILES; G5/G6 sync only the pinned
specimen commits; DIFF_FALSIFICATION's lineage hunts used parseable synthetics.
The receipt caught it in its first minute — which is exactly what the receipt
is for.

**The fix (in-allowlist, both files are package files, ~20 lines)**: a parse
failure degrades to a ZERO-unit cut carrying `:parse-error` (adapter);
`cut-named-units` passes it through so lineage sees empty name maps (no fake
edges, discontinuities surface as counted unmatched-vanished/appeared); the
raw surface still ingests verbatim (R1 "store raw + atoms" holds — the atoms
are honestly absent, the map does not lie); `code-sync!` counts
`:blobs-unparseable` (an overlay count over ingested/converged; the
`:blobs-seen` identity is unchanged). Regressions bite: the adapter test
proves cut-degrade + 0-unit import validity, the driver test proves the
passthrough, and all three G5/G6/G9 exact-map pins now require
`:blobs-unparseable 0`. Receipt re-run end-to-end green (§8).

Gate-session-authored fix under D-010/fix-don't-defer (revert-cheap, biting
regressions, physically proven by the receipt); flagged here for Sid's redline
like every other uncommitted change.

## 6 · G-F2 (NEW, latent) + minor findings — routed, not improvised

- **G-F2 — `imp:clj:` unrouted by `extract-object-key`** (CONFIRMED, latent).
  `clojure-import-key` mints `imp:clj:<object-key>:<sha>` (adapter :572-574),
  but `extract-object-key` (object_container.clj:284-339) handles `imp:tr:` and
  `imp:md:` only — `imp:clj:` falls to `:else` (the full string), so a FOREIGN
  `read-import-completion` (runtime.clj:149-151; `$$import-completions-by-key`
  is `:key-partitioner partition-by-object-key`, oc:1704-1705) partitions by
  hash of the whole key and reads nil. In-topology dedup is unaffected (the
  local-select runs after `(|hash *object-key)`, oc:1824-1831) — G1 convergence
  is real. No package consumer calls `read-import-completion` → latent. This is
  the same class Sid just RULED for block-kernel's `imp:sense-block:` key.
  Options: (a) restructure the key to ride a handled prefix (no semantically
  honest one exists for clj — `imp:tr:` means transcript); (b) ONE additive
  `imp:clj:` cond branch in `extract-object-key`, the exact `imp:md:` shape —
  a kernel edit needing Sid's authorization (T13-analog). **Recommended: (b)**,
  bundled with the block-kernel fix-pass or this package's own fix window.
  PROPOSED in decisions.md; keys are pre-commit and revert-cheap either way.
- **N3 — basis-missing conflation** (§4.4). One-line; coordinated pass.
- **N4 — materialize-throw temp leak**: `materialize-head-tree!` runs OUTSIDE
  the `try/finally` that P1 added (code_atoms.clj:862-877) — a NON-git throw
  mid-materialization leaks that one temp dir. Per-run accumulation (the P1
  finding) is fixed; this is the residual corner. One-line; coordinated pass.
- **N5 — history sort nit**: at a fixed head with ≥10 transitions, `t10` sorts
  lexically before `t2` in the status log's order-key. Count (what F3 reads)
  and the authoritative row are unaffected; display order of a pathological
  history could interleave. Note only.
- **deps.edn placement**: CONTRACT §2 said "dev/test-visible" deps; rewrite-clj
  + clj-kondo landed in top-level `:deps` — necessary because adapter + driver
  live under `src/` and must load on the server classpath. Additive ✓,
  allowlist ✓; wording deviation recorded.

## 7 · Falsification pass (protocol sections)

**Failure modes attempted** (beyond §4–§6): custody collision (Sid hand-asserts
the same pair — CANNOT-HAPPEN: asserter-scoped relation-id rk:112-120 + the
explicit asserter filter code_atoms.clj:764-766); F3 key collision at fixed
head (CANNOT-HAPPEN while transitions strictly increase: each accepted
transition adds one history row, keys embed the count; byte-identical re-run →
to-assert empty → converges, G9); dedup-vs-literal collision (F2 — `~` cannot
occur in a symbol token, positional `%06d` cannot collide with names since
symbols can't start with a digit; regression + R7 law test); `(def ns …)`
after `(ns …)` (shared `seen` map → `ns~2`); defmethod dispatch collision
(dispatch value in the path + dedup); merge double-count (per-parent blocks,
`seen-edges` + stable journal keys); F4 identity (holds by construction:
denied + git-failures + ingested + converged + unresolved partitions
`blobs-seen`; unresolved shas always have cached text so `cut-of` never
re-throws); F5 non-git exceptions still propagate (`git-failure?` re-throw —
real bugs are never counted as git failures); healthy→broken→healthy history
(G-F1 fix: counted discontinuity, no fake edges).

**Writers/readers/clearers**: unchanged from DIFF_FALSIFICATION's table except:
(1) the F2 collision hazard on `$$derived-units-by-id` is CLOSED (distinct
block-paths → distinct unit-ids, regression-pinned); (2) the reconcile's
clearer role now covers vanished-endpoint edges via the basis lane (writer:
`analyzer-sync!` reset of the caller's atom, :879; reader: next pass's
`prior-basis`; clearer: the reset itself — one writer, one pass grain);
(3) `:analyzer-basis` is the only NEW mutable state — caller-owned, no
cross-thread sharing, dies with the cluster.

**Async ordering**: appends are `:append-ack` (rk:925-934 — durable, NOT
materialized). Within one pass no rid is read after being written (the three
append sets are disjoint). ACROSS passes, reads race the microbatch: two
back-to-back syncs with a status flip inside the materialization window could
read a stale transition count → mint an already-journaled key → the transition
is dropped; if that edge was basis-only it is then forgotten (basis rolls
forward). Tests drain on the processed-count barrier; the REPL dogfood is
sequential and spaced; git_spine has the same shape. Open doubt 2 with
falsifier below — the cheap hardening is an `rk/await-relation` settle at
`analyzer-sync!` exit, if Sid wants it.

**Error-path cleanup**: per-blob git isolation in ingest, per-block in lineage,
per-file in materialize (all counted); analyzer temp tree deleted in `finally`
(N4 corner recorded); kondo throw propagates BEFORE any edge append (derive
precedes reconcile — no partial writes); every retract's envelope actor == the
stored asserter, so no self-inflicted rejections.

## 8 · Definition-of-done — the REPL dogfood receipt (CONTRACT §12)

Script: `dogfood_receipt.clj` (session scratchpad; reproduced in NOW-grade
detail here). Full-history `code-sync!` + whole-HEAD-tree `analyzer-sync!`
against THIS repo on the trail-view runtime, then the three required reads +
the G11 worked example.

Ran end-to-end, RECEIPT-DONE (post-G-F1; the pre-fix run died on the first
broken historical blob — that death IS the G-F1 evidence):

- **`code-sync!` full history — 41s**: `{:commits-seen 352 :blobs-seen 895
  :blobs-ingested 895 :blobs-denied 0 :blobs-converged 0 :blobs-unresolved 0
  :git-failures 0 :blobs-unparseable 5 :supersedes-mech 1782
  :supersedes-silver 524 :re-addressed 13518 :unmatched-vanished 996
  :unmatched-appeared 4151 :lineage-over-unresolved 0}`. The F4/F5 identity
  holds on the full corpus (895 = 895 ingested + zeros); the 5 unparseable
  blobs are exactly the 5 the probe enumerated (4 old electric_flow.cljc
  states, 1 old ttf.clj).
- **`analyzer-sync!` whole HEAD tree — 19s**: `{:ns-usages-seen 485
  :var-usages-seen 46934 :var-usages-dropped-nonapp 38082 :usages-unmapped 279
  :requires-asserted 458 :calls-asserted 5073 :retracted 0 :converged 0
  :reconcile-basis-missing 1 :unresolved-residual 50 :git-failures 0}` — the
  P3/DIFF_FALSIFICATION whole-tree pins (458/5073/279/50), reproduced a third
  time, now END-TO-END through the kernel (edges landed, not just derived);
  basis-missing 1 is the honest first-sync signal.
- **(1) Specimen census**: relation_kernel.clj@`a002c89` → 103 units
  `{:clj/ns 1 :clj/def 9 :clj/fn 84 :clj/record 8 :clj/module 1}` + 26 comment
  runs; the HEAD blob's source artifact reads back (53,543 stored bytes).
- **(2) `oc/fixed-width-order-key` callers — ONE relation read**:
  `markdown-adapter/source-materialization`, `object-container/edit-effects`,
  `relation-kernel/relation-outcome`, `relation-kernel/target-sort-key` (the
  G7 specimen pair plus the two cross-file callers the whole tree adds).
- **(3) `relation-outcome` supersedes chain** across its 4 blob versions
  (1dfea68→79b6ae3→f102b76→a002c89): `79b6ae3 supersedes 1dfea68`
  (`|mech|af0e0e2…`, :asserted) · `a002c89 supersedes f102b76`
  (`|mech|63202b0…`, :asserted) — and honestly NO edge across fd59b78, where
  the fn was hash-equal (re-addressed mints nothing, T2).
- **(4) G11 worked example** (§9): the PARTIAL 1–180 tuple covers 45 of
  object_container.clj@HEAD's 224 units; one concrete mark printed with the
  real deterministic unit-id.

## 9 · G11 — a MAP.md coverage tuple as grounds-marks (worked example)

MAP.md ledger row: `object_container.clj · PARTIAL · lines 1–180 · 2026-07-09`.
As marks over code blocks: for each top-level form-unit of
`object_container.clj@HEAD` whose span intersects `[0, line-start(181))`, one
`:grounds` edge — from the reading actor (`fable:map-session-2026-07-09`,
asserter-scoped identity like every other edge), to the unit
(`target-kind :code-form`, target-id = the deterministic unit-id), depth
qualifier riding `note` (`map-coverage|depth=PARTIAL|lines=1-180`), asserted-at
= the read date. DECLS rows are the same shape with `depth=DECLS`; UNREAD files
are the ABSENCE of marks — the map never claims what was not read. `:grounds`
is already registered (block-kernel's authorized kinds). The receipt computed
the real mark set for this tuple (§8): the tuple is expressible with zero new
machinery — G11 holds.

## 10 · Open doubts (non-blocking, each with its cheap falsifier)

1. **F4 non-zero ingest paths still tested at zero only** (inherited residue).
   *Falsifier*: fault-injected runtime dropping one decision →
   `:blobs-unresolved 1` and the identity holds.
2. **Cross-pass reconcile race** (§7 async). *Falsifier*: two immediate
   `analyzer-sync!` calls flipping one desired edge with no barrier between;
   assert the final row status matches the last desired set.
3. **`:blobs-unparseable` non-zero is receipt-proven, not CI-pinned** (the 5
   real broken blobs drift with history like the whole-tree analyzer pins —
   same class as the FIXWAVE's recorded residue). *Falsifier*: commit-filter a
   sync to the ttf.clj-breaking commit; assert `:blobs-unparseable 1`.
4. **G-F2 latent mis-route** (§6). *Falsifier*: one REPL line —
   `(ocr/read-import-completion rt (:import/key req))` on a multi-task cluster
   returns nil for a stored clj import.
5. **Two IPC launches in code-atoms-test** (inherited, sequential,
   try/finally-closed, empirically green).

## Appendix — the receipt script (CONTRACT §12 "documented REPL invocation")

Run with `clojure -M:test <file>` from the repo root. This exact script produced
§8's numbers (2026-07-09, gate session). Reproduce it verbatim; only the pinned
whole-tree numbers drift as HEAD moves.

```clojure
(require '[app.server.rama.trail-view :as tv]
         '[app.server.rama.code-atoms :as ca]
         '[app.server.rama.object-container :as oc]
         '[app.server.rama.object-container.clojure-adapter :as adapter]
         '[app.server.rama.object-container.runtime :as ocr]
         '[app.server.rama.relation-kernel :as rk])

(def repo (System/getProperty "user.dir"))
(def rt (tv/start-trail-view-runtime! {:tasks 4 :threads 2}))

(try
  ;; full sync: every commit, every code blob, both lanes
  (prn :code-sync (ca/code-sync! {:runtime rt :repo-root repo}))
  (let [basis (atom nil)]   ; cluster-scoped, minted with the runtime (F1)
    (prn :analyzer-sync (ca/analyzer-sync! {:runtime rt :repo-root repo
                                            :analyzer-basis basis})))

  (def head (ca/resolve-head-sha repo))
  (def rk-path "src/app/server/rama/relation_kernel.clj")
  (def rk-sha (ca/head-blob-sha repo head rk-path))
  (def rk-text (ca/blob-text repo rk-sha))

  ;; (1) specimen census + stored-surface join
  (let [{:keys [units comment-spans]} (adapter/clojure-form-v0 rk-text)
        okey (oc/object-key-for (str "git-blob:" rk-sha) (oc/source-hash rk-text))]
    (prn :census {:total (count units)
                  :kinds (frequencies (map :unit-kind units))
                  :comment-runs (count comment-spans)})
    (prn :source-present? (some? (ocr/read-source rt (oc/source-id-for-object-key okey)))))

  ;; (2) fixed-width-order-key callers — ONE relation read
  (let [fwok "app.server.rama.object-container/fixed-width-order-key"
        _ (rk/await-relation
           #(get (rk/read-relations-for-targets rt [fwok] [:calls] false) fwok)
           seq 30000)
        rows (get (rk/read-relations-for-targets rt [fwok] [:calls] false) fwok)]
    (prn :fwok-callers (->> rows (filter #(= fwok (:target-id (:to %))))
                            (map #(:target-id (:from %))) sort vec)))

  ;; (3) relation-outcome's supersedes chain across its blob versions
  (let [shas (->> (ca/read-code-log repo) (mapcat :changes)
                  (filter #(= rk-path (:path %))) (map :new-sha)
                  (remove #(= % "0000000000000000000000000000000000000000")) distinct)
        uids (into {} (keep (fn [sha]
                              (when-let [u (ca/code-unit-id repo sha "relation-outcome")]
                                [u (subs sha 0 7)]))) shas)
        edges (->> (rk/read-relations-for-targets rt (vec (keys uids)) [:supersedes] false)
                   vals (apply concat)
                   (map (juxt :relation-id identity)) (into {}) vals)]
    (doseq [e (sort-by :first-asserted-at-ms edges)]
      (println (uids (:target-id (:from e))) "supersedes" (uids (:target-id (:to e)))
               "|" (:note e) "|" (:relation-status e))))

  ;; (4) G11 — the MAP.md tuple (object_container.clj · PARTIAL · 1–180) as marks
  (let [oc-path "src/app/server/rama/object_container.clj"
        oc-sha (ca/head-blob-sha repo head oc-path)
        oc-text (ca/blob-text repo oc-sha)
        okey (oc/object-key-for (str "git-blob:" oc-sha) (oc/source-hash oc-text))
        end-off (nth (ca/line-start-offsets oc-text) 180 (count oc-text))
        units (:units (adapter/clojure-form-v0 oc-text))
        covered (filter #(< (long (:start-offset %)) (long end-off)) units)]
    (prn :g11-covered (count covered) :of (count units))
    (prn :g11-mark {:kind :grounds
                    :from {:target-kind :actor :target-id "fable:map-session-2026-07-09"}
                    :to {:target-kind :code-form
                         :target-id (adapter/derived-unit-id okey (:block-path (first covered)))}
                    :note "map-coverage|depth=PARTIAL|lines=1-180"}))

  (finally (tv/close-trail-view-runtime! rt)))
(println "RECEIPT-DONE")
(System/exit 0)
```

## 11 · D-006 evaluation notes (this package's implementation-contact evidence)

- The five-layer QC model earned its keep in the exact order designed: gates
  proved happy-path math; the adversarial diff pass killed 8 seam bugs; the
  GATE receipt — the only layer that touches the full real corpus — found the
  one break all cheaper layers structurally could not (G-F1 lives in the gap
  between "HEAD parses" and "history parses").
- Contract hygiene finding for the next package: a "whole-repo" claim in a
  spike (P0's 895/895) must name its VERB (enumerate vs parse vs ingest) — the
  ambiguity cost nothing here only because the receipt existed.
- The traps-ledger discipline held (every load-bearing choice in the diff
  cites its trap); the one trap that fired mid-package (T8, at contract-write
  time) was caught by the standing file(1) gate.
