# Design task — chat-to-blocks view

v4 · 2026-07-10 · plain design-task language (Sid's ruling: no internal
metaphors, no mood sections — state the exact problem). Paste 1 of 2; the
dataset arrives as the next message.

## Context

We ingest transcripts of human↔agent coding sessions. A pipeline splits
each conversation into addressable units called **blocks**. Every block
has: a stable id, an actor (user / agent / tool), a type (user-message,
user-sub-chunk, agent-thinking, agent-prose-paragraph, agent-list-item,
tool-call, tool-result), and its text. User messages are additionally split
into sub-chunks: the message is the parent, its paragraphs are children.
Tool/system noise is retained in the data but is not part of the main flow.

Nobody has ever seen this data rendered. That is the task.

## The task

Design a two-pane, scrollable, read-only view of ONE real conversation:

- **Left pane:** the raw conversation, as a normal chat log reads.
- **Right pane:** the same conversation as its blocks.

The core design problem is the **right pane**: how blocks are represented.

## The structure the right pane must express

1. Within one agent response, blocks are ordered top-down (thinking, then
   prose paragraphs, list items, tool calls).
2. A user message plus the agent response to it form one unit (a turn).
3. Turns in sequence form the conversation.
4. User messages contain sub-chunks (parent/child).

So the hierarchy is: block → response → turn → conversation. Four levels of
nesting; sibling order always matters.

## Explorations wanted

Render each as a full screen over the SAME dataset, desktop width, enough
rows on screen to judge real density:

- **A. Outline** — indented blocks, Roam/Workflowy-like.
- **B. Canvas** — blocks as spatial cards, Miro-like, grouped by turn.
- **C. Tree/topology** — explicit structure, nodes and edges.
- **D. At least one direction of your own** that none of the above covers.

The human picks by looking, so make the differences real, not skins.

## Design questions every exploration must answer

- What does a single block look like? (id, actor, type, text — id visible
  but subordinate to the text)
- How is a sub-chunk shown relative to its parent message?
- How does a 40-line thinking block coexist with one-line blocks —
  collapse, preview, something else?
- Where are the turn boundaries, at a glance?
- How does left↔right correspondence work — hover or select a message and
  see its blocks, and the reverse?
- Where does tool/system noise sit? (present, de-emphasized, never deleted)
- The dataset is page 1 of a longer conversation — how does the view say
  "there is more"?

## Data

Next message: 64 real blocks from one conversation — real ids, actors,
types, and text. Use it as-is; the wildly variable text lengths are part of
the test. Do not summarize or paraphrase the text; render it.

## Constraints

- Read-only. No tasks, queues, workflow actions, or editing.
- Ids and types are real data — never invent ids or hide them entirely.
- Visual tone in one line: quiet, text-forward, built for sustained
  reading; not a dashboard.
