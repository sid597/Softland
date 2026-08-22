---
name: feedback-agent-wave-cost-visibility
description: "Before launching multi-agent waves, state agent count + token estimate and get Sid's go-ahead; offer a manual runbook option"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 196e5329-9e82-4b1e-bea1-08e5b57c842e
---

During the Rama retro (2026-06-11), Claude launched a 13-agent parallel wave (~1.5M tokens) without surfacing the cost first. Sid interrupted, worried about token spend, and asked which steps were directly attached to the task vs safeguard loops, and whether he could run phases manually.

**Why:** Sid wants throughput AND cost visibility. Subagent waves are opaque from the outside — he can't see what each agent costs or which are essential until after the spend. He also values being able to take over execution manually (prompt files + runbook), pacing the spend himself.

**How to apply:**
- Before any wave of >2 subagents: one line with agent count, what each does, and a token estimate; distinguish defect-finding/core steps from reference/safeguard steps; let Sid pick depth (full / core-only / minimal).
- Always offer the manual option: write self-contained phase prompts to files + a RUNBOOK.md so Sid can run each step in a fresh session (`claude -p "$(cat prompt.md)"`).
- Related: [[independent-design-cut]], the /rama retro method in `docs/retros/rama/README.md`.
