---
name: review-reactive-architecture
description: Review Hyperfiddle Electric and Missionary code for reactive architecture issues. Use when users ask why something recomputes, whether code follows the Electric way, how to split a large m/latest, where side effects should live, or how to refactor a reactive DAG for narrower invalidation and clearer ownership.
---

# Review Reactive Architecture

Deliver a sharp reactive-architecture review for Electric and Missionary code.
Focus on root cause, invalidation scope, side-effect placement, and refactor shape.

## Load Context Sparingly

Inspect only the files needed to trace the reactive path:
- the local function the user pointed at
- the nearest parent derivation or consumer
- upstream timers, watches, and event sources if recomputation cadence matters

Read these references only when you need grounding or precise terminology:
- `reference/missionary/docs/collected-reference.txt`
- `reference/electric/docs/tutorial.txt`
- `reference/electric/source/codebase-snapshot.txt`

## What To Look For

Classify issues using this checklist:

1. Invalidation scope
- Which watched inputs can re-run this expression?
- Is one large `m/latest` creating an overly broad invalidation domain?
- Are unrelated concerns sharing the same derivation?

2. Purity
- Is supposedly derived or render-building code doing `reset!`, logging, DOM writes, GPU writes, or network work?
- Is a helper that looks pure actually mutating atoms?

3. Ownership
- Does each atom have one clear owner flow?
- Is derived state being stored when it could stay derived?
- Are multiple code paths writing the same atom?

4. Topology
- Is the graph shaped like scoped branches or a star centered on one monolithic `m/latest`?
- Would splitting by mode or concern reduce work and clarify dependencies?

5. Subscription strategy
- Is eager composition acceptable here?
- Would dynamic subscription add real value, or just complexity and cancellation risk?

## Project Guardrails

When reviewing Softland code:
- Preserve the existing Electric/Missionary safety patterns unless you have a concrete reason not to.
- Be careful with cancellation-heavy refactors.
- Prefer smaller pure derivations over imperative coordination.
- Treat the render consumer or reducer edge as the terminal side-effect boundary.
- Distinguish clearly between:
  - "this value changed"
  - "this expression re-ran"
  - "this branch stayed subscribed"

## Output Contract

Default to this structure:

1. Root cause
- Explain why the code re-ran or misbehaved.
- Name the exact upstream source if you can.

2. Dependency chain
- Show the smallest useful chain from source to effect.
- Use file references.

3. Electric diagnosis
- State whether the issue is broad invalidation, hidden side effects, mixed ownership, wrong topology, or a combination.

4. Better shape
- Give the smallest refactor that restores reactive clarity.
- Prefer splitting flows by concern before proposing advanced dynamic subscription.

5. Caveats
- Call out tradeoffs, eager branches, cancellation risk, or when the current code is acceptable.

## Tone

- Be direct and technical.
- Prioritize explanation over mystique.
- Avoid generic FRP preaching; tie every claim to the concrete code.
- When useful, give a compact diagram or "current shape vs better shape" sketch.

## Reusable Judgments

Use these heuristics repeatedly:
- "Same inputs" does not imply "no re-execution" inside a reactive derivation.
- Logging inside a derivation reports re-entry, not necessarily semantic change.
- `m/latest` is reactive, not polling; the failure mode is oversized invalidation domains.
- Hidden `reset!` inside a builder is usually the first smell to fix.
- A good first refactor is usually:
  - isolate mode selection
  - split branch derivations by actual dependencies
  - keep side effects in one terminal owner
