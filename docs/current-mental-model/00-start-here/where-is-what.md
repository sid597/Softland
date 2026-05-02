# Where Is What

Status: human routing map for `docs/current-mental-model`, 2026-05-02.

This page answers the practical question: "where do I look or write this?"

## Folder Order

```text
README.md
  Stable overview and read order.

context-map.md
  Rules for what belongs in global context, handoff, architecture notes, trails,
  and prompts.

00-start-here/
  Bootstrap and narrative orientation. Read this first when opening a new chat
  or rebuilding context.

10-anchors/
  Concrete anchors and wall artifacts. Use these when abstractions are too
  floaty and we need a grounded walkthrough or visual map.

architecture/
  Durable architecture notes and recurring systems-level distinctions.

architecture/dogfood-runtime/
  Runtime direction for World / Compute / LLM-agent depots.

trails/
  Dated traces of important reasoning or implementation episodes until Rama
  stores trails natively.

90-prompts/
  Execution prompts. Use only after the user has chosen to review or implement.

docs/sessions/next-prompt.md
  Active handoff only. This is not global context.
```

## Current High-Value Files

```text
00-start-here/new-chat-bootstrap.md
  Fresh-chat bootstrap when no next task is chosen.

00-start-here/conversation-trail.md
  Short narrative of how the model got here.

10-anchors/rama-world-kernel-text-instance.md
  Concrete vertical walkthrough through the Rama world kernel using text.

10-anchors/rama-world-kernel-v1-wall-map.txt
  Full visual wall artifact for the current Rama kernel shape.

architecture/dogfood-runtime/slice-a-compute-run-command.md
  Canonical Slice A compute-run architecture.
  Also contains the executor-placement origin question and Rama/AOR answer.

architecture/dogfood-runtime/compute-track.md
  General compute track: build/test/run/deploy/serve through Rama.

architecture/dogfood-runtime/agent-track-aor.md
  LLM/agent track and AOR-shaped long-running execution.

90-prompts/implementation-slice-a-compute-run-command-prompt.md
  Slice A implementation prompt. Do not use for open-ended architecture.
```

## Where To Put New Information

```text
Stable global orientation
  -> README.md or 00-start-here/new-chat-bootstrap.md

Rules about document routing
  -> context-map.md

Question-driven architecture distinction
  -> architecture/<topic>.md
  -> include "Origin question:" and "Why this matters:"

Dogfood runtime details
  -> architecture/dogfood-runtime/<track-or-slice>.md

Executor / TaskGlobal / out-of-band compute notes
  -> architecture/dogfood-runtime/slice-a-compute-run-command.md
  -> architecture/dogfood-runtime/compute-track.md if generalized

Dated reasoning trail
  -> trails/YYYY-MM-DD-short-name.md

Implementation prompt
  -> 90-prompts/<task-name>-prompt.md

Resume context for an active task
  -> docs/sessions/next-prompt.md
```

## Current Rule Of Thumb

```text
Global context explains how to think.
Architecture notes preserve durable distinctions.
Trails preserve how a decision emerged.
Prompts tell an agent what to execute.
Handoffs resume one active task.
```
