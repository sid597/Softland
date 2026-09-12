---
name: rama-retro
description: How to retrospectively review already-committed Rama code (modules, topologies, PStates, executors) that was built without the /rama skill or whose production-safety is unproven. Use when the user asks to retro/review/audit existing Rama work, find encoded wrong patterns, or validate committed slices after the fact. The /rama skill builds new work; THIS skill reviews as-built work. Synthesized from two real retros (2026-06): the static falsification pipeline (history/old-docs/retros/rama/) and the runtime-probed review (history/docs/rama-retro-review/).
---

# Rama Retro — reviewing as-built Rama work

## Why this exists

Two independent retros of the same five pre-skill modules proved that neither method alone is enough:

- **Static falsification pipeline** found ~3–4× more finding classes (especially cross-partitioner partial commits and retry-reset families) — but confidently mis-traced two mechanisms that one execution disproved.
- **Runtime probing** found six truths static reading missed entirely: a namespace that does not load, a fatal NPE poison record (eager let-binding before guards), observations folding with no authorization, semantic leaks, conflict-blind idempotency aliasing, UTF-8 byte corruption.

**Rule one of any retro: combine them.** Static tracing proposes; execution disposes.

## The process (per module/track)

| Step | What | Mode |
|---|---|---|
| R-1 **Scope by era/provenance** | `git log`/`git blame` to find what was built without the process under review. Review code at HEAD; use history only to scope and era-tag (mixed files: blame-scope line ranges). | orchestrator |
| R0 **Implicit spec** | Run `/rama` `phase-0-implicit-spec.md` verbatim. Inputs: design docs + identity header + tests-as-contract. **Code-blind.** Distill REQUIREMENTS; never carry design choices (PState/depot names, topology types) into the spec. | fresh agent |
| R1 **Blind plan** | `phase-1-plan.md` verbatim from IMPLICIT_SPEC.md ONLY (withhold original design docs — they contaminate). Produces the reference design fixes build against. | fresh agent |
| R2 **Plan validation** (optional tier) | `phase-2-plan-validate.md` verbatim; loop R1 on fail. Each round-1 fail is usually ALSO an as-built gap — check the amendments against the code. | fresh agent |
| R3 **Design diff** (optional tier) | Diff as-built vs PLAN per dimension; classify each divergence: `as-built-defect` / `equivalent` / `as-built-better` / `plan-defect`. | fresh agent |
| R4 **Implementation validation** | `phase-4-impl-validate.md` verbatim against spec (+plan if it exists). Adversarial, default major-fail, line-cited runtime traces. **This is the defect core — never skip.** | fresh agent |
| R5 **Runtime probes** | See probe list below. `require` every reviewed namespace FIRST. Convert each HIGH static finding into an IPC probe; run the depot-adversary matrix. Record probe outputs verbatim in the artifact. | fresh agent w/ Bash |
| R6 **Test validation** (optional tier) | `phase-6-test-validate.md` verbatim against the spec. | fresh agent |
| R7 **Consolidation** | Merge all streams; dedupe by ROOT CAUSE (one cause, many symptoms = one finding); severity-rank; provenance-tag (`static` / `probe` / `both`); reconcile severity disagreements explicitly (separate defect-now from spec-sanctioned deferred scope). Outputs FINDINGS.md + FIX_PLAN.md. | agent or inline |

**Artifact layout** (per track, so a fix session resumes standard `/rama` Phase 3 in-place):
`IMPLICIT_SPEC.md, PLAN.md, [PLAN_VALIDATION, DESIGN_DIFF, TEST_VALIDATION], IMPLEMENTATION_VALIDATION.md, PROBES.md, FINDINGS.md, FIX_PLAN.md` + a top-level README with method, status table, scope notes.

## Depth tiers (pick BEFORE starting; announce cost)

- **Full pipeline** (~1M tokens/track at 2026 rates): R0–R7. Use for the FIRST track only — it calibrates the method and produces a validated reference plan.
- **Defect core** (~400–500k/track): R0 + R1 + R4 + R5 + inline R7. The default for remaining tracks.
- **Minimal** (~250k/track): R0-lite + R4 + R5. When the question is only "what's broken," not "what should it have been."

Cost discipline (learned the hard way): before any wave of >2 agents, state agent count + token estimate (range it — big modules ran 1.5× small-module estimates); offer the manual runbook (write each phase prompt to a file; user runs `Codex -p "$(cat prompt.md)"` per phase at their own pace).

## Orchestration modes — agents are NOT the point

Evidence from the two retros: the probe retro ran **one single-context session per block (~10 min/block, six blocks in an afternoon)** and found the highest-severity truths; the agent pipeline spent ~450k–1M tokens/track and found 3–4× more classes. What actually produced findings was (1) fresh context per review unit, (2) the adversarial checklist applied verbatim with default-fail, (3) runtime probes, (4) the blind-plan contrast object. Only #4 *requires* sub-block context isolation; #1 is satisfied equally by "new session per block."

- **Mode A — single session per block (DEFAULT).** One context reads the block brief, reconstructs the implied phase artifacts, runs the full check matrix + R5 probes inline, writes the review. Cheap, user-steerable, probes are natural (Bash in hand), and the reviewer accumulates the cross-block taxonomy as they go. Driver: a human-started session per block, or one agent per block if fan-out speed matters.
- **Mode B — phase-split agent pipeline.** Use ONLY when you want the blind re-derivation (R0/R1 code-blind → DESIGN_DIFF) — a validated reference plan for fix sessions and design-level "wrong way" answers — or a sealed calibration experiment. This is the only goal that cannot be met in one context (a session that has seen the code cannot write a blind plan).
- **Hybrid (best value for a multi-block retro):** Mode A per block for defects + probes, plus ONE Mode-B blind-plan agent per block you intend to actually re-design. Skip R2/R3/R6 unless the plan will be built against.

## Execution model

- One fresh-context agent per step (the skill's own anti-anchoring law). Prompt = thin retro preamble + exact input paths + hard exclusions + "follow your phase doc exactly" + short return format + verdict line.
- Isolation: R0–R2 never read module code or any findings. R3/R4/R6 never read each other's outputs or any benchmark review. R7 reads everything.
- **Calibration trick:** if an independent review of the same code exists (or do a quick direct falsification pass first), keep it sealed from the pipeline and have R7 provenance-tag against it. found-by-both = highest confidence; found-by-one-only = method gap to record.
- **Interrupted agents leave half-written artifacts** (e.g. a copied-but-unfilled template). Before trusting/committing any artifact: check it is filled and ends with its verdict line.

## R5 probe list (the runtime half — never skip)

Step zero: `(require 'every.reviewed.namespace)` — a file that does not load invalidates every static claim about it.
Then, against an IPC instance, per depot (the **depot adversary matrix** — every event × every entity state → expected accepted/ignored/rejected):

- observation/control **before** its request exists (unknown ids)
- observation with **no** token, wrong token, stale executor identity
- duplicate id + same payload (must replay/no-op) and duplicate id + **different payload** (must conflict-reject, never reset)
- any write **after terminal state** (must not regress; sticky terminals)
- partial-retry across every partitioner hop (kill between hops where harness allows; else trace + targeted probe)
- non-ASCII/multi-byte input wherever identity uses byte offsets; malformed input carrying secrets (redaction proof)
- one collection pushed past its claimed bound

Semantic assertion rule for any probe/test: assert the **payload the spec promised**, not row existence.

## What to look for (the recurring wrong patterns — all confirmed across 5+ modules)

1. **Unguarded `termval` over lifecycle rows in at-least-once streams** — request replay/duplicate resets live/terminal state → duplicate physical side effects (spawns, model runs). The single most repeated defect.
2. **Invariants spanning partitioner hops** — transaction scope = between partitioners; grant/inbox pairs, idempotency check↔write splits, multi-hop commits partially apply and the retry guard skips the second half forever.
3. **Observations/controls trusted too easily** — folding without claim/token/state/existence checks; controls inventing missing targets.
4. **Named invariant ≠ proven invariant** — docs state the rule, validation accepts weaker evidence. Every named invariant needs: an enforcing mechanism, an unbypassable check, an adversarial trace, and a test that fails if it's false.
5. **`{String Object}` schemas + unbounded non-subindexed collections + whole-row write amplification** on the hottest path.
6. **Compatibility/transitional paths becoming permanent truth** (broad compat events, in-memory atom mirrors as UI truth with no rebuild path).
7. **Green tests proving the spine, not the contract** (no rejection-path, duplicate, post-terminal, restart coverage; 1 IPC launch is usually enough for disjoint-key tests).

Full taxonomy + the 17-question done-gate: `history/docs/rama-retro-review/META_LEARNINGS.md`. Worked examples: `history/old-docs/retros/rama/` (pipeline + comparison) and `history/docs/rama-retro-review/` (probes).

## Handoff (what the retro must end with)

Per track: FINDINGS.md (root-cause-deduped, severity-ranked, provenance-tagged, file:line cited, scenario-traced) + FIX_PLAN.md (ordered batches: correctness → robustness → perf/hygiene → tests; each batch names findings, sketch, and the IPC probes that verify it). Cross-track: a unified fix queue naming the order and the shared-abstraction fixes (e.g. one guarded-fold helper instead of N module-local patches). Fix sessions resume the standard `/rama` process at Phase 3 with these artifacts in-place and MUST re-run R4 + R5 after changes.
