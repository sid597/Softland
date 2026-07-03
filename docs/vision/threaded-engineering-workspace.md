# Threaded Engineering Workspace - Design Brief

## 1) Context
Teams can ship code quickly with AI support, but understanding, review, and handoff remain slow and fragile.  
The code diff survives; the reasoning path usually does not.

## 2) Problem Statement
Developers and reviewers lose critical context between "work happened" and "work approved."

Current artifacts (PR diff, comments, scattered chat/tool logs) do not reliably communicate:
1. What was intended.
2. What changed.
3. Why this option was chosen.
4. What alternatives were rejected.
5. What evidence supports the claims.
6. What remains risky or unknown.

Result: repeated clarification loops, slower reviews, and weak long-term team memory.

## 3) Product Concept
Softland should represent software work as a navigable thread of reasoning + evidence, not only final code changes.

Goal: help a user move from high-level question to grounded proof in code quickly.

## 4) Users
1. Developer/author
2. Peer reviewer
3. Tech lead / manager reviewer

The same review grammar should apply at all zoom levels, with different detail depth.

## 5) Jobs To Be Done
1. "Help me understand this change before reading raw diff."
2. "Show where proof lives in code/tests/commands."
3. "Preserve reasoning so context can be reloaded later."
4. "Enable feedback on weak links in logic/evidence quickly."

## 6) Design Challenge Prompt
Design a workspace that turns a review from a static document into guided traversal.

A user should start from a question and follow a clear path through:
1. Claims
2. Evidence
3. Decisions
4. Risks/unknowns

with fast access to underlying code context.

Design for cognitive flow, traceability, and confidence-building, not only visual polish.

## 7) Non-Negotiable Outcomes (Design Invariants)
1. High-level summary appears first.
2. Progressive disclosure reveals detail on demand.
3. Key claims are traceable to concrete evidence anchors.
4. Evidence can be opened in code context quickly.
5. Decisions and rejected alternatives are visible.
6. Risks/unknowns are explicit.
7. Context can be revisited later without reconstructing from memory.
8. Branching exploration ("this leads to this") remains legible.

## 8) Core Information Objects
1. Question / Intent
2. Scope
3. Claims
4. Evidence anchors (code/test/command/context/link)
5. Decisions (accepted/rejected/parked)
6. Risks / Unknowns
7. Change clusters / diff context
8. Work session metadata (issue, branch, author, timestamp)

## 9) Primary Workflow Shape
1. Start with a change or question.
2. Read concise narrative.
3. Traverse supporting thread nodes.
4. Open proof in code context.
5. Add feedback where logic/evidence is weak.
6. Reach review decision.
7. Preserve the trail as team memory.

## 10) UX Tensions To Solve
1. Rich context vs information overload
2. Free exploration vs guided path clarity
3. Speed for experts vs legibility for newcomers
4. Live evolving work vs stable review snapshots

## 11) Current Phase Boundary
Current phase focus:
1. In-product review context
2. Threaded understanding for code changes

Deferred for later phases:
1. Broad org-wide graph views
2. Heavy external knowledge ingestion

## 12) Success Criteria
1. Faster reviewer confidence on "what changed and why."
2. Fewer clarification comments / loops.
3. Lower author communication overhead.
4. Better confidence before approval.
5. Better context recovery after time gaps.

## 13) Open Design Space (Intentionally Unspecified)
1. Canvas graph vs timeline vs hybrid
2. Single-screen vs multi-pane information architecture
3. Thread branching visualization patterns
4. Evidence and uncertainty/staleness visualization
5. Feedback attachment model (node, edge, thread-level)

## 14) One-Line Brief
Design a developer-reviewer workspace where code changes are navigated as a structured reasoning thread (intent -> claims -> evidence -> decisions -> risks) so reviewers gain confidence quickly without losing depth.

