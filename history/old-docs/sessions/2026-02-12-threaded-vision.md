# Session Notes - 2026-02-12 (Threaded Codebase Guidance Vision)

## User-stated end goal

Build Softland into a system that helps humans traverse complex codebase threads with strong context continuity.

The core workflow is not "AI answers in text." It is:

1. User asks a broad question (example: "How does Softland work?")
2. AI explores the codebase and returns an explanation
3. Every claim in that explanation is attached to concrete file/region evidence
4. User clicks through those evidence anchors and drills deeper
5. The system progressively reveals the thread ("this leads to this leads to this") across files and layers

The desired behavior is like a guide through a valley:
- Not just describing the route
- Pointing at the exact path and landmarks while you walk

---

## Product framing captured

### Problem to solve

In real code work (understanding, implementing, reviewing), progress comes from following chains of causality:
- entrypoint -> call path -> state transition -> side effects -> result

Current tools often return disconnected summaries. The target is a navigable reasoning path with evidence.

### Primary use cases

1. **Understand existing system**
- "How does this codebase work?"
- AI builds and presents main architectural/behavioral threads.

2. **Implement feature**
- "Where should I edit?"
- AI identifies entrypoints + downstream impact path.

3. **Review change**
- "What is the context for this diff?"
- AI reconstructs surrounding thread and risk points.

### UX principle

"Thread first, prose second."

Text is useful, but the primary object should be an inspectable thread made of:
- nodes (claims/steps)
- edges (leads-to/dependency)
- evidence links (file span, symbol, callsite, event)

---

## Architectural implication (aligned with current direction)

The user's suggested direction is valid:
- Move from direct large file loading/parsing in UI toward Rama-backed reactive projections.

But it should be implemented as **windowed + cached reactive reads**, not synchronous backend fetch per wheel tick.

### Correct shape

1. Rama holds canonical project/file index and version metadata
2. UI opens a file via an initial content window
3. Scroll triggers prefetch of adjacent windows (ahead/behind)
4. UI keeps a local ring cache for snappy render/hit-testing
5. Thread/evidence clicks jump to precise regions and ensure the needed windows are hydrated

### Why this matters

- Keeps UI fast on large codebases
- Makes "thread navigation" practical (jumping across files/spans)
- Preserves a single source of truth for provenance and replay

---

## Proposed data model additions (vision-level)

### 1) Project index state
- project-id
- file-path
- language
- size/mtime/hash
- symbol summary (later phase)

### 2) File window state
- file-path
- revision-id
- line-start / line-end
- content lines
- parse/token/fold artifacts for that window

### 3) Thread object state
- thread-id
- question / goal
- nodes: [{id, type, text}]
- edges: [{from, to, relation}]
- evidence: [{node-id, file-path, span, snippet, confidence}]

This turns AI output into a traversable artifact instead of ephemeral text.

---

## MVP path (pragmatic)

### Phase 1: Evidence-linked answers
- Keep current answer panel
- Require structured citations per answer point (`file + span + snippet`)
- Click citation -> open file and jump to span

### Phase 2: Thread panel
- Render answer as ordered thread steps
- Expand a step to show code evidence bundle
- "Follow next lead" actions

### Phase 3: Rama-backed file windows
- Add file index + window retrieval through Rama
- Prefetch around viewport
- Keep UI caches for smooth scrolling and gutter interactions

### Phase 4: Thread persistence and replay
- Persist threads as first-class records
- Re-open old thread and continue from prior context

---

## Decision captured from this session

The vision direction is approved conceptually:
- Build toward a thread-centric code understanding workflow
- Use Rama as memory/projection backbone
- Avoid naive "backend call on every scroll event"
- Prefer reactive window streaming + local caching

This is consistent with the larger Softland goal: navigable, replayable knowledge paths rather than flat chat responses.

---

## Next implementation slice recommendation

Start with **Phase 1 (evidence-linked answers)** before deep windowing work.

Reason:
- immediately validates the thread UX model
- low-risk integration with existing editor and agent path
- creates clear schema contracts needed for later Rama-backed window architecture
