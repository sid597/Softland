---
name: work-package
description: >-
  The one-pass law for Softland work packages: contract = the hard thinking
  (one session, one document, one-two screens); everything after is execution
  straight through by the implementer; acceptance is Sid's word. Use when
  cutting a contract, implementing an atom/package, or closing one. The
  ceremony this replaces (validation ladders, gate sessions, stop codes) is
  dead — see "Dead ceremony".
---

# Work Package — the one-pass law

Rewritten 2026-08-06 on Sid's throughput ruling, after the image atom wrapped
a 372-line namespace in 13 process documents and a 72KB contract, and its
stop code halted an already-acceptable implementation over five foreign test
failures. The prior 610-line process machine is in git history; it had
absorbed two "less process" amendments and grew longer both times. This file
is the whole law now. Packages opened before 2026-08-06 finish under their
opened contracts (their NOW files govern).

## The law (Sid, 2026-08-06)

**Contract = the hard thinking; everything after is execution.**

### The contract — one pass, one session, one document, one-two screens
It carries exactly:
- scope in plain words — what the atom is and is not;
- the few laws/invariants the work must honor (pointers to W1/engine docs,
  never restatements);
- exact entry points — files, namespaces, functions; new code goes in its
  OWN namespace; big files (`renderer.cljs`, `ground.cljs`,
  `electric_flow.cljc`) get thin hooks only;
- the 3–5 decisive scenarios — the ones frozen as tripwire tests at close;
- real MUST-NOTs only (env.clj, protected files). Nothing conjunctive.

It ends with the implementer's opening prompt. No validation round runs on
it. An open question surfaced while cutting is ruled in-session or handed to
the implementer as a default plus a note — never a second contract session.

### Execution — straight through
The implementer builds the WHOLE atom in one lane: build → surface bugs →
fix them in-session. The implementer's own falsification pass stays — it
keeps catching real bugs (image atom: 3; the P7 ledger: every real catch
across eight packages came from driving the seam live or probing a NEW
claim, never from a suite re-run). A genuine fork — two readings that cannot
both hold — is ONE question in the thread file: note it, route around it,
keep building what is unblocked. Never a stop-code halt.

### Close of an atom
- Freeze the contract's named scenarios as 3–5 tripwire tests + 2–3
  representative goldens. Minutes, not matrices — no Cartesian banks.
- Run the FOCUSED suite for what you touched. The full repo suite is never
  a per-atom gate; foreign failures are board debt — record them, pass by.
- Changed-file list diff-derived (`git diff --name-only`), never from
  memory.
- One NOW entry (≤15 lines) in the thread file + board line flip.
- **Acceptance = Sid's word. No other acceptance exists.**

### Once per package — not per atom
Full repo suite · cross-atom integration · broad goldens where they earn
it · the felt/lived pass on real surfaces (decisions.md lived-gate law).
Escalating any atom beyond this law is Sid's explicit call — never
self-authorized by a contract, a session, or a model.

## Dead ceremony — never to return
Validation ladders/rounds (R1/R2/…), default-fail verdicts, recut ledgers,
ruling packets, merged rulings, traps ledgers cited by number, input
manifests, gate matrices, per-phase gate partitions, stop codes (S1/S2),
conjunctive full-suite gates ([JVM-FULL]-as-gate), allowlist partitions,
phase artifacts beyond the thread file, PLAN.md, fresh-context re-derivation
of contracts, second-model verification of implementations, separate
gate-review sessions, per-package retros with adversarial rechecks.

A new contract that reintroduces any of these is wrong by definition. Do
not imitate closed packages' contracts (IMAGE-ATOM, SEAM-STEP1, shaping
correction) — they predate this law. This law also outranks per-domain
phase ladders (EXCEPT the rama and rama-retro skills) for package acceptance;
those skills serve their own domains' internals AND FOR RAMA SHOULD BE FOLLOWED.

## Files
`build/<package>/` holds exactly two living documents per atom: CONTRACT.md
and NOW.md (STANDING frozen at open · NOW entries appended, ≤15 lines each).
The board gets one pointer line per thread. Rulings live in decisions.md.
That is all.

## Field notes that earned their keep (cheap lines, expensive bugs)
- Thread-file precedence: the baton never outranks CONTRACT.md or
  decisions.md — flag drift in a NOW entry, keep moving.
- Whole-corpus processors: one receipt over the FULL real corpus, ASSERTED
  (run-and-print catches aborts; only assertion catches silent class gaps).
- GPU/perf receipts state the adapter/device identity first (SwiftShader vs
  hardware reclassifies every number).
- A suite that reads git HEAD dynamically pins a specimen, or is re-run
  after the closing commit moves HEAD.
- Durable-touch work stays CUTOVER-CLASS (decisions.md boundary test) —
  that is architecture, not ceremony.
- Commits: Sid's word; code and docs in separate commits, both on
  `docs/current-mental-model-local`; never push.
