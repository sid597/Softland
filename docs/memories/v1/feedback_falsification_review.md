---
name: Falsification review protocol
description: When reviewing code (especially from other LLMs), do an adversarial falsification pass — don't just validate architecture. Trace state lifecycles, consumers, error paths, ordering, and shape. Session 41 exposed that Claude catches systemic flow issues but misses edge-case correctness bugs that Codex finds.
type: feedback
---

Review for falsification, not coherence. The architectural pass catches systemic issues (reactive flow coupling, ownership model). The adversarial pass catches correctness bugs (stuck state, dead fields, race conditions, error leaks).

**Why:** In S41, Claude reviewed Gemini's sidebar 3-layer migration. Claude caught the hover→text churn perf bug (architectural). Codex caught 5 correctness bugs Claude missed: nil overlay sentinel masking truth, selected-file field computed but never consumed, file fetch race condition, in-flight flags stuck on error, wrong tree traversal in metrics. Every miss maps to a skipped adversarial check.

**How to apply:** The full protocol is now in CLAUDE.md under "Code Review Protocol — Falsification Pass." The core habit: never say DONE without naming a failure mode and explaining why it can't happen. Never approve optimistic state without tracing write → render → reconciliation → clear.

**Root cause of the miss:** Claude defaults to validating intent ("does this code mean the right thing?") rather than tracing behavior ("what does this code do when truth is nil? when the network fails? when responses arrive out of order?"). The fix is structural — make the adversarial pass mandatory in output format, not optional.
