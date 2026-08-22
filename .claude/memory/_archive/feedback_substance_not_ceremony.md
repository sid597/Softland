---
name: substance-not-ceremony
description: "When packaging work for the user or Codex, deliver the substance we negotiated. Don't import formal handoff templates (Mode/Altitude/Authority, Preserve, STOP, etc.) onto small mechanical changes."
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 0cc5ee85-9efa-4ff0-8470-7e010e1051d5
---

**Rule:** When the user asks me to package a decision or hand work to Codex, deliver exactly what we discussed — no more. Do not import the `ask-codex-for-feedback` template structure (MODE / ALTITUDE / AUTHORITY / PRESERVE / DO NOT REOPEN / STOP CONDITIONS / verification gates / commit message templates / PR description templates) onto small mechanical changes.

**Why:** This has now happened multiple times in one session:
1. Wrote a 290-line "kernel.clj as a spec with 3-PR ladder" when the user wanted a guiding doc.
2. Wrote a 250-line `pr-1-rename.md` with step-by-step decomposition when the user said "i will ask codex to break down what to do when."
3. Wrote another 250-line formal brief with Mode/Authority/Preserve/STOP sections when the user said "just give the feedback to codex based on what we discussed only that."

The pattern: I import formal-handoff structure even when the negotiated decisions are small and the user has been explicit about wanting brevity.

**Why the user keeps pushing back:** the ceremony obscures the substance. Sid (and Codex) can read a rename map + a file-split spec + a collision note + an out-of-scope list — that's the entire content. Wrapping it in 7 formal sections doesn't add information; it adds friction and signals I'm pre-cooking work that the executor should plan.

**How to apply:**

When asked to package work for Codex (or for the user to review before handoff):

- DEFAULT: deliver as terse markdown or text — the actual decisions, in code blocks where applicable, with minimal headers.
- DON'T import: MODE / ALTITUDE / AUTHORITY / PRESERVE / DO NOT REOPEN / STOP CONDITIONS / verification-gate sections / commit-message templates / PR-description templates.
- DON'T enumerate "Step 1, Step 2, …" — Codex decomposes.
- DON'T frame as "PR 1 of N" — Sid works commit-by-commit.
- DO include: the renames / decisions / locked choices / known risks (1-line each) / out-of-scope (1-line each) / pointer to source of truth.

The litmus test: if I removed all section headers, would the content shrink by less than 30%? If yes, the headers are doing work. If no, they're ceremony — strip them.

**Specific to the `ask-codex-for-feedback` skill template:** that template is for GATE / REVIEW handoffs where Codex's job is judgment. For IMPLEMENTATION handoffs where Codex's job is execution of decisions we already made, most of that template is overkill. Use the template only when the substance genuinely needs the framing — not by default.
