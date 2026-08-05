# Fix-Session Prompts — one file = one session

Each `SESSION-N-*.md` is a self-contained work packet. To run one, start a FRESH session and say:

> Read docs/retros/rama/fix-prompts/SESSION-N-<name>.md and execute it: plan the work first, then do it.

Order and dependencies:

| Session | Scope | Depends on |
|---|---|---|
| 0 | Foundations: kernel.clj loads, shared guarded-fold + obs-auth helpers in core.clj, probe harness | — |
| 1 | Compute (the template fix) | 0 |
| 2 | LLM | 0 (read 1's diff if done — it is the pattern) |
| 3 | Space | 0 |
| 4 | Kernel-contract remainder | 0 |
| 5 | Transcript (must respect June object-container layer) | 0 |

Every session follows the same protocol (embedded in each prompt): plan mode first → load `/rama` + `rama-retro` skills → `require` everything you touch → write the FAILING adversarial probes before fixing → fix until probes pass → re-run Phase 4 validation on the changed module → report verdicts + update this folder's status line in the session prompt file.

If a session runs out of context mid-batch: split the batch, record exactly where you stopped in `docs/sessions/next-prompt.md`, and leave the SESSION file's status line updated.
