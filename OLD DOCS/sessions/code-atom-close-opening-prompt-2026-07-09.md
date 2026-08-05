# Code-Atom CLOSE — fix window → commits → retro (paste into a FRESH session; Opus-grade is fine)

You are the **close session** for the **code-atom work package**. The gate
PASSED (2026-07-09, Fable — `code-atom/GATE_REVIEW.md`) and Sid has
**countersigned every recommended answer** (decisions.md, "code-atom GATE
REVIEW" entry, COUNTERSIGNED note): the G-F2 kernel branch is AUTHORIZED, the
N3/N4/N6 fixes are ruled, and **commit word is GIVEN** with the recommended
three-way split. Your job: apply the small fix window, re-green everything,
re-run the receipt, make the code commits, then close + retro + adversarial
retro-recheck.

Package folder: `docs/current-mental-model/build/sense-line-mvp/code-atom/`.

Read, in order:
1. `docs/sessions/next-prompt.md` — ONLY the code-atom NOW block (the GATE
   entry is last). Skip the block-kernel sections.
2. `code-atom/GATE_REVIEW.md` — findings G-F1/G-F2/N3–N6 with line cites; §10
   open doubts; the Appendix receipt script you will re-run.
3. `code-atom/CONTRACT.md` + `SPEC.md` — binding. `FIXWAVE.md` +
   `DIFF_FALSIFICATION.md` — prior records, input not authority.
4. `decisions.md` — the gate entry + its COUNTERSIGNED note (your authority for
   every edit below; cite it, don't re-derive).

Hard rules (unchanged): NEVER open `src/app/server/env.clj` · code and docs in
SEPARATE commits · docs commits automatic on the local docs branch, never
pushed/merged · another package (block-kernel) has LIVE uncommitted edits in
this same tree — touch NOTHING of theirs (`block_distiller.clj`, the
`:grounds :assembled-from :refines` registry lines, block fixtures).

## A · Fresh-context look at the gate-authored G-F1 diff (you are the reviewer)

The gate session both found and fixed G-F1; per the QC model its fix needs one
fresh-context falsification look — that's you, before you build on it. The
diff: `clojure_adapter.clj` (parse guard in `clojure-form-v0` + the
`parsed-form-cut` split + `declare`), `code_atoms.clj` (`cut-named-units`
`:parse-error` passthrough; `:blobs-unparseable` count in `code-sync!`), tests
(`gf1-*` in both nss; `:blobs-unparseable 0` added to the three exact-map
pins). Default-fail: trace it, try to break it (e.g. does a parse-failed blob
at HEAD reach the analyzer? — `head-file-context` cuts it to `:units []`, every
usage in that file goes `:usages-unmapped`; confirm). Record the verdict in
your close artifact. If it breaks: stop-clause, do not proceed to commits.

## B · Fix window (ALL pre-authorized — cite the countersign, no new rulings)

1. **G-F2** — in `object_container.clj` `extract-object-key` (:284-339) add ONE
   additive branch, exactly the `imp:md:` shape:
   `(str/starts-with? s "imp:clj:") (leading-object-key (subs s 8))`
   ("imp:clj:" is 8 chars — count, don't copy `imp:md:`'s 7). Regression in the
   adapter test ns (it has an IPC launch): after a G1 import,
   `(ocr/read-import-completion runtime (:import/key req))` returns the
   completion row (pre-fix it partitions by the whole key and reads nil on a
   multi-task cluster — assert on the existing 4-task launch).
2. **N3** — `reconcile-edges!`: `:reconcile-basis-missing` must be 1 ONLY when
   the prior basis is ABSENT (nil), not when a prior pass legitimately desired
   zero edges (`{}`). Thread `prior-basis` nil-ness through (don't `(or … {})`
   before the check). Update G10's basis atom to `(atom nil)` (the honest
   "no prior pass" caller shape) so D1's `:reconcile-basis-missing 1` pin still
   BITES; add one step asserting a zero-edge pass then a next pass reports 0.
3. **N4** — no leaked temp dir on ANY `analyzer-sync!` throw path: today
   `materialize-head-tree!` runs OUTSIDE the try/finally (code_atoms.clj — see
   GATE_REVIEW §6). Restructure so the dir is deleted even when materialization
   itself throws mid-write (e.g. mint the dir first, pass it in, finally-delete
   from the caller). Inspection-grade is fine; no regression required.
4. **N6** — `:analyzer-basis` docstring: one basis atom per (repo,
   `path-filter` scope); reusing an atom across scopes over-retracts edges that
   merely left the analyzed scope. Docstring only.

## C · Re-green + receipt

- All three suites: `clojure -M:test -e "(require '[clojure.test :as t] '<ns>)
  (t/run-tests '<ns>)"` — expect adapter ≥9t (G-F2 regression adds),
  code-atoms ≥9t (N3 step adds), `relation-kernel-test` **2t/222a/0f
  untouched**. Gate-time baselines: 9/88 · 9/181 · 2/222.
- Re-run the receipt (GATE_REVIEW Appendix, verbatim into a scratch file).
  Expect: sync completes 895+/895+ (HEAD may have moved), `:blobs-unparseable
  5`, `RECEIPT-DONE`, and the four printed sections shaped like §8.

## D · Code commits (word GIVEN — recommended split; code-only, this branch)

⚠ The `relation_kernel.clj` registry hunk holds BOTH packages' kinds. Stage the
partial hunk (`git add -p` edit-mode, or a hand-built patch) so ONLY the
`;; code-atom …` comment lines + `:requires :calls` land; verify with
`git diff --cached relation_kernel.clj` that `:grounds :assembled-from
:refines` and their comment stay UNSTAGED. Then:

1. `clojure_adapter.clj` + `clojure_adapter_test.clj` + `test/resources/code-atom/`
2. `code_atoms.clj` + `code_atoms_test.clj`
3. `deps.edn` (two additive coordinates) + the split `relation_kernel.clj`
   registry lines + the `object_container.clj` `imp:clj:` branch (G-F2,
   authorized)

No `Co-Authored-By` lines (standing rule). After committing, `git status` must
still show block-kernel's files modified/untracked — untouched. Sid
cherry-picks to main himself; do not push, do not merge.

## E · Close + retro (work-package skill "Closing a package")

1. `code-atom/RETRO.md` from the FULL trail (NOW log + P0/P1/P2/P3 artifacts +
   DIFF_FALSIFICATION + FIXWAVE + GATE_REVIEW + the committed source): QC-layer
   scorecard (what each layer caught/missed and the cost — note G-F1: four
   layers structurally could not see it, the §12 receipt caught it in a
   minute); "what the next contract should do differently", each rule traceable
   to a concrete failure HERE (e.g. a spike's whole-repo claim must name its
   VERB — enumerate ≠ parse ≠ ingest); mechanisms that earned their keep;
   residue = GATE_REVIEW §10's open doubts + consumer disciplines for the next
   package.
2. **Adversarial retro-recheck as a FRESH subagent** (never self-recheck):
   verify every scorecard claim against the artifacts, the git trail, and a
   fresh suite run; correct the retro in place.
3. Route the lessons: coding gotchas → `memory/implementation-quirks.md`
   (G-F1: git history holds committed-BROKEN blobs — any whole-history parser
   isolates per blob and counts, never aborts); process/skill amendments
   (/work-package, /atomize) as PROPOSED — Fable signs canon, don't self-sign;
   evaluation notes → decisions.md D-006; baton NOW entry (≤15 lines) with the
   next queue state. Commit docs as you go (docs-only commits).

⚠ **T11 standing:** block-kernel re-runs ITS gates after your commit 3 lands
(trivial rebase of the registry literal). Note it in your baton entry so the
block fix-pass session sees it.
