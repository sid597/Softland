---
name: feedback-subagent-model-policy
description: "When spawning subagents, set model to opus (Opus 4.8); the orchestrator context stays Fable"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 1843c9d7-b1a4-433d-b486-2b601388be7f
---

When a session running on Fable spawns subagents (Agent tool or Workflow), the subagents should run on **Opus 4.8** (`model: "opus"`), not inherit Fable. Sid stated this during the Rama Session-3 fix planning (2026-06-11): "the main orchestrator that is you should be fable xhigh and the subagents should use opus-4.8 max instead of fable".

**Why:** Fable is reserved for the orchestrating/judging context; fan-out work should use Opus 4.8 at max effort instead of consuming Fable on subagent tasks.

**How to apply:** Pass `model: "opus"` explicitly on every Agent/Workflow `agent()` call. Keep orchestration, synthesis, and final judgment in the main (Fable) context.
