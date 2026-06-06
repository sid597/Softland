# Current Mental Model Context Map

Status: meta map for new sessions, not an implementation handoff.

This folder has two jobs that must stay separate:

```text
global context
  = what a new session must understand to talk intelligently about Softland/Rama

task handoff
  = what a new session should execute next, if we have already chosen the task
```

Do not confuse them. A session can read the global context and then discuss a
new direction with the user without assuming any named next implementation.

## Session Entry Points

Use these by intent:

```text
00-start-here/where-is-what.md
  -> human routing map
  -> use when asking "where does this go?" or "where is that model?"

00-start-here/new-chat-bootstrap.md
  -> global context bootstrap
  -> use when starting a new conversation and the next direction is not fixed

90-prompts/implementation-review-prompt.md
  -> implementation/review prompt
  -> use only when the user is ready to inspect or change code

90-prompts/implementation-slice-a-compute-run-command-prompt.md
  -> historical Slice A implementation prompt
  -> use only to understand the original compute-spine implementation protocol

docs/sessions/next-prompt.md
  -> task handoff
  -> use only when there is an active next task to resume
```

If the user says they have a new direction to discuss, start from
`00-start-here/new-chat-bootstrap.md`, not `docs/sessions/next-prompt.md`.

## Where Things Go

```text
README.md
  -> index, read order, current center, and stable summary
  -> should stay short enough to scan

context-map.md
  -> meta map of this folder
  -> where to put global context vs handoff vs exploration vs implementation notes

00-start-here/where-is-what.md
  -> human routing map of folders, high-value files, and where new information
     belongs

00-start-here/new-chat-bootstrap.md
  -> copy/paste prompt for a fresh chat
  -> should give the model the right altitude without prescribing the next task

00-start-here/conversation-trail.md
  -> compact narrative of how the model evolved
  -> useful when the user asks "how did we get here?"

trails/*.md
  -> pre-Rama trace captures for important reasoning/build episodes
  -> preserve origin pressure, artifact lineage, decisions, handoffs, and later
     implementation proof while Rama trail storage does not exist yet

10-anchors/rama-world-kernel-text-instance.md
  -> historical concrete vertical walkthrough through the kernel using text
  -> use when abstractions feel floaty

10-anchors/rama-world-kernel-v1-wall-map.txt
  -> visual/ascii map of the pre-rename Rama system shape
  -> use when the user wants to build the model spatially

90-prompts/implementation-review-prompt.md
  -> code-review / implementation prompt
  -> includes preflight questions, acceptance criteria, and anti-lossiness checks

90-prompts/implementation-slice-a-compute-run-command-prompt.md
  -> historical Slice A compute-run implementation prompt
  -> use to understand how the compute-spine implementation was kicked off, not
     as an active handoff

architecture/*.md
  -> durable architecture notes, open questions, and settled distinctions
  -> one file per recurring systems-level question

architecture/dogfood-runtime/*.md
  -> current Rama/AOR dogfood runtime direction
  -> compute track, LLM-agent track, the three-depot system shape, the
     implemented Slice A compute spine, and the implemented LLM contract MVP
  -> not a task handoff

build/object-container/*.md
  -> implementation-slice contracts for object-container work
  -> read only when the user has chosen to implement that slice

Post `1ef1cbd`, active code uses space/turn vocabulary. Older docs that say
world-thread/world-turn are historical unless explicitly marked current.

trails/2026-05-02-slice-a-cross-model-experiment.md
  -> trace of the Slice A Claude/Codex/user experiment before Rama can store
     trails natively
  -> use when asking how the compute-spine decision emerged
```

## Architecture File Roles

```text
architecture/action-request-kernel-routing.md
  -> request/decision/event distinction, routing, and kernel contract

architecture/logical-lifecycle-and-derived-depots.md
  -> lifecycle vs physical depot layout, and source-of-truth vs derived depots

architecture/object-container-spec.md
  -> settled object substrate: SourceArtifact, ObjectContainer, Revision,
     DerivedUnit, composition, projection, situate, and graduation

architecture/object-container-reviewer-world-model.md
  -> Codex reviewer lens for future sessions: kernel fit, readiness, first
     slice gate, and red flags

build/object-container/IMPLICIT_SPEC.md
  -> first implementation slice contract for markdown import, outline, and
     graduate-on-edit; not global context unless implementing

architecture/policy-granularity-mastodon-parallel.md
  -> exploratory policy granularity note; not settled design

architecture/rama-blog-patterns.md
  -> lessons from Rama examples/blogs

architecture/rama-policy-throughput-post.md
  -> throughput and policy placement reasoning

architecture/rama-world-kernel-v0-pr-trail.md
  -> trace of the V0 PR/model change

architecture/prompt-to-implementation-lossiness.md
  -> how discussed model gets lost during implementation, and the guardrail

architecture/cross-model-architecture-loop.md
  -> workflow insight from the Slice A experiment: Claude initializes, Codex
     gates, Claude ingests/redraws, Codex implements

architecture/dogfood-runtime/README.md
  -> overview of the new dogfood runtime direction

architecture/dogfood-runtime/compute-track.md
  -> build/test/run/deploy/serve execution through Rama-owned run state and
     observation streams

architecture/dogfood-runtime/agent-track-aor.md
  -> AOR-shaped LLM/agent execution through Rama-owned run state and streaming
     observation depots

architecture/dogfood-runtime/three-depot-current-system.md
  -> current three conceptual depot families: World, Compute, LLM-agent

architecture/dogfood-runtime/slice-a-compute-run-command.md
  -> implemented Slice A.0 spine for the first dogfood-runtime vertical slice:
     :compute/run-command through TaskGlobal-owned claim/reconcile,
     observations, and live view
  -> use as architecture context for implementation discussion, not as an
     automatic handoff

architecture/dogfood-runtime/llm-track-slice-roadmap.md
  -> implemented LLM contract MVP record: world-first turns, ContextBundles,
     LLM runs, controls, executor claims, catalog/slices/forks/patch proposals,
     projections, costs, test counts, and slice commits
  -> use as architecture context and implementation proof, not as an automatic
     next-task handoff

architecture/dogfood-runtime/llm-track-claude-research.md
  -> Claude executor surface research adjacent to the Codex-backed LLM MVP

architecture/dogfood-runtime/transcript-capture.md
  -> candidate passive-observation slice for harvesting/watching Claude
     transcripts; not implemented in the LLM MVP
```

## What Counts As Global Context

Global context is the material a new session needs before it can reason with us:

```text
Softland is a world/place, not a tool.
Rama is the event-sourced world substrate, not a database attached to an app.
Projection reads materialized state and emits proposed action data.
ActionRequest asks.
ActionDecision records Rama's answer.
KernelEvent happened only if accepted.
Rejected decisions are durable but are not world facts.
PStates are materialized/queryable world views.
Text is the first carrier; it is not the ontology.
ObjectContainer is the durable identity atom; SourceArtifact preserves raw
source; Revision stores authored content; DerivedUnit graduates on touch.
Dogfood runtime direction: WorldDepot is truth, ComputeDepot does physical
execution, LLMDepot does epistemic/agent execution.
Workers and agents stream observations back into Rama; the UI reads Rama.
Compute Slice A.0 and the LLM contract MVP are implemented vertical spines, but
they do not choose the next task.
```

Global context should not say:

```text
the next task is already chosen
the user has already chosen the next implementation path
all open explorations are settled design
```

## What Counts As Handoff

Task handoff is execution-specific:

```text
current branch
current dirty worktree hazards
exact next task
files to edit
tests to run
commit/push instructions
known blockers
```

Handoff expires quickly. Global context should age more slowly.

## Rule For New Directions

When the user brings a new potential direction:

```text
1. Read the global context bootstrap.
2. Do not assume the old handoff is still active.
3. Restate the current model only as needed.
4. Let the new direction collide with the model.
5. If a new implementation path emerges, write a fresh handoff after the decision.
```

The goal is not to make the new session remember everything. The goal is to give
it the right compression: enough stable context to think with the user, without
smuggling in an old next step.
