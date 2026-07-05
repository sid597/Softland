# PHASE P3 — component D, display names (F-L3) · git-spine WP2

Fresh-context builder, 2026-07-05. Contract: `CONTRACT.md` v1.2 §3.D, gate G9.
Owned files: `src/app/server/rama/trail_view.clj` + `test/app/server/rama/trail_view_test.clj`
(additive; the gate-passed trail-view suite untouched in semantics). Plus one
HQ-adjudicated allowlist extension mid-phase (see the cross-package finding below).

## What was built

All changes are PURE data/projection — no PState, no topology, no schema change
(the §5.7c truth seam stays deferred, per contract).

**`trail_view.clj` — new pure fns** (one block, after `flatten-row-groups`):
- `commit-source-ref?` — source-ref starts with the literal `"git-commit:"` (§3.A interface).
- `source-ref->sha` — the sha verbatim after the prefix.
- `sha7` — first 7 chars, tolerant of shorter input.
- `basename` — last `/`-segment of a path-like ref, nil-safe.
- `source-ref->display-name` — FEED name: commit ref → `<sha7>`; md/doc → basename;
  nil in → nil out (the raw id stays the honest fallback; cards.cljc:275 already
  falls back to `:id`). The feed row carries NO subject (v1.2 per validator S2).
- `subject-line` — parses the `subject:` header line of a §3.A labeled canonical
  text (first match wins; the header precedes any body by format).
- `bundle-display-name` — BUNDLE/View-3 name: commit material → `"<sha7> · <subject>"`
  (subject read from `[:material :content-text]`, which for the md-ridden commit
  artifact IS the §3.A canonical text — the md adapter stores the full raw body as
  the document container's `current-content-text`, `markdown_adapter.clj` document-row
  construction); md material → basename of source-ref; else nil.

**`trail_view.clj` — three wiring points:**
- `file-activity-entry` → `:entry/target` gains `:display-name (basename (:file-path row))`.
- `source-activity-entry` → `:entry/target` gains `:display-name (source-ref->display-name (:source-ref row))`.
- `render-bundle-text` heading: the CONTRACT §8 `"<display-name>"` slot (previously
  mis-filled with `container-kind`, WP2 INPUTS §5.3) now renders
  `(or (bundle-display-name tb) container-kind "")`. The `== <tid>` heading is
  UNCHANGED — the hash stays the honest anchor; trail-view gate 13 stays green.

**`trail_view_test.clj` — two additive deftests (G9):**
- `g9-display-name-helpers-test` — pure-fn matrix: basename, commit-ref
  classification, sha7, feed-name rules, `subject:` parse, bundle-name rules
  (incl. nil-fallback honesty).
- `g9-feed-and-view3-names-test` — own IPC: ingests a commit-shaped source via the
  EXISTING `md/source-ingest-request` with source-ref `git-commit:<40-hex>` and a
  §3.A-format body (R1/R2-validated synthetic source-ref path; git_spine NOT
  required) plus a plain md doc; asserts (a) md feed entry carries basename,
  (b) commit feed entry carries `<sha7>`, (c) bundle rendering shows
  `<sha7> · <subject>`, (d) View-3 shows names where display-names exist while the
  raw tid heading remains. Barrier: OC is a stream topology → the existing
  `oc-ingest!` decision-await harness is the correct settle (no relation writes in
  this fixture, so the microbatch counter barrier is not needed). Feed window
  spans wall-clock now because `$$source-ingest-completions-by-ref` rows stamp
  `completed-at-ms = core/now-ms` in-topology (`object_container.clj`
  `source-ingest-completion-row`).

## G9 verdict

**PASS.** `app.server.rama.trail-view-test` standalone: 3 deftests, 112 assertions
at first pass — 115 final after the blank-input hardening (see Finals) —
0 failures, 0 errors (pre-existing gate test + both new G9 tests). `file(1)`:
both owned files "Clojure module source, Unicode text, UTF-8 text"; the `·`
is C2 B7 in both; zero NUL bytes.

## Cross-package finding (v1.2 allowlist gap #3) + adjudication + path taken

**Finding.** The contract-mandated feed `:display-name` broke
`fixture-fidelity-test` — `test/app/client/workspace/trail_face_test.clj:590`
(assert inside `shape-keys=`; call site 635): hand/live key-set parity on
`:entry/target`, empirically `(not (= #{:id :kind} #{:id :kind :display-name}))`.
The fixture `test/resources/trail_face/feed.edn` and that test are view-mvp
deliverables — outside P3's allowlist; the parity gate did its designed job on a
real cross-package shape change. v1.2 did not anticipate this seam (third
contract gap today; to close-out scoring per HQ).

**Adjudication (HQ, contract author, in-phase):** implementer-fixable; allowlist
extended to `feed.edn` re-derivation ONLY, exact parity semantics preserved;
STOP before any `trail_face_test.clj` edit (trail-room R-1 is concurrently
editing that namespace — confirmed in-tree: the file is locally modified).

**Path taken.**
1. `feed.edn` re-derived (done): the `:source-ingested` target gains
   `:display-name "april.md"`, the `:transcript-file-updated` target gains
   `:display-name "ab12.jsonl"` — each value is the live constructor rule applied
   to that entry's own adjacent detail (`basename` of `source-ref`/`file-path`).
   Relation-transition targets deliberately carry NONE — the live feed emits none
   there; inventing one would make the fixture lie. Provenance comment added at
   the top of the fixture. The fixture edit introduced zero new failures
   (trail_face suite: 18 tests / 429 assertions / the 1 known failure only).
2. Fixture-only path is empirically INSUFFICIENT — the residual failure is the
   test's own pairing logic, not the fixture: `fixture-fidelity-test` compares
   `(first (:feed/entries feed))` (line 615 — a relation-transition, correctly
   WITHOUT `:display-name`) against `(first (:feed/entries live-feed))` (line 611 —
   the highest-arrival live entry, a wall-clock source/file entry, correctly WITH
   it). Kind-heterogeneous pairing was vacuously safe while all target shapes were
   identical; display-name makes target shape kind-dependent, which is the new
   truth. **Needs `trail_face_test.clj` edit** — per the collision guard: STOP,
   HELD until R-1 completed.
3. UNHOLD (HQ, after R-1 reported done; line numbers re-grepped before editing —
   unmoved): the recorded minimal fix applied to `fixture-fidelity-test` ONLY.
   Pairing is now same-KIND — a `first-of-kind` helper picks the first hand and
   first live entry per kind; per pair, the three exact key-set checks
   (entry / target / actor) run through the UNTOUCHED `shape-keys=` (exact
   equality, never subset). Guard against vacuousness: an explicit assertion
   requires `:relation-transition` + `:source-ingested` in the live feed (both
   provably produced by `build-fixture!`); `:transcript-file-updated` is
   compared only when a live counterpart exists, because
   `$$transcript-file-offsets` is fed ONLY by the transcript-file-state depot
   (ops-topology `source>` block, verified by read), which the fixture never
   drives — and the old first-vs-first check never compared that kind either
   (it compared exactly ONE arbitrary pair). Net: the parity gate is strictly
   STRONGER (two kinds × three exact checks + guard, vs one arbitrary pair).

**Alternatives rejected** (for the record): reordering `feed.edn` to
arrival-desc would put a display-name entry first but breaks the
`(nth entries 1)` source-ingested assumption in the terrain/two-clocks tests
(still a test edit, plus wider fixture blast radius); adding `:display-name`
to relation-transition fixture targets would lie about live truth and was
explicitly outside the adjudication.

## Suite state (composed run, all 23 test namespaces, one JVM)

Baseline composed run (pre-fixture-edit): 173 tests / 2019 assertions / 8
failures / 0 errors. The 8 = 1 × fixture-fidelity (above) + 7 × 
`claude-stream-json-adapter-fold-test` (`dogfood_llm_test.clj:337-345`,
`:claim-state nil` executor-claim shape). The llm 7 are NOT P3-caused:
no dependency path from that namespace to trail-view (its ns requires only
`dogfood.llm` + stdlib), and the namespace passes ALONE in this same working
tree (12 tests / 114 assertions / 0 failures) — a one-JVM suite-composition
artifact under 23 namespaces' IPC load, pre-existing relative to this diff.

**Policy change mid-phase (Sid, via HQ, throughput):** the composed full-suite
re-run was ABANDONED mid-flight on instruction; namespace-level finals are the
phase finals. Full-suite composition (G10) is HQ's final integration phase —
carry the llm one-JVM-composition observation above into it. The abandoned
run's JVMs were killed and verified gone (stale-IPC quirk guard).

## Finals (namespace-level; post-hardening, post-pairing-fix)

- `app.server.rama.trail-view-test` — **3 tests / 115 assertions / 0 failures /
  0 errors** (pre-existing gate test + `g9-display-name-helpers-test` +
  `g9-feed-and-view3-names-test`). Includes the falsification-pass hardening:
  `basename`/`sha7` return nil on nil/blank input, so a degenerate ref can
  never render a blank title — `(or display-name id)` always falls back to
  the id.
- `app.client.workspace.trail-face-test` — **18 tests / 432 assertions /
  0 failures / 0 errors** (includes R-1's added gates and the per-kind
  fixture-fidelity parity).
- `file(1)` reports text for every file this phase touched (G12).

## Files touched by P3 (final list)

- `src/app/server/rama/trail_view.clj` — new pure fns `commit-source-ref?`,
  `source-ref->sha`, `sha7`, `basename`, `source-ref->display-name`,
  `subject-line`, `bundle-display-name` (+ private `commit-source-ref-prefix`);
  `:display-name` wired into `file-activity-entry` + `source-activity-entry`;
  `render-bundle-text` heading fills the §8 display-name slot via
  `bundle-display-name`, container-kind fallback preserved.
- `test/app/server/rama/trail_view_test.clj` — additive G9 deftests + hardening
  assertions.
- `test/resources/trail_face/feed.edn` — HQ-authorized re-derivation
  (display-name on source/file targets; provenance comment).
- `test/app/client/workspace/trail_face_test.clj` — HQ-authorized post-R-1
  pairing fix inside `fixture-fidelity-test` ONLY (same-kind parity;
  `shape-keys=` semantics untouched).
- `docs/current-mental-model/build/git-spine/PHASE_P3.md` — this artifact.
