---
name: ask-codex-for-feedback
description: Use when preparing a prompt for Codex to review, falsify, gate, or give feedback on a Codex/chat artifact such as an architecture draft, design sketch, implementation plan, code diff, prompt, or canonical candidate. The skill packages the artifact with mode, altitude, authority, preserve/do-not-reopen constraints, source scope, and exact output format so Codex reviews the intended thing instead of solving a different problem.
allowed-tools: Read, Grep, Glob, Bash
---

# ask-codex-for-feedback

Use this skill to package an artifact for Codex review. The sender's job is
context hygiene: give Codex enough structure to review the right thing without
pre-biasing it into agreement or inviting it to solve a different problem.

## Core Rule

Never ask Codex:

```text
What do you think?
```

Ask Codex:

```text
Review this artifact at this altitude, under this authority level, using this
source scope, and output only these kinds of findings.
```

## Classify The Artifact

Before writing the Codex prompt, label the artifact:

```text
MODE:
  DRAFT   - exploratory artifact; feedback may challenge shape
  REVIEW  - find issues; do not rewrite freely
  REPAIR  - apply accepted corrections only
  GATE    - decide if ready for implementation/commit/use

ALTITUDE:
  meaning       - intent, invariants, conceptual shape
  architecture  - boxes, arrows, source-of-truth, ownership
  contract      - APIs, schemas, partition keys, states, lifecycle, tests
  code          - actual diff, behavior, regressions

AUTHORITY:
  exploratory   - open to reframing
  candidate     - preserve accepted decisions unless invalid
  canonical     - do not reopen without fatal contradiction
```

If you cannot classify it, ask the user or mark the uncertainty in the prompt.

## Package Without Bias

Include:

- The raw artifact or the smallest self-contained excerpt.
- The question Codex should answer.
- What Codex should preserve.
- What Codex should not reopen.
- The source scope Codex should use.
- The output format required.

Avoid:

- "I think this is correct" or "please confirm".
- Long persuasive explanation before the artifact.
- Mixing rejected old versions into the main artifact body.
- Asking for both redesign and review unless the user explicitly wants that.
- Hiding uncertainty; mark it as OPEN instead.

## Prompt Template

```text
MODE: <DRAFT | REVIEW | REPAIR | GATE>
ALTITUDE: <meaning | architecture | contract | code>
AUTHORITY: <exploratory | candidate | canonical>

TASK:
Review the artifact below. Do not solve from scratch unless a concrete claim is
invalid. Give the smallest corrections that preserve the accepted shape.

SOURCE SCOPE:
- Use the artifact as the primary object of review.
- Verify repo/library/framework claims against local files/docs when relevant.
- If you make an inference without verification, label it as inference.

PRESERVE:
- <accepted invariant / decision>
- <accepted boundary / scope>

DO NOT REOPEN:
- <old decision>
- <deferred scope>

CHECK SPECIFICALLY:
- <risk category 1>
- <risk category 2>
- <risk category 3>

OUTPUT:
1. Verdict: READY | NEEDS REVISION | UNSAFE | WRONG ALTITUDE
2. Fatal findings
3. Soft findings
4. What should be preserved
5. Smallest corrected contract or next handoff

ARTIFACT:
<paste artifact>
```

## Mode-Specific Requests

For architecture review:

```text
CHECK SPECIFICALLY:
- source-of-truth direction
- ownership boundaries
- event/dataflow arrows
- claims that sound implementation-ready but are only conceptual
- missing concrete mechanism at the contract boundary
```

For Rama review:

```text
CHECK SPECIFICALLY:
- PState ownership
- depot partitioning and local ordering
- partitioner/event-boundary atomicity
- retry plus external side effects
- ack-level semantics
- foreign-proxy/select paths
- TaskGlobal/executor handoff
- idempotency keys and lifecycle guards
```

For code review:

```text
CHECK SPECIFICALLY:
- behavioral regressions
- missing tests
- edge cases
- concurrency/retry/idempotency issues
- whether the diff implements the stated contract
```

For meaning review:

```text
CHECK SPECIFICALLY:
- whether the artifact preserves the intended invariant
- whether important tension was flattened
- whether wording changes shifted the concept
- whether implementation detail is distorting the meaning
```

## If The Artifact Is Too Large

Create an artifact capsule:

```text
ARTIFACT CAPSULE:
name/version:
current state:
accepted invariants:
open questions:
exact excerpt to review:
links/paths for full context:
```

Do not summarize away the claim Codex must judge. If a precise claim matters,
quote that specific sentence or diagram fragment.

## Return To The User

When this skill is used, output:

```text
Codex prompt
  <paste-ready prompt>

Why this framing
  <1-3 bullets explaining mode/altitude/authority>

What I left out
  <biasing context or old versions omitted on purpose>
```

Keep the generated prompt direct. The goal is to reduce the user's work as the
human message bus, not create a new meta-document.

## Companion skills

- `review-cross-model-artifact` - the Codex-side receiving skill. This is what
  Codex should use when the packaged artifact lands.
- `ingest-codex-feedback` - ingests Codex's review when it lands, repairs the
  artifact minimally, and emits the next paste-ready Codex follow-up prompt.
  Use after `second-order-mirror`.
- `second-order-mirror` - internal discipline to run when Codex's review
  arrives, BEFORE drafting the response. Separates genuine agreement from
  accommodation; surfaces silent position retreats.
- `rama-pitfalls` - load the relevant section numbers into the
  CHECK SPECIFICALLY block when MODE is Rama-domain. Reference by section
  (e.g. "rama-pitfalls #2: side-effect retry trace") rather than re-stating
  the whole protocol inline.
