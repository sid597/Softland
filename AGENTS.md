# Softland — Codex Boot (AGENTS.md)

Current ground, in precedence order: this bootstrap + `CLAUDE.md` →
`docs/decisions.md` (settled ground, binding) →
`docs/next-prompt.md` (the board) → the package's CONTRACT.md +
NOW.md. The 200-line historical body this file once carried is deleted
(2026-08-06, Sid's throughput ruling); git keeps it.

## Active Codex Bootstrap — BINDING

- At the start of a build session read `CLAUDE.md`, `docs/decisions.md`, and
  the package handoff (CONTRACT.md + NOW.md). The starter names every boot
  artifact with its byte size; over ~100KB is cut or semantically sharded
  before boot. Binding law is read PRIMARY; code is skeleton-first and then
  only the contract-pinned windows, never whole big files by default.
- The primary owns binding-law interpretation, synthesis, source authorship,
  causal rulings, and final artifacts. Delegate collection batches — multi-file
  scans, source census, logs, suites, environment state, transcript metrics —
  to fresh read-only `collector`/`explorer` subagents at the lowest competent
  model/effort. Give exact non-overlapping files/commands, no inherited history
  when supported, and a bounded evidence schema; raw output goes to scratch.
  Parallelism buys independent coverage only, and parent + children total cost
  is the economy measure. Gatherer output has no verdict authority.
- **Contract = the hard thinking; everything after is execution** (Sid,
  2026-08-06). Build the WHOLE atom straight through: build → surface bugs
  → fix them before source freeze. Keep one implementation adversarial check
  on newly introduced claims and the real seam. A genuine fork (two readings
  that cannot both hold) is ONE question in the thread file: note it, route
  around it, keep building what is unblocked. Never halt on a stop code.
- A plan is a checklist, never authority. Every item names SOURCE (contract,
  direct request, or observed in-scope defect) and DONE WHEN. An unsourced
  device, matrix, artifact, or tooling road becomes one debt line, not work.
- Work in coherent batches: collect independent reads once, patch a namespace
  or compile milestone together, and run the focused check once per batch.
  Three repeated patch→same-check microloops trigger one complete diagnostic
  collection and one regrouped repair batch, not another conversational tail.
- Whole-atom custody may cross one meaningful context boundary at candidate
  source freeze; that is a self-handoff, not a phase ladder, gate, or Sid
  touch. Run already-specified close receipts in a fresh `receipt_runner`
  context when the primary is deep. Do not invent missing harnesses mid-pass:
  optional evidence is debt; required missing evidence means RECEIPT PENDING.
- At atom close: freeze the contract's 3–5 scenarios as tripwires + 2–3
  representative goldens (no matrices) · run the focused suite · record
  foreign failures as debt · derive changed files from `git diff --name-only`.
  The ≤15-line NOW close receipt and board flip are the final planned close
  mutation.
  After NOW, do not change source/tests/tooling; append one correction marking
  REPAIR/RECEIPT PENDING if a later finding invalidates the close. Acceptance
  is Sid's word; second-model verification does not exist.
- On a stop/side-quest/status interruption, stop active work before answering;
  make no cleanup edit or new test. Report only ACTIVE WORK · LAST
  DECISION-CHANGING FINDING · CURRENT MUTATION/PROCESS STATE, then wait.
- Operating corners (decisions.md "The corners"): two Sid-touches per atom
  — cut and accept; ambiguity takes the strongest default plus a note,
  never a queued question. Parallelism buys coverage, never confidence —
  two lanes never share one question.
- Dead ceremony — never reintroduce it, and never imitate it from old
  contracts (IMAGE-ATOM, SEAM-STEP1 predate the law): validation
  ladders/rounds, default-fail verdicts, recut ledgers, ruling packets,
  gate matrices, stop codes (S1/S2), conjunctive [JVM-FULL] gates,
  allowlist partitions, phase artifacts beyond the thread file. Packages
  opened before 2026-08-06 finish under their opened process.
- For any unexplained live failure, regression, performance problem, or
  disputed causal claim, invoke
  `.agents/skills/investigation-fence/SKILL.md` before attribution,
  architecture rulings, or a correction contract. Reading may establish
  structure; magnitude and causation require a receipt. Escalate modality,
  never model prestige; model identity is zero evidence. A refutation meets
  the same taken-path standard as the claim it would kill. Executing an
  already-specified close receipt is not an investigation unless it produces
  an unexplained result and the session intends to attribute cause.
  Investigation is causal attribution after unexplained behavior, not either
  adversarial check.
- Package mechanics: `.agents/skills/work-package/SKILL.md` (bridge to the
  canonical one-pass law in `.claude/skills/work-package/SKILL.md`).

## Hard rules

- NEVER read `src/app/server/env.clj` — API keys; reference as symbols only.
- Commits only when the contract/handoff says so: code and docs in SEPARATE
  commits, both on `docs/current-mental-model-local`; never push, never
  merge, never a Co-Authored-By line.
- New code goes in its own namespace; big files (`renderer.cljs`,
  `ground.cljs`, `electric_flow.cljc`) get thin hooks only.
