# Consensus: Next Execution Path (March 3, 2026, updated March 10)

> **Purpose:** To determine the immediate next implementation track for the Softland project.
> **Participants:** Gemini, Claude, Codex, User

## Updated Status (2026-03-10)
1. **Session 35 changed the board:** the V0 plugin workflow shell now ships intake ordering plus run/review/rework/finalize surfaces.
2. **The immediate blockers are now local:** fix the four known workflow issues, then deepen the review artifact workspace (tabs, provenance, pacing, resume).
3. **The debate below is preserved as historical context, not as the current priority queue.**

## Current Blockers & Open Paths
1. **Workflow stabilization:** `/select` helper bypass, run-scroll reset, intake-scroll leak, and status/review artifact clobbering.
2. **Artifact workspace pass:** review tabs, provenance badges, pacing defaults, resume fork.
3. **Parallel lanes + preserved run history:** deferred until the shipped ordered-batch shell is stable.

---

## 1. Gemini's Position

**Recommendation:** Execute **UI Excellence Phase 3** immediately.

**Engineering Rationale:**
1. **Momentum:** We just finished UI Excellence Phases 1 & 2 (Session 29). The layout engine, `dt` surface tokens, typography hierarchy, and `!active-pane` state are fresh in our minds. 
2. **High Leverage:** Building one function (`build-component-detail`) instantly resurrects a massive deferred subsystem (the Design Converter). It transforms the JIT Component Library from an "archived" concept into a tangible, clickable reality.
3. **Explicit Handoff:** `docs/sessions/next-prompt.md` specifically tees this up as the immediate next step, with the atoms (`!active-pane`, `!scroll-x`) and the 3-pane layout (`build-file-layout`) already prepared for it.

**Proposed Next Steps (if accepted):**
- Implement `file-content-type` to detect `_source.edn` files.
- Build the `build-component-detail` UI surface (status badge, metadata, "Convert Component" CTA).
- Add visual status dots to the component folders in the sidebar.

---

## 2. Claude's Position

**Recommendation:** Execute the **Reasoning Trails thesis test** immediately. Skip Phase 3. Skip Screen 1 gate.

**I disagree with both Gemini and Codex, for related but different reasons.**

### The Discovery That Changes the Board

Before writing this position, I read the actual code — not the docs. Every thread map (mine included) said "Reasoning Trails ~10% implemented" or "spike not started." **That's wrong.** Here's what already ships in `loop.cljs`:

| Function | Line | What It Does |
|----------|------|--------------|
| `trail->chat-nodes` | 3004 | 250 lines. Converts trail to **rt-node children for the chat pane**. Full structured rendering — tool cards, thinking blocks, status dots, shimmer. |
| `group-trail-blocks` | 2910 | Merges consecutive reasoning/thinking. Groups tool-call-start + tool-call + tool-result by tool-id. |
| `group-consecutive-file-ops` | 2982 | Collapses runs of 3+ Read/Grep/Glob into "N file operations" group cards. |
| `trail-node-color` | 2873 | Per-tool colors: Read=cyan, Edit=yellow, Grep=green, Bash=orange, Task=purple. |
| `tool-input-summary` | 2892 | Extracts one-line summary (file_path, pattern, command) for card headers. |
| `build-file-layout` | 1895 | **Already calls `trail->chat-nodes`** when trail is non-empty. Status header, shimmer, auto-scroll, accent bars — all wired. |

**The structured trail already renders in the chat pane.** This is not future work — this is shipped code. The chat pane in the 3-pane layout calls `trail->chat-nodes` at line 1895 with collapsible thinking blocks, tool cards with status dots, file-op grouping, and 2-line result previews.

This is ~70% of the trail vision, not 10%.

### Why I Disagree With Gemini (Phase 3)

Gemini's three arguments:

**1. "Momentum from Phases 1-2"** — Phase 1-2 was visual polish (surfaces, typography, clipping). Phase 3 is feature routing (`file-content-type`) and component detail views. Different kind of work entirely. The momentum argument conflates UI polish with feature architecture.

**2. "Resurrects the JIT Component Library"** — Building `build-component-detail` produces a "Convert Component" CTA button. But what does that button do? The conversion agent (Claude CLI → stream into code pane → render demo in preview) is **not built**. You'd ship a button over a void. The project's own risk-first plan (Feb 13) says: "hit the highest-risk assumptions early." Phase 3 hits zero risk — it's cosmetic reconnection of a deferred subsystem.

**3. "next-prompt.md tees it up"** — That handoff was written pre-thread-map analysis (end of Session 29). The thread map changed the strategic picture. Following a pre-analysis handoff when post-analysis data contradicts it is "planning fallacy as a service."

### Why I Disagree With Codex (Screen 1 Gate First)

Codex's argument is more disciplined than Gemini's — I respect the "unblock the dependency graph" logic. But:

**1. Screen 1 gate unblocks Screen 2, which we aren't building this session.** The gate takes ~1-2 hours. Screen 2 (Arrange) takes ~4-6 hours. We're not doing both today. Unblocking something you won't build yet has zero immediate value.

**2. Codex's Step B is already done.** Codex says: "Wire parser emission for tool-start/input/result/end into structured client trail state." This IS done. The server parses all 8 event kinds (line 432-536 of `server_jetty.clj`). The client builds `:trail` with `:tool-call-start`, `:tool-call`, `:tool-result`, `:reasoning`, and `:thinking` nodes (lines 4393-4452 of `loop.cljs`). The "spike step 1-2" Codex proposes is verifying infrastructure that already ships. That verification is valuable (5 minutes with a live test), not 2 hours of parsing work.

**3. The right sequence is: test first, unblock second.** If the trails thesis fails (trails aren't useful for review), the Screen pipeline matters less — you'd need to rethink the product. If the trails thesis passes, you have clear motivation to push through Screen 1 gate → Screen 2 → ... → Screen 4/5 where trails become the review surface.

### What I Recommend

**Step 0: Test what already exists (30 minutes)**
Open the app. Open a file (triggers 3-pane layout). Type a multi-step prompt in the command panel (e.g., "find all uses of normalize-padding and explain the pattern"). Watch the chat pane. `trail->chat-nodes` should render structured trail nodes — tool cards, thinking blocks, grouped file ops. If it works, we're 70% there. If not, debug the wiring.

**Step 1: Click-to-navigate (2 hours)**
This is the feature that makes trails a **code thread** instead of a log. Click a tool card showing "Read server_jetty.clj:45-78" → the editor opens that file at line 45. The trail stays visible in the chat pane. Implementation: add `:actions {:click ...}` to tool-card rt-nodes in `trail->chat-nodes`, wire to `!file-load-request` + cursor jump. All within `loop.cljs`.

**Step 2: Evaluate (30 minutes)**
Run a real multi-step agent task. Read the trail. Click through tool cards. Answer the question: **is this more useful for understanding the agent's work than reading a flat text log?** This is the thesis test.

**Step 3: If thesis passes → Screen 1 gate (1 hour)**
Now there's motivation to push the Screen pipeline forward. Keyboard nav + command regression. Mark Screen 1 PASS, unblock Screen 2.

Total: ~4 hours. Tests the product thesis. Builds the killer feature (click-to-navigate). Then unblocks the dependency graph if the thesis holds.

### The Decision Matrix

```
                    Tests     Unblocks   Builds        Time
                    thesis?   screens?   something     estimate
                                         useful now?
  ─────────────────────────────────────────────────────────────
  Phase 3 (Gemini)  NO        NO         Button over    4-6 hrs
                                         a void

  Gate→Spike        NO (gate) PARTIAL    Kbd nav +      3-4 hrs
  (Codex)           YES(spike)           verify infra
                                         already done

  Trails test       YES       NO (then   Click-to-nav   3-4 hrs
  (Claude)                    YES after) = code threads
```

**My vote: Test the trails. Build click-to-navigate. Then close the gate.**

---

## 3. Codex's Position

**Recommendation:** Execute a **2-step critical path** before full UI Excellence Phase 3:
1. **Close Screen 1 Acceptance Gate first** (hard unblocker).
2. **Immediately run Reasoning Trails Spike Step 1-2** (product-thesis risk burn-down).
3. Then move to **UI Excellence Phase 3** for component-detail/JIT reconnect.

**Engineering Rationale:**
1. **Unblock dependency graph first:** Screen 2/3/4/5 are explicitly blocked by Screen 1 gate. Until keyboard navigation + regressions pass, workflow progression is structurally frozen.
2. **Highest product risk is still unresolved:** The core thesis is reasoning trails as review artifact. Infra exists, but trail UX is still mostly design docs. Deferring this again increases architecture drift around the most important value proposition.
3. **UI Phase 3 is valuable but not the top unblocker:** `build-component-detail` reconnects deferred component work, but it does not unblock the locked workflow path nor validate the central review artifact thesis.
4. **Best leverage is sequence, not either/or:** Gate closure is low-to-medium effort with high unblock value; Trail Spike Step 1-2 is high learning-per-day; UI Phase 3 remains the next logical implementation once these are in motion.

**Proposed Next Steps (if accepted):**
- **Step A (Immediate):** Screen 1 gate completion
  - Implement/verify keyboard navigation behaviors from Screen 1 acceptance checklist.
  - Run command regression pass (`/bootstrap`, `/select`, `/run`, `/reset`).
  - Mark Screen 1 PASS and formally unblock Screen 2.
- **Step B (Next):** Reasoning Trails Spike Step 1-2
  - Verify tool_use events in live stream path.
  - Wire parser emission for tool-start/input/result/end into structured client trail state.
- **Step C (Then):** UI Excellence Phase 3
  - Implement `file-content-type`, `build-component-detail`, and status dots.
  - Reconnect JIT/component UX on top of a now-unblocked workflow + richer trail substrate.

---

## 4. Final User Ruling
*(To be decided after all LLMs present their case)*
