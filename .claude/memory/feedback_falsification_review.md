---
name: falsification-review-protocol
description: "When reviewing code (especially from other LLMs), do an adversarial falsification pass — don't just validate architecture. Trace state lifecycles, consumers, error paths, ordering, and shape. Session 41 exposed that Claude catches systemic flow issues but misses edge-case correctness bugs that Codex finds."
metadata: 
  node_type: memory
  type: feedback
  originSessionId: dd1c5180-bc7f-4596-a1e4-ef8946ec5936
---

Review for falsification, not coherence. The architectural pass catches systemic issues (reactive flow coupling, ownership model). The adversarial pass catches correctness bugs (stuck state, dead fields, race conditions, error leaks).

**Why:** In S41, Claude reviewed Gemini's sidebar 3-layer migration. Claude caught the hover→text churn perf bug (architectural). Codex caught 5 correctness bugs Claude missed: nil overlay sentinel masking truth, selected-file field computed but never consumed, file fetch race condition, in-flight flags stuck on error, wrong tree traversal in metrics. Every miss maps to a skipped adversarial check.

**How to apply:** CLAUDE.md keeps the golden rule + the staff-product-architect angle (2026-07-16 curation); THIS file is now the home of the operational checklist. The core habit: never say DONE without naming a failure mode and explaining why it can't happen.

**The checklist — for every changed state field or async flow:**
1. **Ownership** — who writes, who reads, who clears? More than one writer: who wins?
2. **Lifecycle** — what exact value clears it? Does nil/empty/false clear correctly? Can it stick and mask future truth?
3. **Consumer** — where is it actually consumed? Connected to behavior, or computed and discarded?
4. **Error path** — on failure, what cleanup runs? In-flight flags, locks, pending sets, caches always released?
5. **Ordering** — A starts, B starts, A finishes after B: what happens? Can an older callback overwrite newer state?
6. **Shape** — execute every tree/path expression against the actual structure; never trust intent.
7. **Done gate** — name one plausible failure mode and why it cannot happen; without that, the step is not DONE.

Review output includes: Architecture · Failure modes attempted (specific scenarios) · Writers/readers/clearers per changed state · Async ordering risks · Error-path cleanup · Open doubts. Applies to DESIGN PROPOSALS too, not just code ([[meta-failure-generators]] — the journal fiasco shipped three doc revisions no falsification pass ever touched).

**Root cause of the miss:** Claude defaults to validating intent ("does this code mean the right thing?") rather than tracing behavior ("what does this code do when truth is nil? when the network fails? when responses arrive out of order?"). The fix is structural — make the adversarial pass mandatory in output format, not optional.

**Extends to docs/contracts (2026-07-15, first-light A/B catch — Codex's):** falsify GATES against schedulability. Any gate requiring an event that cannot be scheduled (recurrence, external adoption, a second organic friction) will pressure fabricating that event — split the package instead (prove metabolism now; inheritance opens when the event genuinely occurs). And before committing a DIRECTION, drop altitude once: run its slice against the repo's actual seams (the first-light-vs-P3c "ordering question" dissolved the moment someone read P3c's contents). Run my own laws against my own slices before Codex does.
