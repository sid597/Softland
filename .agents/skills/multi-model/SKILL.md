---
name: multi-model
description: MANUAL-ONLY — never auto-invoke. Use ONLY when Sid explicitly types /multi-model or explicitly asks how to divide, check, or coordinate engineering work across multiple models or parallel sessions. Model-agnostic lens for independent checks, runnable gates, and low-friction splits, paired with dated trial logs — zero fixed role sheets.
---

# Multi-Model Collaboration Lens

**Scope guard:** manual-only. Load only when Sid types `/multi-model` or explicitly asks how to divide, check, or coordinate work across models or parallel sessions. For shaping exploratory prompts to get multiple views in chat, see [`prompt-maker`](../prompt-maker/SKILL.md).

## Core Principle

> **A claim is worth what it was checked against (a run, the record, or the tree) and how little the checker shared with its author (the other's answer, the brief, an unchecked page, or the model family).**

No model is "the diligent one" — diligence comes from the arrangement. Never freeze static job titles per model: versions, effort tiers, quotas, and existing checks change faster than this page. Keep the inputs and decide fresh each time:

- **When a run can judge the work:** model choice matters little. Pick by live quota and speed, and have a *different* session write the check.
- **When only a reading can judge it** (scope against Sid's decisions, stored record shape, product words, UI/visual feel): leans and family diversity (`Claude` / `Gemini` / `Codex`) matter. Run blind views and bring real disagreements to Sid.

---

## 1. Four Questions Before Splitting Anything

1. **Does this need a second context at all?** One session with a runnable judge is the base case. Split only when a single session would grade its own blind spot or hit a quota/context wall.
2. **What checks the work — and has the check itself failed red?** Checks have bugs too. A check counts once it has failed red on something known to be wrong (a live bug, a broken mutant, or a racy copy).
3. **How little does the checker share with the author?** Separate session, zero peek at the builder's plan or diff before writing the check, and a different model family when shared blind spots hurt. Check [`references/observations.md`](references/observations.md) and flag if this domain is still untested.
4. **What does this arrangement ask of Sid?** Count pastes, product calls, wait rounds, and scarce-quota burn — Sid's attention is the scarcest resource. Avoid splitting "write tests" and "write code" into separate human-launched waves when a self-contained build-to-gate can carry it.

---

## 2. Five Hard Traps (Prompt & Split Hygiene)

- **Keep the judge uncontaminated:** A reference oracle or breaker works from `SPEC.md` + Sid's calls alone — never show it the builder's `PLAN.md`, and never let a builder edit the test file that gates its own diff.
- **Verify disk and logs before attributing or building:** Check `git log`, file mtimes, and session records directly rather than trusting summaries. Open handoff/review pages with a header stating *who wrote it* and *what was run vs. only read*.
- **Merge by receipt, never by vote or smoothing:** Models rank themselves first and smooth tensions when summarizing. Keep only claims backed by `file:line`, a record quote, or a runnable probe; surface genuine disagreements for Sid.
- **Never bury unconfirmed calls in Sid's voice:** Pull any open product or contract choice to the top of your reply for Sid's yes before writing *"my call"* inside a paste-ready prompt.
- **Isolate parallel windows cleanly:** Give each window an explicit owned-path allowlist, check `git status -sb` before staging owned paths by name, and commit-and-stop rather than polling or sleeping for another window.

---

## 3. References & Upkeep

- **Dated trial log & untested domains:** [`references/observations.md`](references/observations.md)
- **Cross-harness dispatch & machine quirks:** [`references/harness-notes.md`](references/harness-notes.md)

**Upkeep (per `AGENTS.md`):** Update [`references/observations.md`](references/observations.md) **only on Sid's word**. File entries by **trial/arrangement** (`date · slice · arrangement · caught · got past · cost · receipt`), never under per-model trait profiles.
