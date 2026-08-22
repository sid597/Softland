---
name: deep-research-workflow-limits
description: "The multi-agent deep-research Workflow is the RIGHT tool for broad+deep research when usage budget is ample — it failed 3x only on usage/power limits, then succeeded brilliantly on 20x usage. Lean main-loop research is the cheap fallback. Plus enabling fixes and assembly gotchas."
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 1997f19a-c510-444a-b26a-083dc4413eeb
  modified: 2026-08-05T20:32:06.397Z
---

The large multi-agent deep-research **Workflow** (fan-out ~40 subagents, each doing many WebFetches) for the giants/HCI design research (2026-06-09 → 06-10) failed 3 times **on usage/power limits, NOT on the method**:
1. Power cut killed the background run (resume journal logs only `started`, so resume gives no cache hits).
2. Account **monthly spend limit** hit mid-run (~2M tokens, empty `md:null`).
3. **Session limit** hit (~912k tokens, empty result).

Then on a **20x-usage plan the SAME workflow succeeded** — 40 agents, ~2.25M tokens, ~25 min — producing excellent, adversarially fact-checked sections (synth agents even respected the fact-check verdicts, dropping confabulated claims and flagging uncertain attributions). Output is COMPLETE at `OLD DOCS/design/claude/giants-research-deep.md` (10 threads + 4 supplementary nodes + Part III cross-thread synthesis).

**How to apply:**
- The workflow IS the right tool for broad+deep research **when usage budget is ample** — don't reflexively avoid it; the quality when it ran was high. On a **tight budget, prefer lean main-loop research** (Claude itself: a few WebSearch+WebFetch per thread, write each section to disk as you go) — cheap and reliable; it produced threads 1, 4 and all 4 supplementary nodes here.
- **Enabling fix (critical):** background subagents have no human to approve permission prompts, so a single un-allowed WebFetch domain stalls an agent ~18 min (6×180s) then fails. Blanket `WebFetch` is now in `.claude/settings.local.json` allow so subagents run unattended. Keep it.
- **Verify-before-trust:** arm a background health-check (grep the run dir for a success signal like a `StructuredOutput` emission vs limit/stall errors) so a bad run is caught in ~2 min, not 30+.
- **Gotcha — args:** passing `args` via `Workflow({scriptPath, args})` did NOT populate the script's `args` global (batch filter ignored, ran all threads). Hardcode batches in the script (`const BATCH=[...]`) instead.
- **Gotcha — assembly:** the run's output file is one pretty-printed JSON object; sections live at `.result.sections[]` (not top-level); synth agents prepend a chatty preamble before the real `## ` header (strip everything before the first `^## `). Assemble with file surgery (jq + awk + cat), not by loading 180KB into context.

Related: [[MEMORY]].
