---
name: work-package
description: >-
  The one-pass law + the corners for Softland work packages: contract = the
  hard thinking (one session, one document, one-two screens); everything
  after is execution straight through; acceptance is Sid's word; two
  Sid-touches per atom. Use when cutting a contract, implementing an
  atom/package, or closing one. Validation ladders, gate sessions, and stop
  codes are dead.
---

# Work Package — the one-pass law

Rewritten 2026-08-06 on Sid's throughput ruling (the image atom: 13 process
documents and a 72KB contract around a 372-line namespace; full history in
git). Packages opened before 2026-08-06 finish under their opened contracts.
**This file has a size budget: ~100 lines, forever. Adding a rule means
deleting one** (corner: no rule without a corpse, no corpse without expiry).

## The law (Sid, 2026-08-06)

**Contract = the hard thinking; everything after is execution.**
**Two Sid-touches per atom — the cut and the accept. A third means the
process is broken, not diligent.**

### The contract — one pass, one session, one document, one-two screens
Overflow means think harder, never write more (enumeration is what
incomplete thinking looks like). It carries exactly:
- scope in plain words — what the atom is and is not;
- the named REFUSALS, one line each — what it deliberately does not do;
  each an extension point routed to LATER, never a void (an atom whose
  refusals can't be named hasn't seen its edges);
- the few laws/invariants (pointers to W1/engine docs, never restatements);
- exact entry points — files, namespaces, functions; new code in its OWN
  namespace; big files (`renderer.cljs`, `ground.cljs`,
  `electric_flow.cljc`) get thin hooks only;
- the 3–5 decisive scenarios — frozen as tripwire tests at close;
- real MUST-NOTs only (env.clj, protected files). Nothing conjunctive.
It ends with the implementer's opening prompt. No validation round runs on
it; open questions are ruled in-session or handed down as a default + note.

### Execution — straight through
The implementer builds the WHOLE atom in one lane: build → surface bugs →
fix in-session. The implementer's own falsification pass stays — it catches
real bugs (image atom: 3; the P7 ledger: every real catch came from driving
the seam live or probing a NEW claim, never a suite re-run). Ambiguity takes
the strongest default plus a note; a genuine fork (two readings that cannot
both hold) is ONE question in the thread file — note it, route around it,
keep building. Never a stop-code halt. Parallel lanes split ATOMS, never
share one question: parallelism buys coverage, never confidence —
confidence escalates modality (reading → runtime → profile), never
head-count.

### Close of an atom
- Freeze the contract's scenarios as 3–5 tripwires + 2–3 representative
  goldens. Minutes, not matrices — no Cartesian banks.
- FOCUSED suite only; the full repo suite is never a per-atom gate —
  foreign failures are board debt: record them, pass by.
- Changed-file list diff-derived (`git diff --name-only`), never memory.
- One NOW entry (≤15 lines) + board line flip. Self-audit in the entry: if
  this session wrote more process-artifact lines than code lines, say so.
- **Acceptance = Sid's word. No other acceptance exists.**

### Once per package — the seam courtroom
Full repo suite · cross-atom integration driven as SEAMS — real material
through the real artery, on screen (the kill record lives BETWEEN
independently-green subsystems) · broad goldens where they earn it · the
felt/lived pass (decisions.md lived-gate law). The felt receipt is the
primary receipt for projection work: **never a third consecutive dark
atom** — by then felt activation opens or the lane waits for Sid.
Escalation beyond this law is Sid's explicit call; redundant verification
is purchased insurance priced by IRREVERSIBILITY — durable/Rama work keeps
its ladders, reversible projection work buys once per package.

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
`docs/<package>/` holds exactly two living documents per atom: CONTRACT.md
and NOW.md (STANDING frozen at open · NOW entries ≤15 lines). The board gets
one pointer line per thread. Rulings live in decisions.md. That is all.

## Field notes that earned their keep (cheap lines, expensive bugs)
- Thread-file precedence: the baton never outranks CONTRACT.md or
  decisions.md — flag drift in a NOW entry, keep moving.
- Whole-corpus processors: one receipt over the FULL real corpus, ASSERTED
  (only assertion catches silent class gaps).
- GPU/perf receipts state the adapter/device identity first (SwiftShader vs
  hardware reclassifies every number).
- A suite that reads git HEAD dynamically pins a specimen, or is re-run
  after the closing commit moves HEAD.
- Commits: Sid's word; code and docs separate, both on
  `docs/current-mental-model-local`; never push.
