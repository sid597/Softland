


# Codex Next Step Notes

Date: February 5, 2026
Scope: Consolidated notes from the last two conversations.

---

## 1) Conversation A Summary: "Enter in command editor should run Claude/Codex/Gemini via subscriptions (not paid APIs)"

### Problem Statement
You want Enter in the browser command editor to submit a task to your already-subscribed CLI tools (`claude`, `codex`, `gemini`) instead of integrating provider APIs directly.

### Key Constraint
A web browser cannot execute local binaries directly.  
So we need a local or remote process bridge that the browser can call.

### Three Solutions Discussed

1. Localhost CLI Bridge (recommended first)
- Browser sends request to local backend endpoint.
- Backend shells out to provider CLI and streams output.
- Best for: immediate value, low complexity, subscription reuse.

2. Stateful Session Broker
- Keep long-lived CLI sessions (PTY/tmux/process pool).
- Enter messages go into existing sessions with persistent context.
- Best for: IDE-like continuity, lower per-request startup overhead.

3. Cloud Relay Runner
- Same bridge model but hosted on your own VM/container.
- Works from anywhere; still CLI-based, still subscription-auth workflows.
- Best for: multi-device access; higher operational/security burden.

### Best Approach for This Codebase
Start with **Solution 1**, then evolve into a **hybrid of 1 + 2**:
- Step 1: local request/response + streaming
- Step 2: optional persistent sessions per provider/project

Reason:
- You already have a command-submit hook and backend routing pattern in place.
- It minimizes time-to-first-use without closing the door on stateful sessions.

### Existing Integration Anchors (already in repo)
- Command Enter handling:
  - `src/app/client/webgpu/loop.cljs` (command panel Enter case around current submit path)
- Server route middleware:
  - `src/app/server_jetty.clj` (`/api/*` route handling pattern exists)
- Existing event-driven LLM/Rama pattern:
  - `src/app/server/rama/util_fns.cljc` (`send-llm-request`)
  - `src/app/server/rama/core.clj` (`:llm-request` action handling in ETL)

### Recommended Execution Architecture
Use the three pillars in their strongest role:

- Electric: control plane + reactive transport
  - Submit command intent
  - Stream status/output updates to client

- Rama: durable orchestration + replayable events
  - Persist request lifecycle
  - Record chunks, completion, errors, cancellation
  - Enable multi-user and time-travel/debugging later

- WebGPU: interaction/render plane
  - Render transcript/timeline/logs
  - Keep heavy process logic out of render loop

### Why This Is Better Than "just add one endpoint"
- It solves immediate CLI integration.
- It also builds the data model needed for your larger meta vision (history, provenance, replay, async collaboration).

---

## 2) Conversation B Summary: "Read README super-meta and say where/how we go from here"

### README North Star (interpreted)
From `README.md`, the project is aiming for:
- "Google Earth for knowledge"
- Multi-scale navigation (60k ft to 30k ft and deeper)
- Time travel through knowledge evolution
- Papers as executable/reconstructable logs, not static text
- AI as malleable collaborator (not linear chat)
- Asynchronous interaction and non-disruptive workflows
- A system that supports deep sense-making and contribution

### Core Meta Insight
The product is not "an editor with LLM integration."  
The product is a **knowledge operating system** with:
- structured intent
- asynchronous agent collaboration
- durable epistemic memory
- navigable causal/history views

### Current State vs Vision

You already have:
- High-performance interactive editor runtime (WebGPU)
- Reactive orchestration (Missionary + Electric patterns)
- Sidebar/file operations and command panel primitives
- Existing Rama event/depot foundation in codebase

Missing for the meta vision:
- Structured intent model (beyond raw command text)
- Durable event model for agent interactions
- Provenance/causality model for claims/results
- Time-travel UI on top of stored event history

### Strategic Direction (from here)
Go from:
- "command text -> immediate response"

To:
- "intent -> orchestrated async execution -> durable events -> replayable/navigable knowledge state"

This is the shortest path that serves both:
- immediate CLI utility
- long-term "super meta" product direction

---

## 3) Unified Recommendation (Best Approach to Both the Immediate and Meta Problem)

Build a **CLI-first Agent Runtime** as an **event-sourced subsystem**:

1. Command Panel emits structured intents
- `:task/run`, `:ask`, `:summarize`, `:trace-origin`, `:compare`, `:simulate`
- Include provider (`:claude|:codex|:gemini`), workspace path, metadata

2. Backend starts provider CLI runs
- Local bridge first
- Stream stdout/stderr chunks
- Add timeout and cancel controls

3. Rama stores lifecycle events
- `:agent-requested`
- `:agent-started`
- `:agent-chunk`
- `:agent-completed`
- `:agent-failed`
- `:agent-cancelled`

4. Electric streams state into UI
- Real-time transcript updates
- Status transitions
- Retry and cancellation UX

5. WebGPU renders durable interaction objects
- Transcript view
- timeline / replay
- later: claim-evidence graph overlays

This gives you immediate working behavior and preserves every interaction as reusable knowledge material.

---

## 4) Practical Implementation Plan

### Phase 0: Thin Vertical Slice (fastest)
- Add `/api/agent/submit` and `/api/agent/stream/:id`.
- On Enter, submit command from command panel.
- Stream output back to a simple transcript panel.
- Start with one provider toggle, then all three.

Outcome:
- "Press Enter and run via subscribed CLI" works end-to-end.

### Phase 1: Productionize Local Runner
- Add provider abstraction and command builder.
- Validate command whitelist and workspace root constraints.
- Add per-run timeout and kill endpoint.
- Normalize streamed chunks into common event format.

Outcome:
- Stable, safer local execution.

### Phase 2: Rama-backed Event Source
- Persist run lifecycle in Rama.
- Move stream fanout to event subscribers.
- Enable replay/reload continuity.

Outcome:
- Durable and debuggable orchestration.

### Phase 3: Async Collaboration UX
- Non-blocking "ask now, read later" interaction.
- Notifications/status badges.
- Save and re-open runs with context.

Outcome:
- Matches README async collaborator behavior.

### Phase 4: Knowledge-Time-Travel Layer
- Convert transcripts into structured claims/events.
- Add diff/provenance timelines.
- Build multi-scale navigation affordances.

Outcome:
- Begins implementing the "Google Earth for knowledge" vision.

---

## 5) Guardrails (Critical)

- Keep CLI process handling out of WebGPU render loop.
- Avoid adding expensive recompute paths tied to blink/RAF cycles.
- Use buffered/throttled UI append for stream chunks.
- Store transcript as chunk/line vectors, not one giant string.
- Keep provider-specific details isolated behind one interface.

---

## 6) Concrete Next Coding Move

If implementing now, the first step should be:

1. Hook Enter submit in command panel into backend request dispatch.
- Start at `src/app/client/webgpu/loop.cljs` command Enter path.

2. Add new `/api/agent/*` routes in `src/app/server_jetty.clj`.

3. Create a small server namespace for CLI execution + chunk streaming.

4. Render basic transcript panel using existing text ops pipeline.

This creates a working end-to-end slice while keeping architecture aligned with the meta direction.

---

## 7) Short Decision Record

Decision:
- Use CLI-first bridge (subscription-backed) rather than direct provider APIs for now.

Rationale:
- Cost model and product preference
- Fastest integration path
- Works with existing architecture

Long-term architecture decision:
- Event-sourced orchestration (Rama) + reactive transport (Electric) + high-performance interaction surface (WebGPU).

Rationale:
- Only path that satisfies both immediate utility and README-level meta goals.
