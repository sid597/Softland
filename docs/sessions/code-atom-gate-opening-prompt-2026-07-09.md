# Code-Atom GATE REVIEW — opening prompt (paste into a FRESH Fable session)

You are the **gate-review session** for the **code-atom work package**. All three
implementation phases are GREEN, adversarial falsification is done, and the FIX
WAVE (6 CONFIRMED + 2 PLAUSIBLE findings) is applied and green. Your job is the
/work-package gate: independently re-verify, read all package code in full,
falsify, rule G11, record the verdict — then drive the definition-of-done
receipt, then close + retro. **You are the FRESH CONTEXT the QC layer requires**
— the fix wave's author (an Opus session) deliberately did NOT self-gate.

Read, in order (the code-atom package folder is
`docs/current-mental-model/build/sense-line-mvp/code-atom/`):

1. `docs/sessions/next-prompt.md` — ONLY the "Active work package #2: code-atom"
   STANDING/NOW block (top of file). Skip everything below it.
2. `code-atom/FIXWAVE.md` — what the fix wave changed per finding, receipts, and
   the ONE judgment flagged for YOUR adjudication (F1: in-memory basis, not a
   durable file) + the recorded residue.
3. `code-atom/DIFF_FALSIFICATION.md` — the 8 findings the wave answers (each with
   its original line cite + scenario).
4. `code-atom/CONTRACT.md` (v1 — rulings R1–R7, traps T1–T11, gates G1–G11) +
   `code-atom/SPEC.md` (v0). **BINDING.** Phase records PHASE_P0/P1/P2/P3.md are
   input, NOT authority.
5. The CODE, IN FULL (this is what you judge — not the phase prose):
   `src/app/server/rama/object_container/clojure_adapter.clj` ·
   `src/app/server/rama/code_atoms.clj` · `test/app/server/rama/code_atoms_test.clj`
   · `test/app/server/rama/object_container/clojure_adapter_test.clj`. The one
   authorized registry edit is `relation_kernel.clj:58-68` (`:requires :calls`).
6. `docs/current-mental-model/decisions.md` — the "code-atom FIX WAVE" D-006 note
   (the F1 durable-vs-in-memory judgment + cross-boot deferral you must weigh).
7. Load **/work-package** + **/rama-pitfalls** skills. Harness invariants:
   `memory/implementation-quirks.md` (esp. durable-side-state × ephemeral cluster;
   deterministic microbatch barrier; physical PState readers for negatives).

Hard rules (unchanged): NEVER open `src/app/server/env.clj` · CODE commits ONLY on
Sid's word (a gate PASS is a precondition, not the trigger) · docs commits
automatic on the local docs branch, never pushed/merged · code and docs in
SEPARATE commits · allowlist = the package's four code files + tests + fixtures +
`deps.edn` (additive) + the one `relation_kernel.clj` registry line + package docs;
anything else = stop-clause.

## The gate protocol (/work-package "Gate review")

1. **Re-run ALL THREE suites THIS session** — never take the fix wave's word.
   `clojure -M:test -e "(require '[clojure.test :as t] '<ns>) (t/run-tests '<ns>)"`
   Expected (independently reproduce): `clojure-adapter-test` **8t/82a/0f** ·
   `code-atoms-test` **8t/179a/0f** · `relation-kernel-test` **2t/222a/0f**
   (untouched). Any failure → diagnose before proceeding.
2. **Read every package code file IN FULL.** Spot-check the CONTRACT traps (T1–T11)
   in the diff, byte-level where relevant (T8: the `U+0000` literals must survive
   the cut — grep-check no raw NUL entered any source/doc).
3. **Falsification pass** per the CLAUDE.md review protocol (architecture; failure
   modes attempted with concrete scenarios; writers/readers/clearers per changed
   state; async ordering; error-path cleanup; open doubts). HUNT the fix wave's own
   changes hardest:
   - **F1 basis (the #1 adjudication):** is the in-memory, caller-owned,
     cluster-scoped `:analyzer-basis` atom sound (`reconcile-edges!` /
     `analyzer-sync!`)? Is the cross-boot deferral honest and is
     `:reconcile-basis-missing` a truthful boundary signal? **RULE the judgment:**
     accept the in-memory form, or send it to Sid, or require the durable snapshot.
   - **F3 transition-key:** can a re-assert / retract key ever collide at a fixed
     head, or the `read-relation-detail` history count drift under the microbatch
     barrier? Confirm a byte-identical re-run still converges (G9 spirit).
   - **F4/F5 non-zero paths** are tested at ZERO only (recorded residue): is the
     `:blobs-seen = denied + git-failures + ingested + converged + unresolved`
     identity sound by construction, and the per-blob git isolation leak-free?
   - **F6** per-path deny agreement · **P1** temp-dir cleanup on the throw path ·
     **P2** the synthetic skip-count pin.
4. **G11 (review-time):** express one `docs/architecture/MAP.md` coverage tuple as
   grounds-marks over code blocks — a worked example in the gate artifact, no UI.
5. **Verdict + D-006 evaluation note** in `decisions.md`; write
   `code-atom/GATE_REVIEW.md` (falsification-protocol sections). Open doubts
   recorded non-blocking WITH their cheap falsifier named.

## Definition of done (CONTRACT §12) — the REPL dogfood receipt (at/after gate)

G1–G10 are green as IPC tests; the receipt is the other half of the DoD. Sync THIS
repo end-to-end (`code-sync!` + `analyzer-sync!` over the real tree) and print: the
specimen census, `oc/fixed-width-order-key`'s callers, and `relation-outcome`'s
supersedes chain. Confirms the code works in the real app, not only in tests.

## After gate PASS

CODE commits wait on Sid's word (suggested split: adapter+tests · driver+tests ·
registry+deps — or one package commit, his call). Then **close + retro** (from the
full trail: NOW log + phase artifacts + FIXWAVE + source + gate), **adversarial
retro-recheck** (fresh context — a retro is written by the process it judges), and
**route the lessons** (`/atomize` + work-package skill amendments; coding gotchas →
`implementation-quirks.md`; eval notes → `decisions.md` D-006; fresh baton).

⚠ **T11 coordination:** the block-kernel package holds the SAME `relation-kinds`
registry authorization for `:grounds :assembled-from :refines`. After EITHER
package's code lands, rebase trivially and re-run BOTH packages' gates.
