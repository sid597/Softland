# Code-Atom FIX WAVE + GATE — opening prompt (paste into a FRESH session; Opus-class model is right for the fix wave)

You are the fix-wave session for the **code-atom work package** (all three
implementation phases GREEN and independently verified; adversarial
falsification done). Your job: apply the falsification findings, re-green
everything, then run (or hand off) the gate. Budget context: Fable is
capacity-limited this week — this wave is deliberately cheaper-model work
under a line-cited fix list.

Read, in order:
1. `docs/sessions/next-prompt.md` — ONLY the "Active work package #2:
   code-atom" STANDING/NOW block (top of file). Skip everything below it.
2. `docs/current-mental-model/build/sense-line-mvp/code-atom/
   DIFF_FALSIFICATION.md` — THE authority for this wave: 6 CONFIRMED + 2
   PLAUSIBLE findings with file:line citations and scenarios.
3. `code-atom/CONTRACT.md` (rulings/traps/gates) + `code-atom/SPEC.md`
   (cite, don't re-derive). Phase artifacts PHASE_P1/P2/P3.md as records.
4. Load /work-package + /rama-pitfalls skills. Harness invariants:
   `memory/implementation-quirks.md`.

Hard rules (unchanged): NEVER open `src/app/server/env.clj` · NO git
commits of CODE (Sid's word only; docs commits automatic on this branch) ·
allowlist = the package's four code files + tests + fixtures + package
docs; anything else = stop-clause.

## The fix list (details + scenarios in DIFF_FALSIFICATION.md — read it first)

- **F1 (top) — reconcile stale-edge leak on deletion**
  (`code_atoms.clj:663-668`): current-set read covers only DESIRED
  endpoints' target-keys; an analyzer edge whose BOTH endpoints leave HEAD
  is never retracted (violates R5). Fix shape (spine-cursor idiom): persist
  the previous run's desired-set snapshot (edn file under `data/`,
  driver-side); retract set = old-desired ∖ new-desired ∪ the current
  target-key read. Snapshot loss degrades to today's behavior — MUST be
  counted (`:reconcile-basis-missing 1`) so the map doesn't lie. If you
  judge this policy-grade rather than implementer-grade, STOP-CLAUSE it
  to Sid instead of improvising.
- **F2 — block-path `#N` dedup collision** (`clojure_adapter.clj:142-162`):
  literal var `foo#2` collides with the dedup suffix of a duplicated `foo`
  (REPRODUCED as compiler-legal). Fix: switch the dedup separator to a
  character that cannot appear in a Clojure symbol (`~` — reader-reserved),
  e.g. `foo~2`; regression test with a literal `foo#2` var in a fixture.
  NOTE: unit-ids are per-blob so no migration issue; update SPEC §3.5's
  `#2` mention + CONTRACT R7 in the SAME docs commit (direct replacement).
- **F3 — constant-HEAD reassert-after-retract** (`code_atoms.clj:674`
  area): assert key `code:<rid>:<head>` collides with the original
  assert's journal entry → a G10-style retract followed by re-desire at
  the SAME head leaves the edge `:retracted` while stats claim `+1
  asserted`. Fix: make the key transition-unique deterministically —
  suffix with the count of prior status transitions from
  `read-relation-detail` history (deterministic, replay-stable); count
  stats from actual decision outcomes, not optimistic intent.
- **F4 — ingest timeout uncounted** (`code_atoms.clj:354` +
  `runtime.clj:326` nil): add `:blobs-unresolved` (spine `:unresolved`
  idiom); lineage proceeds (dangling endpoints are kernel-legal) but
  counts `:lineage-over-unresolved` per affected pair.
- **F5 — git exit-code swallow** (`code_atoms.clj:70`): check exit code;
  per-file/per-call isolation with a `:git-failures` stat (spine's
  per-file isolation precedent) — never a silent empty blob.
- **F6 — deny-granularity mismatch** (ingest `:223` vs lineage `:241`):
  unify to per-path checks in BOTH lanes; test with a fake deny entry on
  a tracked path (env.clj itself is gitignored — the test needs a
  synthetic entry via the existing `:deny-list-override`).
- **P1 (plausible) — analyzer temp-dir leak** (`code_atoms.clj:485/501`):
  delete-recursively in `finally`.
- **P2 (plausible) — skip-count only gated at zero**: assert exact
  `:usages-unmapped`-class counts in the G7 test (pin from a run, comment
  the derivation).

Wave shape: apply F1–F6+P1+P2 → extend tests (F1/F2/F3 minimum get their
own regression assertions) → re-run ALL suites green
(`code-atoms-test` · `clojure-adapter-test` · `relation-kernel-test`) →
write `code-atom/FIXWAVE.md` (what changed per finding, receipts) →
docs-commit it (code stays uncommitted) → append a ≤15-line NOW entry.

## Then the gate (route per Sid's budget — ask him ONE line if unclear)

- **Route A (preferred if Fable capacity allows, e.g. post-reset):** a
  fresh Fable session runs the gate per the /work-package skill: re-run
  suites itself, read ALL package code in full, falsification-protocol
  sections, `GATE_REVIEW.md`, verdict + D-006 note in decisions.md; then
  close + retro (+ adversarial retro-recheck) and the /atomize + package
  skill amendments.
- **Route B (without-Fable clause):** THIS session (strongest available
  model) gates with the same protocol; anything it cannot independently
  verify is an OPEN DOUBT, never a pass; Fable countersigns post-reset.

After gate PASS: everything waits on Sid's word for the CODE commits
(suggested split: adapter+tests · driver+tests · registry+deps — or one
package commit, his call). The REPL dogfood receipt (CONTRACT §12
definition-of-done) runs at/after gate: sync THIS repo, print the specimen
census, `fixed-width-order-key` callers, `relation-outcome` supersedes
chain.
