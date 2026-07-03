# Design Brief: Reasoning Trails

## The problem in one sentence

When an AI agent writes code, its entire exploration — every file it read, every search it ran, every decision it made, every edit it applied — is thrown away, and the reviewer gets only the final diff.

## Why this matters

Software development is increasingly done by AI agents. A developer gives the agent a task ("add auth to the API"), the agent explores the codebase, makes decisions, writes code. Then a human reviews the output.

Today, that review happens on a diff — lines added, lines removed. The reviewer has no idea:
- What files the agent considered but didn't change
- What alternatives it evaluated before choosing this approach
- Why it picked library A over library B
- What it misunderstood about the codebase
- Where it got stuck and backtracked

This is the same problem that exists at every level of an organization:

```
Agent does work        → Developer reviews (sees only the diff)
Developer does work    → Tech lead reviews (sees only the PR)
Tech lead does work    → PM reviews (sees only the status update)
```

At every handoff, the reasoning disappears. Only the output survives. Each reviewer makes judgments with a fraction of the context that informed the work.

## The artifact that doesn't exist yet

There is a new kind of artifact that sits between "chat log" and "diff":

**The reasoning trail** — a structured record of an exploration through code. It contains:
- **Reasoning**: natural language explaining what the agent is thinking and why
- **Actions**: specific operations (read a file, search for a pattern, edit code) with their targets (which file, which lines, what pattern)
- **Results**: what came back from each action (the code that was read, the search matches, whether the edit succeeded)

This trail has properties that neither chat logs nor diffs have:
- It's **anchored to code** — every action references specific files and line ranges
- It's **sequential** — it tells a story with a beginning, middle, and end
- It's **structured** — actions have types (read/search/edit), not just free text
- It's **zoomable** — you can view it at full detail or compressed to just the key decisions
- It's **nestable** — a human's work session contains multiple agent trails; a tech lead's review contains multiple human sessions

## The users

### The developer (reviewing agent work)
- Wants to understand the FULL trail — every file read, every decision
- Needs to see code in context (not just snippets — the surrounding file)
- Wants to verify the agent's reasoning, not just its output
- May run the agent multiple times, adjusting between runs
- Their sequence of runs + adjustments is itself a trail

### The tech lead (reviewing developer work)
- Does NOT want to see every tool call the agent made
- Wants to see: what decisions were made, what tradeoffs were accepted, what changed
- Needs to drill into detail only where something looks wrong
- Wants to leave feedback anchored to specific decisions (not just "line 42 needs a comment")

### The PM (reviewing team progress)
- Wants: what was done, how long it took, what risks exist, what's next
- One paragraph, not one hundred tool calls
- Needs confidence that the work was reviewed, not just completed

### Common needs across all users
- Browse past trails (by recency, by file touched, by task)
- Search across trails ("show me every time we touched the auth module")
- Resume or extend a trail (continue where the agent left off)

## The data

A trail is a sequence of **nodes**. Each node is one of:

| Node type | Contains | Example |
|-----------|----------|---------|
| Reasoning | Natural language text | "No auth middleware exists. I'll need to create one." |
| File read | File path, line range, content | Read server_jetty.clj lines 45-78 |
| Search | Pattern, scope, matches | Searched for "jwt" in deps.edn — no matches |
| Edit | File path, what changed (before/after) | Changed deps.edn line 12: added buddy-sign |
| File create | File path, content | Created src/app/middleware/auth.clj |

A trail also has metadata:
- Who performed it (which agent, which human)
- Who is the intended reviewer
- What task/prompt initiated it
- When it started and ended
- Its status (running, complete, failed)
- Its parent trail (if nested inside a larger work session)

## The key design challenges

### 1. The zoom problem
The same trail needs to be viewable at radically different detail levels. A developer wants 23 nodes with code blocks. A tech lead wants 5 key decisions. A PM wants 2 sentences. The data is the same — the presentation compresses it.

How do you let users move between zoom levels fluidly? Is it a discrete selector? A continuous zoom? Does the trail re-render, or do sections collapse/expand?

### 2. The code-in-context problem
Trail nodes reference code (file X, lines Y-Z). The viewer needs to understand that code. But a 10-line snippet might not be enough — you might need to see the surrounding function, the imports, the callers.

How do you let users move from "snippet in the trail" to "full file in context" and back without losing their place in the trail?

### 3. The nesting problem
A developer's work session contains multiple agent runs. Between runs, the developer makes their own observations and decisions. The tech lead's review of this work is itself a container.

```
Tech lead review
  └─ Developer work session
       ├─ Agent run 1 (23 nodes)
       ├─ Developer note: "agent missed expiry check, re-running"
       ├─ Agent run 2 (11 nodes)
       ├─ Developer note: "looks good, adding tests"
       └─ Agent run 3 (18 nodes)
```

How do you represent this hierarchy? How deep can it go? How do you navigate in and out of nested trails without getting lost?

### 4. The annotation problem
Reviewers need to leave feedback on trails. Not just "line 42 needs a comment" (that's a diff comment). More like: "why did you choose this library?" or "this decision looks risky" or "good call here."

Annotations are anchored to trail nodes, not to code lines. They're part of the review layer, not the work layer. Multiple reviewers might annotate the same trail.

How do you display annotations without cluttering the trail? How do you distinguish the agent's reasoning from the reviewer's commentary?

### 5. The discovery problem
Over time, trails accumulate. A team generates dozens per week. They become a knowledge base — not just about what changed, but WHY things are the way they are.

How do you find the right trail? By date? By which files were touched? By task/ticket? By searching the reasoning text? How do you see patterns across trails ("we've touched auth 6 times this month")?

### 6. The capture problem
The agent's trail captures itself — every tool call is a structured event in its output stream. But the HUMAN's reasoning (between agent runs, during review) doesn't capture itself. The developer's thoughts — "I reviewed this, decided to adjust, re-ran" — are currently ephemeral.

How does a human add their own reasoning to the trail without it feeling like paperwork? How do you make capture effortless enough that people actually do it?

### 7. The liveness problem
An agent run takes minutes. During that time, the trail is being built in real-time. The user might want to watch it live (like watching someone code), or they might want to review it after completion.

Are these the same view? Does the live view auto-scroll? Can you scroll back while it's still running? Does the trail look different when it's in-progress vs. complete?

## Constraints

### Technical
- The application renders to a single GPU-accelerated canvas (WebGPU), not HTML/CSS
- All text is monospace, rendered via signed distance field font atlas
- Rectangles, text, and hit-test regions are the rendering primitives
- The backend stores data in Rama (an event-sourced distributed database)
- Agent output arrives as a real-time stream (Server-Sent Events)
- Existing editor already handles: file rendering with syntax highlighting, scroll, line numbers, code folding, a command palette, and a basic agent output panel

### Data
- Trail data comes from Claude CLI's streaming output (structured JSON events)
- Each event has a type (text, tool_use, tool_result) and structured content
- The stream is real-time — events arrive one at a time over seconds/minutes
- Trail persistence is in Rama — queryable by session, file, date, task

### Workflow
- The developer works in the editor (writing/reading code) and the command palette (issuing agent commands)
- Agent runs are triggered from the command palette with a text prompt
- The developer may have a file open while reviewing a trail — both need to be accessible
- The sidebar shows the project file tree

## Success criteria

1. A developer reviewing an agent's work can understand WHY the agent made each decision, not just WHAT it changed
2. A tech lead can review the same work at a higher level without reading every tool call
3. Past trails are findable and useful weeks/months later
4. The trail view feels like reading a narrative, not scanning a log
5. Moving between the trail and code context is fluid, not jarring
6. Reviewer annotations feel like a natural part of the trail, not a bolted-on comment system
7. The live streaming experience during an agent run is useful, not just a loading indicator
