---
name: work-package
description: >-
  How to run a Softland work package end to end: Fable-authored contract →
  phased implementation by cheaper models (Codex/Opus) → fresh-context
  validation layers → gate review → close + retro. Use when opening a work
  package, writing or amending its contract, running or validating a phase,
  handling a stop-clause escalation, running a gate review (with or without
  Fable), or closing a package with a retro. Complements /rama (which owns the
  phase mechanics for Rama modules); this skill owns the package shell around
  the phases.
---

# Work Package Skill

Provenance: written from `docs/current-mental-model/build/relation-kernel/RETRO.md`
(first full package cycle, closed 2026-07-03) **including its adversarial
recheck addendum** — where the retro body and the recheck disagree, the recheck
is the corrected reading. Per D-006, the package pattern is: Fable does
contracts, adjudication, and gates; cheaper models do phases; nothing else
pulls Fable in mid-package. Amend this skill at each package close.

## What a work package is

One bounded build (so far: a Rama kernel) run against a binding contract, by
multiple models across fresh sessions, with quality enforced by layered
validation instead of trust. The package's artifacts live in
`docs/current-mental-model/build/<package>/`; its session-to-session state
lives in the `next-prompt.md` baton; its rulings live in `decisions.md`.

## Precedence and binding documents

- `decisions.md` (the decision log) and the package `CONTRACT.md` are binding.
- The baton NEVER outranks contract or log. STANDING is frozen at package open
  and WILL go stale (in cycle 1 it said "10 gates" after the contract said 11
  — two sessions flagged the drift inline, cited precedence, and kept moving).
  When the baton contradicts a binding doc: follow the binding doc, note the
  discrepancy in NOW, do not pause.
- IMPLICIT_SPEC and PLAN are derived artifacts. On conflict with the contract,
  the contract governs — but a *genuine* conflict (both readings physically
  implementable, contract ambiguous) is a stop-clause event, never a silent
  pick by the implementing session.

## The five-layer QC model

1. **Contract coherence — fresh-context re-derivation (Phase 0).** A different
   model derives the requirements (operations, invariants, entity×write
   matrix) from the contract. The matrix later powers the test-gap hunt.
   CAUTION (the F2 lesson): a derivation layer can CREATE ambiguity, not just
   find it — it must inherit the contract's partition/colocation context;
   entity descriptions in partition-free prose are how scope conflicts are
   born.
2. **Plan — validation sessions (fresh context, default-fail).** The plan is
   scenario-traced against contract + spec + platform references. Expect FAIL
   rounds; in cycle 1 both FAIL rounds were cheaper than either bug reaching
   code.
3. **Code — executable gates.** The contract's acceptance gates run as IPC
   tests. Gates green is the only definition of done.
4. **Diff — adversarial falsification review.** Implementation validation and
   test validation, each fresh-context, default-fail, tracing with line
   citations, hunting divergence from plan/contract promises (this is the
   layer that caught the 14-seeks-vs-1 read-path divergence).
5. **Everything — daily use (D-001).** The built thing proves itself only in
   use; nothing below this layer closes the loop.

Two rules that hold across all layers:

- **Author self-review never substitutes for a fresh-context layer.** Cycle
  1's Phase-3 self-review was thorough; Phase 4 still found F1.
- **Promises in binding docs don't self-enforce.** The implementer had both
  the "1 seek" promise and the plan's ":all one-read" instruction in front of
  them and still diverged. Validation layers are the mechanism, not the text.

## Opening a package

**1. The contract (Fable writes it).** Must contain:

- Purpose, consumers in order, explicit non-goals/refusals (each refusal an
  extension point, not a void).
- The placement ruling (which module/namespace and why not the alternatives)
  with its reversal cost and the seam that keeps that cost low.
- **Traps ledger**: for each load-bearing choice — naive alternative →
  concrete failure → ruling. Implementers cite trap numbers in code comments;
  the ledger is a live constraint, not documentation. (Also D-006 evaluation
  criterion 1.)
- **Acceptance gates**: numbered, each executable as an IPC test. Style gates
  too — and every style gate names **where it stops**. The cycle-1 F1 plan bug
  came from "typed defrecords" without a boundary; the precise form: *the
  partitioner-read key must be a top-level namespaced key on a plain map
  envelope; typed records may ride inside fields the partitioner never reads;
  records in PStates.*
- **Scope on every uniqueness/identity claim**: any key, journal, or dedup
  mechanism in a partitioned store names its partition scope in the sentence
  that introduces it ("idempotency key" without "(relation-scoped)" cost a
  validation round plus a ruling).
- **Read plans for performance promises**: a promise like "1 seek" enumerates
  the read plan per query shape. This is contract hygiene — it makes the
  promise checkable at plan level; it will NOT stop an implementation
  divergence (Phase 4 owns that).
- **Input manifest**: the exact files/lines any model would need to reproduce
  the contract (feeds the D-006 counterfactual probe).
- Handoff section: implementer, reviewer gate definition, what comes after
  green.

**2. The baton** (`docs/sessions/next-prompt.md`), two sections:

- **STANDING** — the frozen contract-of-engagement: binding docs, the phased
  process, new-files-only allowlist, scope guard, verification duties
  (memory-derived platform claims the implementer must check against on-disk
  references before code), definition of done, stop clause, hard rules
  (commit/env.clj), the **precedence rule** verbatim ("this file is a baton,
  not a source of truth; if it contradicts CONTRACT.md or decisions.md, those
  win — flag the discrepancy in NOW"), and what must NOT start without Sid.
  Do not edit STANDING while the package is active.
- **NOW** — the per-session log, appended at each session end: date, model,
  phase, artifact path, verdict, **findings verbatim on any FAIL**, judgment
  calls flagged for the next reviewer, and the next step. One writer at a
  time. The NOW entry is the trail of record between commits — write it as if
  the phase artifact might be lost (in cycle 1 it once was; the baton saved
  the findings).

**3. The package directory**: `docs/current-mental-model/build/<package>/`
holding CONTRACT.md and every phase artifact.

## Running phases

- One phase per FRESH session. Each phase produces its artifact in the package
  directory. For Rama work the phase mechanics are the `/rama` skill's; this
  shell does not restate them.
- Validation verdicts are **default-fail**; PASS only after explicit scenario
  tracing with citations. `minor-fail` → the authoring phase applies the
  enumerated fixes and the validation phase is NOT re-run. `fail` → the prior
  phase re-runs with the failure artifact as input.
- **Never overwrite a FAIL validation artifact.** Keep per-round files
  (`PLAN_VALIDATION_R1.md`, `_R2.md`, final `PLAN_VALIDATION.md`) or commit
  the round before rewriting. Cycle 1 lost both FAIL rounds' full text; only
  the baton's verbatim findings survived. The log is primary — that applies to
  the package's own trail.
- Tests phase writes + compile-checks only; run-to-green is its own phase. A
  first-invocation green run is the upstream layers' receipt, not luck.
- Test-harness invariants (reuse; gotchas detailed in
  `memory/implementation-quirks.md`):
  - **Deterministic barrier**, never polling: for microbatch,
    `wait-for-microbatch-processed-count` with a submit!==+1 counting
    invariant (every request carries a present routing key so none is
    ingress-dropped). Polling can never prove a no-op ran. Product code may
    ship a polling helper; the suite must not lean on it.
  - **Negative invariants need a physical reader**: public query surfaces mask
    their own bugs (dedup, row-gating). Every "X must NOT have happened"
    assertion names the PState-level read that can actually see X
    (validation-only readers, never product API).
  - **Minimize IPC launches**: one deftest unless a *named shared-mutable-state
    interference* justifies a split (pause/resume blast radius qualifies;
    "different concern" does not).
  - **Task counts injectable**, not only randomized, so partition sweeps are
    reproducible from the committed harness.
  - **No raw control bytes in source literals**: spell NUL as the `\u0000`
    string escape from day one (git classifies raw-NUL files as binary; the
    tool-JSON layer also decodes `\u0000` in edit payloads into raw bytes —
    both traps have fired here).

## Stop clause and escalation

- Fires when the contract cannot be built as specified, is wrong under
  platform semantics, or two binding docs genuinely conflict. The implementing
  session never improvises policy on binding docs.
- First, classify the fork:
  - **Implementer-fixable**: the contract already answers it and the artifact
    drifted (cycle 1: the invented `RelationRequestRow` — reverting to the
    contract's envelope, verified against codebase precedent, needed no
    ruling). Fix, cite the precedent, move on.
  - **Genuine policy fork**: two readings that cannot both hold physically
    (cycle 1: relation-scoped vs global idempotency). Escalate.
- Escalation: record in `decisions.md` Open Questions as PROPOSED — the
  conflict with verbatim citations, the options, a recommendation with
  grounds, and what re-runs under each option. Sid (or Fable on Sid's
  referral) rules.
- **Ruling execution is a sweep, not a banner**: amend the introducing
  sentence AND grep every derived enumeration, count, and list across
  contract, spec, plan, and baton (gate lists, "all N gates", matrices).
  Cycle 1's banner-only spec amendment left "all ten contract gates" standing
  after gate 11 existed; the precedence rule absorbed it, but absorption is
  the backstop, not the plan.
- Re-entry triggers for Fable: **gates green** (→ gate review) or **stop
  clause tripped** (→ ruling). Nothing else.

## Gate review

- Inputs: the green suite + the phase artifacts as prior-pass records — used
  as input, NOT authority. The code is what is judged; read it in full.
- Steps:
  1. Independently **re-run the suite this session** (never take Phase 7's
     word).
  2. Spot-check the contract-named traps in the diff, byte-level where
     relevant.
  3. Falsification Pass per the CLAUDE.md review protocol: architecture;
     failure modes attempted with specific scenarios; writers/readers/clearers
     per changed state; async ordering; error-path cleanup; open doubts.
  4. Record the verdict + D-006-style evaluation notes in the gate artifact
     and `decisions.md`.
- Open doubts are recorded non-blocking **with their cheap falsifier named**
  (e.g. "envelope/payload binding is client trust → one-line server-side
  recheck before agent-authored writers appear").
- **Without Fable**: same protocol, strongest available model, fresh session;
  verdict recorded in `decisions.md`; anything the reviewer cannot
  independently verify is an open doubt, never a pass.

## Closing a package

1. Commit decision is Sid's. Code and docs ALWAYS in separate commits; docs
   only on the local docs branch (never pushed, never merged to main).
2. **Retro**, from the full trail (NOW log + phase artifacts + source + gate):
   - QC-layer scorecard: what each layer caught, what it missed and the cost.
   - "What the next contract should do differently" — rules, each traceable to
     a concrete failure in THIS package (no speculative hardening dressed as a
     lesson).
   - Mechanisms that earned their keep.
   - Residue: tracked non-blocking items **including consumer/importer
     disciplines that must carry into the NEXT package's contract** (cycle 1
     initially dropped the importer-timestamp discipline; the recheck restored
     it).
3. **Adversarial recheck of the retro** before it feeds any skill or binding
   artifact: fresh session, verify every scorecard claim against the
   artifacts, the baton trail in git, and a fresh suite run. Cycle 1's recheck
   caught a residue line stale within minutes, an overcounted cost, and a
   wrong causal story — retros are written by the same process they judge.
4. Route the lessons: coding gotchas → `memory/implementation-quirks.md`;
   process rules → THIS skill (amend it); evaluation notes → `decisions.md`
   D-006; then write a fresh baton for the queue.

## Mechanisms that earned their keep (do not drop)

- **STANDING/NOW split with the precedence rule** — absorbs baton staleness
  without pausing sessions.
- **Fresh-context validation with default-fail verdicts** — every layer found
  something real in cycle 1.
- **Stop clause with escalation path** — one policy fork, one ruling, zero
  improvisation.
- **Pre-registered evaluation (D-006)** — criteria fixed before the work is
  what made honest letter-vs-spirit scoring possible.
- **Traps ledger cited by number in code comments** — constraints that travel
  into the code.
