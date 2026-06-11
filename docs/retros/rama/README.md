# Rama Retrospective — Skill-Driven Review of Pre-Skill Modules

> **Start at `UNIFIED-RETRO.md`** — it merges this retro with the earlier runtime-probed retro (`docs/current-mental-model/build/rama-retro-review/`) into one verdict, one weakness taxonomy, and one master fix queue with a fix-session handoff prompt. The reusable method lives in the `rama-retro` skill (`.claude/skills/rama-retro/SKILL.md`). This README documents how THIS retro was run.

**What this is.** Five Rama feature tracks were implemented (May 2026 and earlier) before the official `/rama` skill was available. This retro runs the skill's own phased process against the as-built code to find where things were done the wrong way. The June 2026 object-container / transcript-ingest work was built WITH the skill and is not a review target (it serves as read-only witness for the kernel-contract track).

**Independence rule.** This retro is Claude's, formed without reading `codex_implementation/`, `.agents/`, or any Codex retro artifacts. All pipeline agents inherit that exclusion, plus the standing ban on `src/app/server/env.clj`.

## Method

The pre-skill modules are missing their Phase 0–2 artifacts (no IMPLICIT_SPEC.md or PLAN.md ever existed), and the skill's Phases 4/6 are already written as adversarial validations of someone else's code. So the retro = reconstruct the missing artifacts blind, run the official validations verbatim, consolidate:

| Step | Skill phase doc | Mode | Artifact |
|---|---|---|---|
| R0 | `phase-0-implicit-spec.md` verbatim | code-blind (design docs + identity header + public API + tests-as-contract; NOT topology internals) | `IMPLICIT_SPEC.md` |
| R1 | `phase-1-plan.md` verbatim | code-blind; input is `IMPLICIT_SPEC.md` ONLY | `PLAN.md` |
| R2 | `phase-2-plan-validate.md` verbatim | fail → re-run R1 | `PLAN_VALIDATION.md` |
| R3 | retro-specific | diff as-built vs PLAN.md; classify: `as-built-defect` / `equivalent` / `as-built-better` / `plan-defect` | `DESIGN_DIFF.md` |
| R4 | `phase-4-impl-validate.md` verbatim | adversarial, full code-tracing; verdict recorded as-is | `IMPLEMENTATION_VALIDATION.md` |
| R6 | `phase-6-test-validate.md` verbatim | existing tests vs `IMPLICIT_SPEC.md` | `TEST_VALIDATION.md` |
| R7 | retro-specific | merge R3+R4+R6 (+ `DIRECT_REVIEW.md` where present); dedupe; severity-rank; provenance-tag (`skill-process` / `direct-review` / `both`) | `FINDINGS.md` + `FIX_PLAN.md` |

Phases 3/5/7 (implement / tests / pass) are fix-session work, teed up by `FIX_PLAN.md`. After the retro each track folder looks like a standard skill impl-root with failing validation artifacts — a fix session resumes the standard process at Phase 3 in-place.

**Execution model.** One fresh-context subagent per step (the skill's anchoring-isolation law), orchestrated from a main session. All agents on the session model, no tier downgrades.

**Deliberate retro adaptations** (deviations from the build-mode phase docs, with reasons):

1. **R1 gets `IMPLICIT_SPEC.md` only** — not the original design docs. The May design docs contain design decisions (depot/PState names, topology shapes); feeding them to R1 would contaminate the blind re-derivation. R0 distills them to requirements first.
2. **R0 must distill requirements, not carry design through.** Protocol-level requirements (e.g., "a claim must be granted before spawn") are spec; storage-level choices (PState schemas, partitioning) are design and must not appear in the spec artifact.
3. **R4 validates against `IMPLICIT_SPEC.md` + `PLAN.md`** — not the prose design docs, for the same contamination reason (the docs enumerate known failure modes; R4's blind verdict is part of the method calibration).
4. **R4/R6 verdicts are recorded, not looped** — in build mode a fail re-invokes implementation; here the fails ARE the retro result.

## Tracks

| # | Track | Code (at HEAD) | Spec sources for R0 | Status |
|---|---|---|---|---|
| 1 | compute | `src/app/server/rama/dogfood/compute.clj` + `test/.../dogfood_compute_test.clj` | `docs/current-mental-model/architecture/dogfood-runtime/compute-track.md`, `slice-a-compute-run-command.md` | **COMPLETE** — R2 r1 fail → r2 pass; R4 major-fail; R6 major-fail; 21 code + 7 test findings (4 HIGH); calibration gate MET (see `01-compute/FINDINGS.md` §Calibration) |
| 2 | llm | `dogfood/llm.clj` + test | `llm-track-canonical.md`, `llm-track-v2.md`, `llm-track-derived-contract.md`, `llm-track-slice-roadmap.md` | **COMPLETE** (defect core: R0+R1+R4; R4 major-fail; FINDINGS.md consolidated inline) |
| 3 | space | `dogfood/space.clj` + test | `three-depot-current-system.md`, space identity header, `rama-world-kernel-*` docs | **COMPLETE** (defect core: R0+R1+R4; R4 major-fail; FINDINGS.md) |
| 4 | kernel-contract | `kernel.clj`, `text_kernel.clj`, `core.clj`, `util_fns.cljc` | KERNEL-SHAPE data form, `rama-world-kernel-v0-pr-trail.md` | **COMPLETE** (defect core: R0+R1+R4; R4 major-fail; FINDINGS.md) |
| 5 | transcript-remnant | `dogfood/transcript.clj`, May-era lines only (git-blame-scoped; 589 of 1135 lines) | `transcript-capture.md`; abbreviated pipeline (R0, R4, R6) | **COMPLETE** (R4 + R6 major-fail; FINDINGS.md) |

**Scope note (cost decision, 2026-06-11):** tracks 2–4 ran the defect core only — R2/R3/R6/R7 were deliberately skipped after Track 1 calibrated the method; their PLAN.md files exist but are UNVALIDATED (no PLAN_VALIDATION). Fix sessions should treat those plans as strong references, not gospel.

**Cross-retro comparison:** `COMPARISON-prior-retro.md` — this retro vs `docs/current-mental-model/build/rama-retro-review/` (the earlier runtime-probed retro). Net: complementary; ours found ~3–4× more classes (notably the cross-partitioner partial-commit family); theirs runtime-proved six findings ours missed or mis-traced (incl. `kernel.clj` does not load, and the compute unknown-run NPE that corrected our C-05). Future retro/fix validation must include a probe phase + `require` every reviewed namespace.

**Pilot-first calibration.** Track 1 runs end-to-end first. `01-compute/DIRECT_REVIEW.md` (Claude direct falsification review, done before this pipeline existed) is the benchmark: R7's provenance tags show what the skill pipeline catches vs. misses vs. adds. The method is trusted for tracks 2–5 only if R4 independently finds the F1–F3 class (retry × state reset, cross-partitioner partial writes, stuck states).

**Input isolation per step:** R0–R2 must not read module topology code or any findings. R3/R4/R6 read code but not each other's outputs and not `DIRECT_REVIEW.md`. R7 reads everything.
