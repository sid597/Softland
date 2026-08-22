---
name: ingest-codex-feedback
description: Use when Codex receives Codex's review, critique, gate result, or feedback on a Codex/chat artifact and must ingest it constructively. Treat Codex as a contract reviewer or alternate perspective, not an adversary. Classify each finding, preserve valid Codex intent, repair the artifact minimally, and automatically produce the next paste-ready Codex follow-up prompt when another Codex round is useful.
allowed-tools: Read, Glob, Grep
---

# Ingest Codex Feedback

Use this after Codex replies to an artifact Codex sent for review. The goal is
not to "answer Codex" as an opponent. The goal is to convert Codex's perspective
into a better artifact with as little user orchestration as possible.

## Core Rule

Codex feedback is input to synthesis:

```text
ingest review
classify findings
repair minimally
surface real disagreements
emit the next paste-ready Codex prompt automatically
```

Do not default to:

```text
defend the old artifact
argue with Codex
rewrite from scratch
ask the user to invent the next prompt
```

## When To Run

Run when the user pastes Codex's review, gate result, or critique and wants
Codex to continue the loop.

Common triggers:

```text
/ingest-codex-feedback
here is Codex's review
Codex replied with this
ingest this and repair the artifact
```

Do not run for pure thanks, one-line PASS results, or brand-new design prompts.

## Inputs To Extract

The user may provide only Codex's review. If the prior artifact is available in
chat context, use it. If not, ask for it only when the review cannot be acted on
without seeing the artifact.

Extract:

```text
artifact under review:
Codex verdict:
fatal findings:
soft findings:
what Codex says to preserve:
smallest correction proposed:
```

## Ingestion Protocol

### 1. Apply Second-Order Discipline

Before responding, check:

```text
Am I accepting because Codex is right, or because Codex sounded confident?
Am I disputing because Codex is wrong, or because I am attached to my draft?
Am I changing altitude instead of solving the actual disagreement?
```

Use `second-order-mirror` when the review creates accommodation,
defensiveness, silent retreat, or cross-altitude confusion.

### 2. Classify Every Substantive Finding

Label each finding exactly one of:

```text
ACCEPT
  Codex is right. Repair the artifact.

ACCEPT-WITH-REFRAME
  Codex is right about the contract, but the original framing or invariant
  remains useful. Repair the contract without throwing away the shape.

PARTIAL
  Split the finding into the part accepted and the part not accepted.

DISPUTE
  Codex is wrong or overclaimed. Cite docs, code, or settled context.
  Dispute without citation is not allowed.

DEFER
  Valid but out of this slice/version. Preserve as future work.

NEEDS-CLARIFICATION
  Cannot act without a sharper claim or missing source.
```

If all findings are accepted, say so plainly, repair the artifact, and prepare
a gate prompt. Do not make acceptance theatrical.

### 3. Preserve Strong Points Explicitly

List what remains true after ingestion:

```text
PRESERVED:
  - accepted slice choice / invariant
  - source-of-truth direction
  - dataflow shape
  - scope boundaries
```

This prevents silent retreat, where a good original position disappears just
because Codex found real contract flaws.

### 4. Repair Minimally

Produce the smallest artifact change that makes accepted findings true.

Do not:

```text
expand scope
reopen settled direction
turn a contract repair into a fresh architecture essay
hide unresolved issues inside nicer prose
```

For Rama-shaped artifacts, preserve this loop unless Codex proved it invalid:

```text
ActionRequest -> depot -> Rama decision
  -> accepted fact / run intent
  -> observations back into Rama
  -> PState materialization
  -> UI/projection reads Rama
```

### 5. Choose The Next Move

At the end, choose one status:

```text
NEXT: SEND_TO_CODEX_GATE
  Artifact was repaired and needs a final Codex gate.

NEXT: SEND_TO_CODEX_CLARIFY
  One or more Codex findings need clarification before repair.

NEXT: USER_DECISION_NEEDED
  Remaining question is product/scope/trust policy, not something either
  model can resolve from docs.

NEXT: READY_TO_IMPLEMENT
  Codex already gave a gate pass, or another Codex round would be loop noise.
```

## Required Output

Return these sections, in this order:

```text
Ingestion Verdict
  ACCEPT-MOSTLY | MIXED | DISPUTE-MOSTLY | BLOCKED

Finding Disposition
  FINDING 1:
    label:
    reason:
    artifact change:
  FINDING 2:
    ...

Preserved
  - ...

Repaired Artifact
  <smallest repaired artifact, or exact delta if full artifact is too big>

Remaining Questions
  - ...

Next Move
  NEXT: <one of the four statuses>
  why:

Paste To Codex
  <copy-paste-ready block, or "N/A" with reason>
```

## Auto-Prompt Rules

The user should not have to invent the next Codex prompt. Always include a
`Paste To Codex` block unless `NEXT: USER_DECISION_NEEDED` or
`NEXT: READY_TO_IMPLEMENT` makes another Codex round wasteful.

### If The Repaired Artifact Needs A Gate

Use this shape:

```text
Use review-cross-model-artifact.
Use think-in-rama if this artifact is Rama-shaped.

MODE: GATE
ALTITUDE: <architecture | contract | code>
AUTHORITY: candidate

TASK:
Review the repaired artifact below. Do not redesign from scratch. Check only
whether the accepted findings from your prior review were repaired and whether
any fatal blocker remains.

PRESERVE:
- <settled invariants>

OUTPUT:
1. GATE VERDICT: READY TO CODE | NEEDS REVISION | UNSAFE
2. Fatal blockers only
3. Smallest correction required before code

REPAIRED ARTIFACT:
<artifact>
```

### If Codex Clarification Is Needed

Use this shape:

```text
Use review-cross-model-artifact.

MODE: REVIEW
ALTITUDE: <contract | architecture | code>
AUTHORITY: candidate

TASK:
Clarify only the specific findings below. Do not review the full artifact
again. For each question, answer with:
  - your precise claim
  - evidence/source
  - smallest actionable correction

QUESTIONS:
1. <question>
2. <question>

CONTEXT:
<brief artifact context>
```

### If User Decision Is Needed

Ask the smallest concrete question and stop. Do not pretend another model can
settle product, scope, or trust policy.

Examples:

```text
Do you want Slice A to include cancel, or keep cancel as Slice 2?
Should local-trusted run arbitrary argv, or require an argv allowlist?
```

## Anti-Patterns

```text
ANTI-PATTERN                  FIX
------------------------------------------------------------
"Codex is adversarial"        Treat Codex as review input.
"Codex must defend itself"   Preserve only what survives inspection.
"User writes next prompt"     Always emit Paste To Codex when useful.
"Everything becomes vNext"    Apply accepted/partial corrections only.
"Dispute by vibes"            DISPUTE requires citation.
"Infinite ping-pong"          After two review/repair cycles, force
                              GATE or USER_DECISION_NEEDED.
```

## Companion Skills

- `ask-codex-for-feedback` - use before sending a Codex artifact to Codex.
- `second-order-mirror` - use before ingesting when the review creates
  accommodation, defensiveness, or cross-altitude confusion.
- `rama-pitfalls` - use as a domain checklist for Rama-shaped artifacts.
