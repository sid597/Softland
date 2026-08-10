---
name: reactive-master
description: Expert guidance on Hyperfiddle Electric and Missionary reactive programming patterns. Activates when users need help with flows, signals, backpressure, or refactoring to "The Electric Way".
---

# Reactive Master

Use this skill for implementation and refactoring work in Hyperfiddle Electric
and Missionary. Prefer `review-reactive-architecture` when the user only asks
for an audit or explanation.

## Load Context Sparingly

Read only the references needed for the immediate problem:
- `docs/reference/missionary-reference.txt` for Missionary operators,
  cancellation, backpressure, and task/flow semantics.
- `docs/reference/electric-tutorial.txt` for Electric syntax and placement rules.
- `docs/reference/electric-codebase.txt` when behavior depends on Electric
  internals or an error points into the library.

## Core Model

- Sources are watches, event streams, timers, network responses, and explicit
  user actions entering the graph.
- Derived flows compute values from sources and should stay pure.
- Sinks own side effects: DOM writes, GPU draws, network sends, durable writes,
  logging, and atom mutation.
- Each durable atom should have one clear owner flow.
- Event handlers should package input events; they should not compute and store
  broad derived state.
- Render loops should pull a coherent snapshot when that is simpler than pushing
  partial updates through the whole graph.

## Implementation Workflow

1. Trace the current source -> derived flow -> sink path before editing.
2. Identify atom ownership and every writer.
3. Split oversized `m/latest` blocks by concern when unrelated inputs cause
   unnecessary re-entry.
4. Move hidden side effects out of builders and derivations.
5. Keep terminal effects explicit and narrow.
6. Add the smallest test or compile check that proves the new graph shape.

## Common Refactors

- Replace repeated `reset!` calls with one owner flow plus pure derivations.
- Replace derived-state atoms with flows when the value can be recomputed from
  existing sources.
- Split mode selection from branch-specific derivations.
- Use `m/relieve` or `m/sample` when fast producers overwhelm slower consumers.
- For editor or canvas rendering, consider a RAF-driven sink that samples the
  latest coherent state.

## Softland Guardrails

- Preserve established Electric/Missionary safety patterns unless the concrete
  bug requires changing them.
- Be cautious with cancellation-heavy dynamic subscription; prefer static split
  flows unless dynamic subscription clearly removes work or fixes ownership.
- Distinguish "the expression re-ran" from "the value changed."
- Treat the render consumer or reducer edge as the usual side-effect boundary.
- Do not widen the task into an architecture rewrite when a local flow repair is
  enough.

## Output Shape

For implementation work, give:

```text
current flow:
problem:
new flow:
files changed:
validation:
remaining caveats:
```

For explanation-only work, give the dependency chain and the smallest clearer
shape without editing unless the user asked for code changes.
