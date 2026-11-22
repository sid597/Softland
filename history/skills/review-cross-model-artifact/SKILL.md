---
name: review-cross-model-artifact
description: "Review chat artifacts from Claude or another model for critique, falsification, gating, or feedback. Use when Codex receives a supplied artifact and must stay bounded to that artifact state, separate meaning-level concerns from architecture/contract/code correctness, preserve what is right, give the smallest corrections, or produce paste-ready feedback for another model."
---

# Review Cross-Model Artifact

Use this when receiving an artifact from another chat or model and the user wants
Codex feedback. The job is not to generate another universe. The job is to
review the artifact at the requested altitude and make the next handoff cleaner.

## First Move

Classify the request before giving feedback:

```text
MODE:
  DRAFT   - artifact is exploratory; review shape without over-gating
  REVIEW  - find issues; do not rewrite unless asked
  REPAIR  - apply accepted corrections only
  GATE    - decide if artifact is ready for code/commit/use

ALTITUDE:
  meaning       - intent, invariants, conceptual shape
  architecture  - system boundaries, dataflow, ownership
  contract      - concrete APIs, schemas, keys, lifecycle, tests
  code          - actual diff, behavior, regressions, test risk

AUTHORITY:
  exploratory   - can be challenged freely
  candidate     - preserve accepted decisions
  canonical     - do not reopen without fatal contradiction
```

If the user did not name these, infer them and say the inference briefly. Ask a
question only when the wrong altitude would make the feedback actively harmful.

## Review Rules

- Review the supplied artifact, not the original broad problem.
- Do not redesign from scratch unless a concrete claim is invalid.
- Separate "what is correct and should be preserved" from "what must change".
- Do not turn meaning-level artifacts into code-gate reviews unless the user asks.
- Do not let beautiful architecture prose pass as an executable contract.
- For repo-, docs-, library-, legal-, financial-, or current facts, verify against
  the relevant source before making strong claims.
- When reviewing code, use normal Codex code-review stance: findings first,
  severity ordered, file/line references when available.

## Feedback Protocol

1. Name what you are reviewing.
2. Name the role you are taking.
3. Extract the artifact's key claims.
4. For each issue, give:
   ```text
   claim in artifact:
   concern:
   evidence/rule:
   smallest correction:
   ```
5. Preserve good decisions explicitly.
6. Mark out-of-scope temptations so they do not reopen the design.
7. If the artifact should go back to another model, include a paste-ready
   feedback block.

## Altitude Routing

```text
meaning feedback
  ask: does this preserve the intended invariant or experiential shape?
  avoid: implementation nitpicks unless they contradict the meaning

architecture feedback
  ask: are the boxes, arrows, ownership, and source-of-truth claims coherent?
  avoid: DSL details unless the artifact claims implementability

contract feedback
  ask: what exact depots/PStates/APIs/keys/states/tests make this true?
  avoid: new vision or product expansion

code feedback
  ask: what breaks in the diff, what tests are missing, what regressions appear?
  avoid: architecture rewrites unless the code cannot be made correct locally
```

## Output Shapes

For a normal artifact review:

```text
Verdict: READY | NEEDS REVISION | UNSAFE | WRONG ALTITUDE

What I am reviewing
  <artifact name/version and inferred mode>

Findings
  1. <severity>: <issue>
     claim:
     concern:
     evidence:
     smallest correction:

What to preserve
  - <accepted/correct parts>

Out of scope
  - <things I am not reopening>

Next handoff
  <optional paste-ready prompt or correction list>
```

For feedback intended to paste back to Claude:

```text
FEEDBACK TO CLAUDE

Preserve:
  - <things the artifact got right>

Repair:
  - <specific corrections, smallest possible>

Do not reopen:
  - <accepted scope/meaning decisions>

Produce:
  <exact requested artifact state, e.g. "v2.2 canonical candidate">
```

For implementation gates:

```text
GATE VERDICT: READY TO CODE | NEEDS REVISION | UNSAFE

Required contract before code:
  - data entering the system
  - ownership/source of truth
  - schemas/API shapes
  - lifecycle transitions
  - idempotency/retry behavior
  - observability/error paths
  - tests that prove the contract
```

## Guardrails

- "Looks good" is never sufficient feedback.
- "Here is my alternative design" is usually the wrong response.
- If a finding depends on a source you did not check, label it as an inference.
- If two agents are disagreeing, first classify the disagreement:
  ```text
  meaning | architecture | contract | code | wording
  ```
- If the artifact is a canonical candidate, make the smallest correction that
  preserves the accepted shape.

## Tiny Template

When the user just says "Codex feedback on this", use:

```text
I am treating this as:
  MODE:
  ALTITUDE:
  AUTHORITY:

Verdict:

Findings:

Preserve:

Smallest next move:
```

## Companion Skills

- `think-in-rama` - use when the incoming artifact is Rama-shaped and needs
  Softland-specific depot, PState, topology, policy, or traceability checks.
- `review-reactive-architecture` - use when the artifact concerns Electric or
  Missionary reactive structure, invalidation, ownership, or side effects.
- `deliver-like-a-staff-level-product-architect` - use when the artifact is a
  product/UI review or implementation handoff that needs premium-bar severity.
