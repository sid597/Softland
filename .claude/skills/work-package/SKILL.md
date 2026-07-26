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

**Amended 2026-07-05 mid-package on Sid's direction** (trail-view WP1 /
view-MVP WP-B2 both live): the one-phase-per-fresh-SESSION cadence is
replaced by one-phase-per-fresh-CONTEXT (see Running phases). Form-break
evidence, recorded in D-006 evaluation notes: Phase A of WP1 = 227 changed
code lines against ~300KB of process docs across the two packages; the
operator reported disorientation on BOTH tracks; validation layers' marginal
catch-rate fell across cycle 2 (plan validation R1 PASS-with-advisories,
A1–A3 first-run green, zero stop-clauses) while the per-session orientation
cost stayed constant. The old cadence's token ledger priced Sid's attention
at zero. QC layers themselves are unchanged — they keep their kill record.

**Amended 2026-07-10 at the code-atom + block-kernel closes** (both retros +
their adversarial rechecks are the sources; every added rule cites its
concrete failure inline — no speculative hardening).

**Amended 2026-07-13 at the scene-substrate close** (`build/scene-substrate/
RETRO.md`; recheck pending Sid's cost call). Three contract-hygiene rules,
each from a concrete failure in that package:
- **GPU/perf gates open with an environment attestation.** The receipt's
  FIRST field is the adapter/device identity (e.g. WebGPU
  `isFallbackAdapter` + description); a perf number without it is
  unclassifiable. (Scene-substrate G4's 91ms FAIL was SwiftShader — Chrome
  rendering on CPU; one full diagnosis cycle proved the code innocent.)
- **A gate letter naming an external system pins its executable form in the
  same sentence.** "Resolves through existing read APIs" shipped with no
  named test shape; the executable half silently became the weaker in-store
  claim (gate F7).
- **Any echo/refresh trigger names its KEYING SOURCE.** "Rebuild on
  projection change" never pinned which atom's identity MEANS
  projection-changed; the implementation keyed a fast-flipping identity
  while rebuilding from a slow-changing source — per-keystroke full repacks
  plus seconds-stale copies (gate F1, the package's top finding).

**Amended 2026-07-13 at the machine-cut close** (retro is the source; its
adversarial recheck is deferred on cost — Sid's call — so these rules carry
that caveat): falsification-batch sizing, transition-keyed identity,
cross-package pinned scans, inherited wearing.

**Amended 2026-07-17 at the durable-ground close** (`build/durable-ground/
RETRO.md`; recheck deferred on cost — Sid's call). Ops-heavy packages (live
external systems, drills as the wearing layer) add four rules, each from a
concrete failure that day:
- **Ops scripts gating on an external state machine wait on the TERMINAL
  marker, never a sleep or a CLI exit code** — an interrupted
  `shutdownCluster` persisted in ZK and RESUMED on the next boot, killing
  every worker (P5b); the completing CLI's own poll dies once daemons
  self-exit, so its exit code lies (P6). And in `set -e` scripts, wait-loops
  use if-forms — a no-match `grep -q X && break` aborts the script silently
  (the first backup run died exactly there).
- **Failure-drill receipts are SERVER-READ receipts.** A client-side
  snapshot captured across a kill drill can be the frozen cache of a session
  the drill itself killed — P5a's "restored" snapshot came from a dead
  Electric session; the cluster read told the truth (and a stronger one).
- **Bank receipt numbers at capture time; /tmp probe rigs are disposable.**
  A tmpfiles sweep deleted two sessions' scratchpads mid-close (scripts, raw
  results, screenshots); zero evidence was lost only because every number
  was already in the conversation/NOW. The reusable rig FACTS (flags, traps,
  interop shapes) go in the thread file, not the scripts.
- **Size guesses about durable state are measured at first contact** — the
  contract said backup snapshots are "MBs"; reality was 1.6GB quiesced/3.0GB
  live. Harmless here; a cadence decision made on it would not have been.

**Amended 2026-07-26 at the space-as-entity close** (`build/space-as-entity/
RETRO.md`; adversarial recheck ran and its corrections are applied in the
retro body). One rule, one concrete failure:
- **A per-phase gate partition sum-checks against the contract's full gate
  list at the moment the partition is made** (staging, a ruling, or a phase
  prompt), and any gate needing a specific environment or actor is assigned
  its OWNER in the same breath. Grounds: contract gate G7 (echo bar — needs
  Sid's headed browser on this box) appeared in NO phase's gate list (P1
  chose G1/G2/G3/G10g; RULING R1 routed P2 to G4/G5/G6/G8/G9/G11/G12);
  nothing summed the lists against G1–G12, and the gap surfaced only at
  gate review — after which closing it required an act only Sid could
  perform, discovered at the worst time. Same class as the
  ruling-execution-sweep rule; this extends it to gate PARTITIONS.

**Amended 2026-07-26 at the space-as-entity rung-3 close**
(`build/space-as-entity/RUNG3_RETRO.md`; adversarial recheck ran — two
substantive + six precision corrections, applied in the retro body). Two
rules, one concrete failure each:
- **The phase artifact's changed-file list is diff-derived and sum-checked
  against the contract allowlist at artifact-writing time** — `git diff
  --name-only` (plus status for new files) at phase end, every path
  classified allowlist / new-per-contract / DRIFT-flagged, never
  reconstructed from memory. Grounds: rung-3 P1's artifact said "seven
  deliverables, exactly" while the tree carried an eighth item outside
  them (`src-dev/dev.cljc`'s LAND_PINNED guard — a legitimate harness
  need, default path behaviorally the old code); no phase layer flagged
  it; the gate found it only by reading the full diff, so it reached the
  commit decision as a gate finding instead of an implementer disclosure.
- **A gate precondition the package itself destroys is marked ONE-SHOT in
  the contract, and its committed harness asserts the post-package
  invariant instead.** Grounds: R3-G1's "no space instance anywhere"
  ended forever at the package's own G3 (the durable instance master is
  append-only; release = holds/inherit); the committed harness's
  `:no-space-instance?` check is permanently unsatisfiable, and the gate
  had to re-derive the strictly-harder released-instance claim by hand.

**Amended 2026-07-27 on Sid's cadence ruling** (at the multi-cascade P1
gate: "too much process … retros … do them in batch for a few phases …
I don't even think gates provide much value doing it for each and every
phase"). Two CADENCE changes; the QC layers themselves are untouched —
executable gates in-phase, the single fresh falsifier, and the wearing
keep their kill record and stay per-package:
- **Gate review is TIERED.** Small additive-and-dark MECHANISM slices
  get a SLIM gate: spot-check the contract traps in the diff, re-run the
  FOCUSED suite, verify the one contract-critical live receipt; verdict
  + residue recorded as usual. FULL independent re-verification (fresh
  full-suite run, independent live re-drive, fence re-hash) is reserved
  for cutover-class or durable-touch work, or when the falsifier /
  wearing / stop-clause flagged something. Grounds: multi-cascade P1
  (2026-07-26) — the full ceremony found ZERO code defects; the phase's
  one fresh falsifier had already caught all three real defects at a
  fraction of the cost; the rung-3 slim gate was likewise confirmatory.
  Same form-break class as the 2026-07-05 cadence amendment (the token
  ledger priced Sid's attention at zero).
- **Retros run in BATCH, not per package.** Per-package close = the
  residue/open-doubts note in the gate record (already standard), the
  mechanical post-commit HEAD-dynamic re-run, and the board prune. ONE
  batched retro + ONE adversarial recheck at a natural boundary —
  dark-lane activation, a stratum milestone, or ~3–4 packages,
  whichever lands first — reading the banked NOW/GATE trails. Grounds:
  retro yield concentrates into 1–2 rules then declines; the one
  unambiguous recheck save (code-atom's red HEAD-dynamic suite at
  committed HEAD) is already absorbed as the mechanical close rule;
  two rechecks were deferred on cost (machine-cut, durable-ground)
  with no observed damage.

## What a work package is

One bounded build (so far: a Rama kernel) run against a binding contract, by
multiple models across fresh sessions, with quality enforced by layered
validation instead of trust. The package's artifacts live in
`docs/current-mental-model/build/<package>/`; its session-to-session state
lives in the `next-prompt.md` baton; its rulings live in `decisions.md`.

## Precedence and binding documents

- `decisions.md` (the decision log) and the package `CONTRACT.md` are binding.
- The baton (the state board + per-package thread files, since 2026-07-10)
  NEVER outranks contract or log. STANDING is frozen at package open
  and WILL go stale (in cycle 1 it said "10 gates" after the contract said 11
  — two sessions flagged the drift inline, cited precedence, and kept moving).
  When the baton contradicts a binding doc: follow the binding doc, note the
  discrepancy in NOW, do not pause.
- IMPLICIT_SPEC and PLAN are derived artifacts. On conflict with the contract,
  the contract governs — but a *genuine* conflict (both readings physically
  implementable, contract ambiguous) is a stop-clause event, never a silent
  pick by the implementing session.

## The six-layer QC model

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
   **Sizing (machine-cut close, 2026-07-13): the batch defaults to ONE
   finder, aimed at the genuinely-new machinery.** Grounds: machine-cut ran
   three by class; the driver-lifecycle finder found ALL 4 HIGH, the other
   two (serve/fixture drift, boot seams) found 0 HIGH for ~2/3 of the
   ~990k-token batch — and the one real boot-seam defect (C-MC-C2) was
   independently caught live by layer 5. Widen only for a prior kill class
   that the wearing will NOT independently exercise.
5. **First integration-drive — the wearing (a gate, run BEFORE daily use).**
   The built thing is driven live over real material (the `/face`-style first
   end-to-end drive) before it is declared done: lifecycle and data-binding
   gaps live in the seam between independently-green subsystems and are
   invisible to JVM goldens. Grounds: framework G15/G24/G25 caught a real
   lifecycle defect in BOTH waves (W1 epoch-bump, W2 projection-routing) that
   no suite layer could see. (Amendment A, framework retro, signed
   2026-07-12.) **A wearing can be INHERITED from real use** (machine-cut
   G14, 2026-07-13): if the built thing gets driven live inside another
   package's session before its own wearing is staged, harvest the receipts
   (boot log, transcript, the operator's word) instead of scheduling a
   duplicate drive — evidence from unstaged use is stronger, the bar is
   unchanged, and the close session's job becomes verifying receipts.
6. **Everything — daily use (D-001).** The built thing proves itself only in
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
  records in PStates.* A gate's ONE-LINE text carries every scope carve-out
  its authorizing sections carry — the fresh gate enforces the short form
  literally (block-kernel G12: the §8 one-liner lost the v0 carve-out that
  CONTRACT §12/g, PHASE_0 §2/g, and the P5 prompt all carried; Round-1
  strictly-failed it as a stop clause).
- **Gate class — `delay`-totality for lazily-booted runtimes feeding the
  render path**: any `delay`/lazy boot whose handle feeds the render path
  must yield a poisoned-but-TOTAL value on failure, never a cached throw
  (a `Delay` re-throws its cached exception on every deref → one boot
  failure = permanent failure for every client). (Framework W2-F7.
  Amendment C, signed 2026-07-12.)
- **Scope on every uniqueness/identity claim**: any key, journal, or dedup
  mechanism in a partitioned store names its partition scope in the sentence
  that introduces it ("idempotency key" without "(relation-scoped)" cost a
  validation round plus a ruling). **And its TRANSITION story** (machine-cut
  A-F2/F3): "stable per identity" and "stable per transition" are different
  laws — a flat per-edge-forever idempotency key met the contract's letter
  and made assert→retract→re-assert impossible (the journal replays the
  first decision forever). If the keyed thing has a lifecycle, the key
  sentence says which transitions mint a NEW key.
- **A schema that fixes an object's IDENTITY must also fix its
  DATA-RESOLUTION**: if the contract pins how a thing is identified (schema,
  provenance, addressing), the SAME section pins how consumers resolve its
  data (the binding/lookup route) — a static code map left beside a fixed
  schema is how a later wave silently outgrows it. (Framework W2-F1: §8
  fixed assembly identity; the face→projection binding stayed a code map →
  empty scenes, found only live. Amendment B, signed 2026-07-12.)
- **Read plans for performance promises**: a promise like "1 seek" enumerates
  the read plan per query shape. This is contract hygiene — it makes the
  promise checkable at plan level; it will NOT stop an implementation
  divergence (Phase 4 owns that).
- **Coverage claims name their VERB**: any "N/N" (blobs, files, events)
  states enumerate | parse | ingest | analyze in the sentence that makes the
  claim, and the definition of done gates EACH verb the processor performs
  over the full corpus it claims. (Code-atom: "895/895" was enumeration read
  as parse — parse-fidelity was only ever checked on HEAD's 96 files; that
  unnamed verb seeded G-F1.)
- **Full-real-corpus receipt as a phase gate** (whole-corpus/whole-history
  processors): when the deliverable claims coverage of a real corpus, the
  definition of done gates a receipt over the FULL real corpus at the FIRST
  phase that touches it — not only at review — and the receipt ASSERTS the
  processor's completeness invariant against durable state (every history
  blob parses-or-is-counted; every ingested event has exactly one durable
  class row), never merely runs-and-prints. Grounds, two packages: code-atom
  G-F1 — 5/895 committed-broken historical blobs aborted the unguarded sync;
  FIVE cheaper layers were structurally blind (pinned specimens, HEAD files,
  parseable synthetics); the gate's receipt found it in its first minute.
  Block-kernel Round-2 Finding-1 — a surfaceless-river class invisible to
  the fixture; the receipt RAN clean and printed plausible-but-wrong counts
  (244/402 for a true 247/399). Run-and-print catches aborts; only assertion
  catches silent class gaps.
- **A new object-container import-key prefix is a named deliverable**: a
  package minting `imp:<family>:` names the `extract-object-key` routing
  branch (or the handled prefix it rides) as an explicit deliverable WITH a
  foreign-read routing gate. The bug is latent by nature — nothing fails
  until a foreign consumer calls `read-import-completion` on a multi-task
  cluster. (Identical class, two packages: code-atom G-F2 `imp:clj:` →
  `:else`; block-kernel F2 `imp:sense-block:` → `:else`.)
- **Input manifest**: the exact files/lines any model would need to reproduce
  the contract (feeds the D-006 counterfactual probe).
- Handoff section: implementer, reviewer gate definition, what comes after
  green.

**2. The board line + the thread file** (restructured 2026-07-10; Sid's
ruling. `docs/sessions/next-prompt.md` is a STATE BOARD — pointer + status
lines per thread under a direction line; the single shared surface, where
cross-thread coordination flags of the T11 class live. Grounds: the monolith
hit 1,568 lines with ~50 live; the REAL N=2 parallel-package workflow
(code-atom ∥ block-kernel) had both packages writing one shared file; per-boot
load cost. Zero navigation failures were observed — this is a
cost/concurrency amendment, not a rescue; the quality falsifier is
pre-registered in the decisions.md note.) A package gets ONE board line
(name, phase, status, ⚠ flags; one writer per thread file, sessions touch
only their own line plus coordination flags) and its own thread file
`build/<package>/NOW.md`, two sections:

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
  time. **Budget: ~15 lines per entry** (amended 2026-07-05) — the entry is a
  pointer to the phase artifact, not a copy of it. The old
  write-as-if-the-artifact-might-be-lost rule came from a cycle-1 artifact
  loss; the durable fix for that is committing the docs trail (now Sid's
  standing practice), not duplicating every artifact into the baton — 2k-word
  entries made the baton illegible to the person it serves. FAIL findings
  verbatim remain the one sanctioned exception to the budget.

**3. The package directory**: `docs/current-mental-model/build/<package>/`
holding CONTRACT.md and every phase artifact.

## Running phases

- **One phase per fresh CONTEXT — not per fresh session** (amended
  2026-07-05). What the QC model requires is that no validation/review layer
  shares a context with the work it judges ("author self-review never
  substitutes for a fresh-context layer"). A fresh-context SUBAGENT satisfies
  this; a separate human-opened session is the fallback when a phase cannot
  fit a subagent, not the default. The default shape is now: ONE orchestrating
  session (Fable when it's already booted — its scheduled gate re-entry cost
  then disappears) runs implementation and launches each validation/review
  layer as a fresh subagent per Sid's standing subagent policy (Opus 4.8;
  judgment stays in the orchestrating context). Model routing tables in a
  baton are advisory on model/effort per phase, never binding on session
  structure. Each phase still produces its artifact in the package directory.
  For Rama work the phase mechanics are the `/rama` skill's; this shell does
  not restate them.
- Validation verdicts are **default-fail**; PASS only after explicit scenario
  tracing with citations. `minor-fail` → the authoring phase applies the
  enumerated fixes and the validation phase is NOT re-run. `fail` → the prior
  phase re-runs with the failure artifact as input.
- **Never overwrite a FAIL validation artifact.** Keep per-round files
  (`PLAN_VALIDATION_R1.md`, `_R2.md`, final `PLAN_VALIDATION.md`) or commit
  the round before rewriting. Cycle 1 lost both FAIL rounds' full text; only
  the baton's verbatim findings survived. The log is primary — that applies to
  the package's own trail.
- Implementation phases run their own gates to green in the same context
  (amended 2026-07-05 — this is what WP1's A1/A2/A3 actually did, each
  first-run green; the old write-only-then-run split cost a session boundary
  and caught nothing). A first-invocation green run remains the upstream
  layers' receipt, not luck — if a phase does NOT go green first run, that is
  signal about the plan/validation layers and belongs in the NOW entry.
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
  - **A suite that reads git HEAD dynamically pins its ground truth to an
    explicitly analyzed specimen** (`:head-override` — analyze the blob, not
    the checkout) **or is re-run after every commit that moves HEAD.**
    Code-atom's `analyzer-gates` was green pre-commit and 7f/3e at committed
    HEAD — the package's OWN closing commit moved the blobs off the pinned
    specimen; only the retro's adversarial recheck caught it (fixed by
    pinning, `381c445`).
  - **A package that edits a file runs every pinned-enumeration scan over
    that file — grep the test tree for the file's path before calling the
    suite selection done.** Style-gate scans pin ANOTHER file's surface from
    a test namespace the editing package may never run: block-write added
    `ocr/read-unit` to `face_projection.clj` (legitimate, gate-reviewed) and
    went green on its own suites; framework's `g21-read-only-scan` in
    `face_arsenal_test.clj` pins that file's exact read surface and went
    stale-red, caught only at the NEXT package's close re-run (machine-cut,
    2026-07-13). The scan's enumeration is a shared surface; whoever moves
    the surface updates the scan in the same change.

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
- Escalation: record in `decisions.md` Open Questions — the conflict with
  verbatim citations, the options, a recommendation with grounds, and what
  re-runs under each option. Sid (or Fable on Sid's referral) rules; the
  ruling lands as current law under its axis, the open-question line is
  deleted (log regime 2026-07-12: current state only, git keeps the trail).
- **Ruling execution is a sweep, not a banner**: amend the introducing
  sentence AND grep every derived enumeration, count, and list across
  contract, spec, plan, and baton (gate lists, "all N gates", matrices).
  Cycle 1's banner-only spec amendment left "all ten contract gates" standing
  after gate 11 existed; the precedence rule absorbed it, but absorption is
  the backstop, not the plan.
- Re-entry triggers for Fable when Fable is OUT of the package: **gates
  green** (→ gate review) or **stop clause tripped** (→ ruling). Nothing
  else. When Fable is already the orchestrating session (the 2026-07-05
  default), there is no re-entry — it receives subagent phases, adjudicates
  inline, and gates at the end; the trigger list guards against opening a
  NEW Fable session for anything less.

## Gate review

- **Tier first (2026-07-27 cadence ruling):** small additive-and-dark
  MECHANISM slices get the SLIM gate — trap spot-checks in the diff, the
  FOCUSED suite re-run, the one contract-critical live receipt. The full
  protocol below is for cutover-class / durable-touch work or a flagged
  package. Either tier records verdict + residue the same way.
- Inputs: the green suite + the phase artifacts as prior-pass records — used
  as input, NOT authority. The code is what is judged; read it in full.
- Steps:
  1. Independently **re-run the suite this session** (never take Phase 7's
     word; SLIM tier: the focused suite).
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
- **Evidence-harness op-shape discipline**: any visual/evidence-capture
  harness asserts op-count parity FIRST; a blank capture is a HARNESS mapping
  bug until the op counts disagree (rect ops are flat, text ops are nested
  per-node — a shape mismatch renders nothing and masquerades as a product
  bug). (Framework W2-INT's two blank-PNG runs. Amendment D, signed
  2026-07-12.)
- **A strict-fail on a gate's one-line text is classified before it is
  enforced**: when the contract's authorizing sections (and phase prompts)
  pre-bless what the one-liner forbids, the gap is literal-vs-intent — a
  stop-clause escalation resolved by reconciling the short form to the
  authoring intent (option A), never by re-deriving a stricter bar no used
  form needs (D-001; the stricter path is deferred to a form-break, not
  rejected). The fresh-context gate is exactly where this gap surfaces.
  (Block-kernel F1/G12 — Sid ruled A, 2026-07-10.)
- **Without Fable**: same protocol, strongest available model, fresh session;
  verdict recorded in `decisions.md`; anything the reviewer cannot
  independently verify is an open doubt, never a pass.

## Closing a package

1. Commit decision is Sid's. Code and docs ALWAYS in separate commits; docs
   only on the local docs branch (never pushed, never merged to main).
2. **After the code commits land, re-run every suite that reads git HEAD
   dynamically** (and the receipt, if it reads HEAD). Green pre-commit is
   not green at committed HEAD when the package's own commits move HEAD —
   the code-atom close shipped a red driver suite that only the retro's
   adversarial recheck caught.
3. **Retro — at the BATCH boundary, not per package** (2026-07-27
   cadence ruling): per-package close ends at step 2 plus the gate
   record's residue note and the board prune. The batched retro (one per
   dark-lane activation / stratum milestone / ~3–4 packages) runs from
   the full trails (NOW logs + phase artifacts + source + gates):
   - QC-layer scorecard: what each layer caught, what it missed and the cost.
   - "What the next contract should do differently" — rules, each traceable to
     a concrete failure in THIS package (no speculative hardening dressed as a
     lesson).
   - Mechanisms that earned their keep.
   - Residue: tracked non-blocking items **including consumer/importer
     disciplines that must carry into the NEXT package's contract** (cycle 1
     initially dropped the importer-timestamp discipline; the recheck restored
     it).
4. **Adversarial recheck of the retro** before it feeds any skill or binding
   artifact: fresh context, verify every scorecard claim against the
   artifacts, the baton trail in git, and a fresh suite run. Cycle 1's recheck
   caught a residue line stale within minutes, an overcounted cost, and a
   wrong causal story — retros are written by the same process they judge.
   ONE recheck per batched retro (2026-07-27 ruling), never one per package.
5. Route the lessons: coding gotchas → `memory/implementation-quirks.md`;
   process rules → THIS skill (amend it); evaluation notes → `decisions.md`
   D-006; then PRUNE the package's board line to a one-line done-pointer and
   set the board's next-up (the thread file + git carry the trail — never
   leave a closed package's block or stale line on the board).

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
