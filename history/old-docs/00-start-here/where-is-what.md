# Where Is What

Status: human routing map for `docs/current-mental-model`, 2026-05-13.

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
  Runtime direction for Space / Compute / LLM-agent depots.

build/object-container/
  Implementation-slice contracts for object-container work. Use only after the
  object-container direction has been chosen for implementation.

build/object-container-common-infra/
  Current common-infra track. Use this before resuming markdown or transcript
  implementation, because the active product goal is shared object-container
  import infrastructure rather than source-local stores.

build/chat-ingester/
  Transcript-specific product/proof artifacts. Use these as inputs to the
  common-infra track, not as the active F5/F6 implementation queue unless the
  user explicitly asks to harden the old prototype.

build/code-ingestor/
  Code ingestor product track. PRODUCT.md is the proposed (not ratified)
  product contract for ingesting code as a third object-container source.
  Use before any code-import implementation; gated on Sid ratification and a
  Codex falsification pass.

build/imported-topology-view/
  Product/design research track for the first view over imported material. Use
  this for Source-to-World / imported topology view thinking, designer
  evaluation, and view briefs. It should not edit Rama common-infra code.

build/knowledge-earth-zui/
  Big-vision design research track for Knowledge Earth, semantic zoom, category
  theory, model translation, paper-as-log, conflict, synthesis, and collective
  intelligence. Use this when asking what Softland becomes after import/source
  trust is no longer the foreground task.

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
  Historical concrete vertical walkthrough through the Rama kernel using text.
  Active text code now lives in text_kernel.clj; shared contracts live in core.clj.

10-anchors/rama-world-kernel-v1-wall-map.txt
  Full visual wall artifact for the pre-rename Rama kernel shape.

architecture/dogfood-runtime/slice-a-compute-run-command.md
  Implemented Slice A compute-run architecture.
  Also contains the executor-placement origin question and Rama/AOR answer.

architecture/dogfood-runtime/llm-track-slice-roadmap.md
  Implemented LLM contract MVP record.
  Start here for the slice commits, verification counts, and cost-rollup
  simplification note.

architecture/dogfood-runtime/compute-track.md
  General compute track: build/test/run/deploy/serve through Rama.

architecture/dogfood-runtime/agent-track-aor.md
  LLM/agent track and AOR-shaped long-running execution.

architecture/object-container-spec.md
  Settled object substrate: source, container, revision, derived unit,
  composition, projection, situate, and graduation.

architecture/object-container-ingester-contract.md
  Corrective contract for source-specific ingesters. Use this before planning
  transcript, Roam, code, canvas, or Linear import. It defines what must be
  common base object-container truth versus source-specific projections/indexes.

build/object-container-common-infra/PRODUCT.md
  Active product seed for the common object-container import infrastructure
  track. Start here when the next task is to make markdown and transcript both
  feed the shared Object-Container Kernel.

build/code-ingestor/PRODUCT.md
  Proposed product contract for the code ingestor (fresh cut, autopilot,
  2026-06-10): trail-to-code resolution as first consumer, stable file
  identity across commits, git-stays-authority, commit-boundary ingest,
  fail-closed scope policy. Awaiting Sid ratification + Codex falsification.

build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
  Principal-design research synthesis for the first view over imported
  material. It frames Softland's view problem as accountable transformation:
  raw source becoming native world material without losing provenance, identity,
  uncertainty, or local orientation.

build/knowledge-earth-zui/CATEGORY_THEORY_DESIGN_RESEARCH.md
  Big-vision research note connecting Softland to category theory, ologs,
  Topos-style collective intelligence, semantic ZUI, learning arrows,
  non-commuting disagreement, synthesis as gluing, and local worlds as
  composable structures.

architecture/object-container-reviewer-world-model.md
  Codex reviewer lens for future sessions: where the spec fits in the kernel,
  what is ready, and what first implementation must prove.

build/chat-ingester/PRODUCT.md
  Product contract for transcript ingest as an object-container interpreter:
  import/watch acquisition, source-specific parsing, native containers, anchors,
  privacy, and the distinction from Space.

build/chat-ingester/COMMON_KERNEL_REPLAN.md
  Corrective gate for transcript ingest. Use before any further F5/F6 work if
  the goal is the shared object-container-kernel architecture rather than
  hardening the transcript-local prototype.

build/object-container/IMPLICIT_SPEC.md
  First implementation slice contract for markdown import, outline projection,
  and graduate-on-edit. Use when implementing, not as default bootstrap.

90-prompts/implementation-slice-a-compute-run-command-prompt.md
  Historical Slice A implementation prompt. Do not use for open-ended
  architecture or as a current handoff.
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

Object-container implementation contract
  -> build/object-container/<slice>.md
  -> keep separate from global context and do not treat as an automatic handoff

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
