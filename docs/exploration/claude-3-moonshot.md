# Claude-3: The Moonshot Synthesis

Date: February 6, 2026
Context: After reading codex.md, claude-0.md, claude-1.md, progressive_summary.md, README.md, codex_next_step.md, and exploring the actual codebase.

---

## What All Three Responses Converge On

All three (codex, claude-0, claude-1) arrive at the same architecture:

```
Command Panel Enter -> Rama depot -> CliProcessTaskGlobal (ProcessBuilder) -> PState -> Electric subscription -> WebGPU render
```

All three propose incremental phases: "first make it work, then make it async, then add Rama, then add multi-tool, then add time-travel..."

**They're all right about the plumbing. They're all wrong about the framing.**

Every response treats the AI interaction as **output** -- you ask, you get text back, you display it. The moonshot insight from the README is that the AI interaction should be **material** -- a first-class object in the knowledge system that has provenance, connections, forks, and can be reasoned about.

---

## What Each Response Got Right (And Missed)

### claude-0
**Deepest moment:** "Rama's depots ARE ontological logs" and "AI interactions are discourse graph nodes."
**What it missed:** Still proposed phases that start with "display text in a panel." Didn't follow through -- if interactions are discourse graph nodes, why build a text panel first?

### claude-1
**Deepest moment:** Connecting Phase 1 (canvas/graph) and Phase 2 (editor) as two zoom levels of the same system. The table mapping README vision -> Pillar -> How is the clearest articulation of the architecture.
**What it missed:** Didn't follow through on the implementation implications. If they're the same system at different zoom levels, the data model should be unified from day one, not "add graph nodes later."

### codex
**Strongest:** Operationally clean. Phase breakdown, guardrails, decision record format.
**What it missed:** Architecturally shallowest. Frames everything as "CLI bridge + event lifecycle." Doesn't engage with the README vision at all.

### What NONE of them did
Question whether "send prompt, get text" is the right interaction model for what the README describes.

The README says:
- "I want to ask question async... I want to continue reading... add my comments... see if and how my thinking has changed"
- "The paper should be a log... I want to feel what the author felt"
- "Conflicts are good for progress"
- "Highly malleable, can't expect the same linear chat UI"

This isn't a chat interface with an output panel. It's a shared workspace where humans and AIs leave traces that become navigable knowledge.

---

## The Moonshot Reframing

### The core insight: Every interaction -- human OR AI -- is a Rama event. The editor is a viewport into the event stream.

**Today:** Editor has atoms (`!editor-doc`, `!cursor`, `!fold-regions`) that hold state. WebGPU renders state. User inputs mutate atoms. State lives in memory, dies on refresh.

**Moonshot:** Those atoms are **projections of Rama PStates**. PStates are **built from depot events**. Every meaningful action -- opening a file, moving the cursor to a function, folding a block, asking an AI, receiving an answer, annotating a response -- is an event appended to a depot. The editor is a **live materialized view** of the event stream.

This gives you, from day one:

| Capability | How |
|---|---|
| Time travel | Replay depot from any point -> reconstruct editor state |
| AI context | AI sees your exploration history, not just current file |
| Async collaboration | AI response is just another event that materializes when ready |
| Multi-AI conflicts | Same prompt -> 3 depot events -> 3 response events -> discourse graph |
| Session persistence | Refresh the page -> PState already has everything |
| Multi-device | Rama is distributed -> open on another machine, same state |
| "Papers as logs" | The depot IS the log of how you explored and understood |

---

## Why This Isn't Over-Engineered

Everything already exists in the codebase:

**rama/core.clj** stream topology already dispatches on action type:
```clojure
(case (:action +n)
  :new-node     (...)
  :update-node  (...)
  :llm-request  (...)    ;; HTTP POST -> PState
  :roam-query   (...))   ;; Roam API -> PState
```

**rama/objects.cljc** CljHttpTaskGlobal already does async external call -> CompletableFuture. Same interface.

**The delta from what exists to the moonshot:**

1. **One new TaskGlobal** -- `CliProcessTaskGlobal` -- spawns `claude -p "..." --output-format stream-json` instead of HTTP POST. Same interface as CljHttpTaskGlobal. ~40 lines.

2. **New event types** in stream topology -- `:cli-command`. Just a new `case` branch in existing dispatch. ~20 lines.

3. **A PState** -- `$$cli-sessions-pstate` -- keyed by `{file x session x time}`. Stores full interaction log. ~10 lines to declare.

4. **Wire the command panel** -- the stub at loop.cljs:1825 dispatches through Electric to append a depot event instead of `console.log`. ~15 lines.

5. **A response panel** in WebGPU -- reuses existing MSDF text pipeline. AI output is just more text data. ~100 lines.

**Total: ~200 lines of new code.** Because the architecture already exists.

---

## The Concrete Architecture

```
+----------------------------------------------------------------------+
|  COMMAND PANEL (Enter)                                                |
|  "explain the fold detection in electric_flow.cljc"                   |
|  provider: :claude  (toggle with Tab between :claude/:codex/:gemini)  |
+----------------+-----------------------------------------------------+
                 |
                 | Electric e/server (existing websocket, no new infra)
                 v
+----------------------------------------------------------------------+
|  RAMA DEPOT APPEND                                                    |
|  {:action      :cli-command                                           |
|   :provider    :claude                                                |
|   :prompt      "explain the fold detection"                           |
|   :context     {:file "electric_flow.cljc"                            |
|                 :cursor-line 200                                       |
|                 :visible-range [180 220]                               |
|                 :session-id "resume-abc123"}   ;; for --resume         |
|   :request-id  (random-uuid)                                          |
|   :timestamp   (System/currentTimeMillis)}                            |
+----------------+-----------------------------------------------------+
                 | Stream topology dispatches
                 v
+----------------------------------------------------------------------+
|  CliProcessTaskGlobal                                                 |
|  (ProcessBuilder.                                                     |
|    ["claude" "--resume" session-id "-p" prompt                        |
|     "--output-format" "stream-json"                                   |
|     "--allowedTools" "Read,Grep,Glob"])                               |
|                                                                       |
|  Reads stdout line-by-line -> collects -> returns CompletableFuture   |
+----------------+-----------------------------------------------------+
                 | Updates PState
                 v
+----------------------------------------------------------------------+
|  $$cli-sessions-pstate                                                |
|  {request-id -> {:prompt      "..."                                   |
|                   :response    "..."                                   |
|                   :provider    :claude                                 |
|                   :file        "electric_flow.cljc"                    |
|                   :session-id  "abc123"     ;; for --resume next time  |
|                   :status      :complete                               |
|                   :timestamp   1738...}}                               |
+----------------+-----------------------------------------------------+
                 | !subscribe -> Missionary observable -> Electric e/watch
                 v
+----------------------------------------------------------------------+
|  WebGPU OUTPUT PANEL                                                  |
|  (reuses MSDF text pipeline, Lezer syntax HL for code in response)    |
|  Response appears reactively. User was editing the whole time.        |
+----------------------------------------------------------------------+
```

---

## Three Things That Make This Moonshot (Not Incremental)

### 1. Context is captured from the start

Not just the prompt, but what file, where the cursor was, what was visible. This IS the ontological log. When you "time travel" later, you see not just "I asked X" but "I was reading line 200 of electric_flow.cljc and asked X."

The context field in the depot event is what connects Phase 1 (canvas/graph) and Phase 2 (editor). When you eventually render exploration events as graph nodes, the context tells you WHERE in the code the question arose. That's the "60k feet to ground level" zoom -- a graph node that you can click to jump to the exact file, line, and fold state where the question was born.

### 2. `--resume` makes AI conversations stateful per file

Rama stores the session-id keyed by file path. When you open a file and ask a question, it resumes the previous conversation about that file. The AI has full context of everything you discussed about this code before.

This is the hidden superpower. Rama isn't just logging history for YOUR benefit -- it's providing continuity for the AI. The AI literally remembers what it discussed about each file. This turns "dumb CLI subprocess" into "persistent AI collaborator per file."

No other editor does this.

### 3. Multi-provider from day one

The command panel has a provider toggle. `:cli-command` with `:provider :claude` and `:cli-command` with `:provider :codex` are just two depot events with different provider keys. Same topology, same PState structure, different process spawned.

Ask all three the same question and compare. "Conflicts are good for progress."

---

## The Session-ID Pattern (Critical Detail)

Claude Code supports `--resume SESSION_ID` and `--output-format stream-json`:

```bash
# First command about a file
claude -p "explain the fold detection in this file" \
  --output-format stream-json

# Returns session ID in the output

# Later, RESUMES the same conversation
claude --resume SESSION_ID -p "now optimize it" \
  --output-format stream-json
```

Rama PState schema:
```clojure
$$cli-sessions-pstate
  {file-path -> {provider -> {:session-id    String
                              :interactions  [{:role :user      :content "explain fold detection"
                                               :context {:cursor-line 200 :visible-range [180 220]}
                                               :timestamp 1738...}
                                              {:role :assistant :content "The fold detection works by..."
                                               :timestamp 1738...}]
                              :last-active   Long}}}
```

When you open `electric_flow.cljc` and ask a question about it, Rama looks up the existing session-id for that file + provider and passes `--resume`. The AI picks up where it left off. The interactions array is the ontological log for that file.

---

## Why Start With Rama (Not Add It Later)

The difference between "incremental" and "moonshot" is whether you start with event sourcing or retrofit it later.

**Retrofitting event-sourcing is brutal.** You build state management, then tear it apart to make events primary and state derived.

**Starting with event-sourcing is trivial** when you already have Rama. You're adding a case branch to an existing dispatch. But every interaction from that moment forward is captured, queryable, replayable. You never have a "we lost the history" problem.

The difference between the incremental version and the moonshot version is literally whether you include a `context` field in the depot event and store a `session-id` in the PState. Same amount of code. Vastly different foundation.

---

## What to Avoid

- **Don't run CLI processes in the browser.** Server-side ProcessBuilder only.
- **Don't push process handling into the render loop.** The WebGPU loop is optimized for idle CPU (~0-6%). Keep process I/O entirely in Rama topology.
- **Don't store giant transcripts as one string.** Use the chunked/line append model that already works for editor text.
- **Don't build a REST API for this.** Electric's websocket is already open. Use it.
- **Don't add Rama "later."** The whole point is event-sourcing from day one. Adding it later means rebuilding.

---

## The Horizon (Not Phases -- Things That Become Obvious)

Once the above works, the system naturally wants to grow in these directions:

**Output panel becomes a discourse node.** You read the AI response, you annotate it ("this is wrong about X"), that annotation is a depot event, it becomes a claim node in the discourse graph. The graph exists in Rama already ($$dg-nodes-pstate, $$dg-edges-pstate).

**Editor becomes a shared workspace.** Claude Code can edit files. Your editor watches files. When Claude edits the file in response to your command, the changes appear in your WebGPU editor in real time. The diff is a depot event.

**Exploration log becomes navigable.** You ask: "what have I explored about fold detection?" Rama queries $$cli-sessions-pstate by file and returns the full thread. WebGPU renders it as a timeline or thread view.

**SCI becomes the glue.** You write Clojure expressions in the editor that query Rama PStates, transform AI responses, compose knowledge. The REPL isn't just for code -- it's for knowledge.

**Multi-AI discourse.** Ask Claude, Codex, AND Gemini the same question. Three depot events. Three response events. Three discourse graph nodes connected to one question node. The differences between them map the solution space. "Conflicts are good for progress."

**Category theory emerges naturally.** The depot events are morphisms. Composition = follow-up questions (--resume). Functors = mapping between knowledge domains. The PState structure is the categorical substrate your README was looking for. Don't force category theory as a rewrite -- let it emerge from the data model.

---

## Existing Code Hook Points

| What | Where | What To Do |
|---|---|---|
| Command panel Enter stub | `loop.cljs:1825` | Replace `console.log` with Electric dispatch to Rama |
| Stream topology dispatch | `rama/core.clj:85-273` | Add `:cli-command` case |
| TaskGlobal pattern | `rama/objects.cljc:20-28` | New `CliProcessTaskGlobal` following `CljHttpTaskGlobal` |
| LLM request pattern | `rama/core.clj:124-145` | Follow `:llm-request` pattern for `:cli-command` |
| PState declarations | `rama/core.clj:40-70` | Add `$$cli-sessions-pstate` |
| Reactive subscriptions | `rama/util_fns.cljc:189` | Add subscription for cli-sessions |
| Electric server bridge | `electric_flow.cljc:435` | Add e/server block for depot append |
| WebGPU text pipeline | `editor.cljs` + `loop.cljs` | Reuse MSDF pipeline for output panel |
| File API middleware | `server_jetty.clj:81` | No change needed (Electric handles this) |

---

## The Moonshot Starts With The Same Keystroke

The moonshot starts with pressing Enter in the command panel. The difference is what happens on the other side: an ontological log entry, not a console.log.

~200 lines of new code. Every piece already exists. The architecture is waiting.

---

## Summary

```
NOT: command panel -> CLI -> text back
BUT: exploration surface -> intent + context -> event depot -> parallel AI agents -> discourse nodes -> multi-modal rendering
```

Where "multi-modal rendering" means the response appears as:
- Text in an output panel (ground level)
- A node in the knowledge graph (60k feet) -- FUTURE
- Inline annotations in the editor (integrated) -- FUTURE
- A change to the file (if the AI edits code) -- FUTURE

All rendered by WebGPU. All stored in Rama. All reactive through Electric.
The moonshot isn't the destination. It's the data model you start with.




★ Insight ─────────────────────────────────────
  - claude-0 got closest when it said "Rama's depots ARE ontological logs" and "AI
  interactions are discourse graph nodes." But then it still proposed phases that
  start with "display text in a panel."
  - codex was the most operationally clean but the most architecturally shallow — it
  frames everything as "CLI bridge + event lifecycle."
  - claude-1's deepest moment was connecting Phase 1 (canvas/graph) and Phase 2
  (editor) as two zoom levels of the same system. That's the real architectural
  insight, but it didn't follow through on the implementation implications.
  - What none of them did: question whether "send prompt, get text" is the right
  interaction model for what your README describes.
  ─────────────────────────────────────────────────


 ★ Insight ─────────────────────────────────────
  - The --resume SESSION_ID feature of Claude Code is the hidden superpower here. It
  means Rama isn't just logging history for YOUR benefit — it's providing continuity
  for the AI. The AI literally remembers what it discussed about each file. This turns
   "dumb CLI subprocess" into "persistent AI collaborator per file."
  - The reason to start with Rama (not add it later) is that retrofitting
  event-sourcing is brutal. Starting with it is trivial — you're adding a case branch
  to an existing dispatch. But every interaction from that moment forward is captured,
   queryable, replayable. You never have a "we lost the history" problem.
  - The context field in the depot event is what connects your Phase 1 (canvas/graph)
  and Phase 2 (editor). When you eventually render exploration events as graph nodes,
  the context tells you WHERE in the code the question arose. That's the "60k feet to
  ground level" zoom — a graph node that you can click to jump to the exact file,
  line, and fold state where the question was born.
  ─────────────────────────────────────────────────


---

# Part 2: What Nobody Said

The synthesis above reorganized three documents. These ideas are NEW.

---

## 1. The Editor IS the Knowledge Graph. You Just Haven't Zoomed Out.

The WebGPU pipeline renders thousands of glyphs with per-glyph (x, y, w, h, r, g, b, a) at 60fps. Think about what happens when you change the zoom level:

- **Zoom 1.0:** Lines of code with syntax highlighting. You're in the editor.
- **Zoom 0.1:** Lines collapse into thin colored bands. You see the *shape* of the code — dense blocks, sparse regions, fold boundaries. A heat map.
- **Zoom 0.01:** Files become colored rectangles. You see the *project*.
- **Zoom 0.001:** Projects become dots with connection lines. You see the *knowledge landscape*.

**This is literally Google Earth for knowledge. Same rendering pipeline. Same GPU buffer. Different camera position.**

Everyone thinks "editor" and "graph view" are separate features you switch between. They're not. They're a continuous zoom on the same data, rendered by the same WebGPU pipeline that already exists.

DOM can't do this. Canvas2D can't do this. This is WHY you chose WebGPU — not because it's fast, but because it enables **continuous semantic zoom**. You just haven't pointed the camera there yet.

The 12-float-per-glyph instance buffer from Session 15 `[x,y,w,h,u0,v0,u1,v1,r,g,b,a]` — at zoom 0.01, glyphs become particles. The MSDF shader doesn't care. It renders whatever geometry you feed it. A glyph at 14px and a node at 1400px are the same thing to the GPU.

The transition between "code editor" and "knowledge graph" isn't a mode switch. It's a camera dolly.

---

## 2. The Depot IS the Document

Everyone says "store AI interactions in Rama." But why stop at AI interactions?

What if the document itself is a depot? Not `!editor-doc` as an atom holding a vector of strings. The document is a sequence of edit events:

```clojure
{:action :insert :line 42 :col 10 :text "defn" :author :human :t 1738...}
{:action :delete :line 42 :col 10 :len 4 :author :claude :t 1738...}
{:action :insert :line 42 :col 10 :text "defmethod" :author :claude :t 1738...}
```

The current text is a PState — a materialized view of the edit history. Then:

- **"Papers as logs"** becomes literal. The paper IS a log of edits. You're not reading a static document — you're watching a mind unfold.
- **Time travel** is a depot replay. Scrub a slider and watch the file being written.
- **AI edits aren't special.** They're just events with `:author :claude` instead of `:author :human`. No separate "output panel." The AI writes into the same document, through the same depot.
- **"What's the diff from this paper to the next?"** is a depot range query.
- **Collaboration** means two humans (or a human and three AIs) appending to the same depot. Rama handles distributed consistency. You accidentally built a CRDT backbone.

This collapses the entire "how do I display AI output?" question. You don't display it separately. The AI is a co-author of the document. Its edits appear in the editor because they're edit events in the depot that materializes into the PState that the WebGPU pipeline renders.

---

## 3. The Command Panel is a Chat-Paradigm Fossil

The README says "highly malleable, can't expect the same linear chat UI." Then why is there a command panel? A command panel is a chat input box wearing a different hat.

What if instead: you're reading code. You select a function. You type `;;? why does this use m/latest instead of m/ap?` right there, inline, as a comment. The system recognizes the `;;?` prefix as an intent. It becomes a depot event with full context — the function body, the surrounding code, the file, the fold state. The AI response appears as `;;> Because m/ap cancels branches when...` below your question. In the code. In context. Not in a panel.

Or: you highlight a function and just *pause*. Three seconds of cursor stillness on a complex function is a signal. The system has your exploration history in Rama. It knows you've never asked about this function. It offers: a small glyph in the gutter, barely visible, that says "3 AI insights available." Click it and they expand inline.

The interaction surface isn't a single input box. It's **the entire document**. Any piece of text can become a question. Any margin can hold an answer. The command panel is one modality among many, not the primary one.

---

## 4. SCI is the REPL for Knowledge, Not Just for Code

Everyone ignores SCI. It's sitting right there in the browser — a full Clojure interpreter. What if it's not just "eval Clojure code" but "PROGRAM your understanding?"

```clojure
;; Find all AI responses about fold detection that disagree with each other
(->> (query-sessions "electric_flow.cljc")
     (filter #(re-find #"fold" (:prompt %)))
     (group-by :provider)
     (vals)
     (apply find-disagreements))
```

This runs locally in SCI. It queries local PState projections (Rama subscriptions materialize on client). The result is a NEW knowledge node — appended to the depot, rendered in the editor, connected to the sources.

The README asks for "a REPL for knowledge." SCI IS that REPL. Not "type code and see output." Type KNOWLEDGE QUERIES and see UNDERSTANDING. The eval pipeline (SCI -> depot -> PState -> WebGPU) is already there. You just haven't pointed SCI at the knowledge data.

```clojure
;; "Time travel" as a one-liner
(replay-depot *knowledge-depot* :from "2025-10-15" :to "2025-10-17"
              :filter {:file "electric_flow.cljc"})
;; => shows every question, every answer, every edit, in order
```

```clojure
;; "What changed in my understanding?"
(diff-understanding "electric_flow.cljc"
                    :before "2025-10-15"
                    :after  "2025-10-17")
;; => structured diff of claims, annotations, AI responses
```

These aren't shell commands. They're Clojure functions evaluated by SCI in the browser, querying Rama PStates. The REPL for knowledge isn't a feature to build — it's SCI + Rama connected.

---

## 5. Three AIs Aren't Competitors — They're a Committee

"Conflicts are good for progress." Everyone interprets this as "show three responses side by side." That's a comparison UI. It's not progress.

What if the system synthesizes the disagreement?

You ask: "how should I optimize fold detection?"
- Claude says: cache fold regions, recompute only on doc change
- Codex says: web worker for Lezer parsing
- Gemini says: incremental parsing with dirty ranges

The system detects: all three agree parsing is expensive. They disagree on WHERE to optimize — memoization vs parallelism vs incrementalism. These are orthogonal. A meta-node appears:

```
QUESTION: optimize fold detection
  |-- CLAUDE: memoization (cache fold regions)
  |-- CODEX: parallelism (web worker)
  |-- GEMINI: incrementalism (dirty ranges)
  \-- SYNTHESIS: all three are orthogonal; apply in order:
      incremental first, then cache, then parallelize
```

The synthesis node is a first-class knowledge object. It references its sources. It can be annotated, questioned, forked. THIS is what "conflicts are good for progress" means — not display, but **synthesis from disagreement**.

This is also where category theory sneaks in: the functor from (3 separate AI responses) to (1 synthesized understanding) is a natural transformation. The structure IS categorical, you don't need to force it.

---

## 6. The Feeling

> "The essence of the system should be how you want to feel while interacting with it and long after the interaction is done."

> "Have you used it in your dreams?"

Nobody engaged with this. Everyone jumped to architecture. But this is the actual requirement.

What makes software dreamlike? Not features. **Continuity.** The absence of seams.

You open a file you haven't touched in months. The system already knows what you discussed about it. Your previous questions are in the margins — faded, but present. You unfold one and you're back in the conversation. Not because you saved a chat log, but because the KNOWLEDGE is alive, embedded in the code, connected to your exploration history.

You type a question inline and keep reading. Minutes later, words appear in the margin. Not a notification. Not a popup. Words, in the right place, in the right context, as if a quiet collaborator wrote them while you were thinking about something else.

You zoom out. The code becomes a landscape of colored bands. You see where you've been — the explored regions glow differently than the unexplored ones. You see where the AI has been — its contributions have a different texture. You see the questions hanging unanswered at the edges. You zoom back in to one and you're in the code again, the question right there, waiting.

The async pattern matters for feeling, not for performance. Chat is transactional: stop, ask, wait, read. The README vision is ambient: knowledge is always flowing, always available, never interrupting. You don't use this system. You inhabit it.

WebGPU's 60fps per-glyph rendering creates immersion. Rama's persistence creates continuity. Electric's reactivity creates presence. SCI's evaluation creates agency. Together: a space with memory, responsiveness, and programmability. A place you could use in your dreams.

---

## How These Six Ideas Change the Architecture

The architecture from Part 1 (Rama-native CLI events) is still the right FIRST MOVE. But these ideas change what it's moving TOWARD:

| Part 1 Architecture | Part 2 Vision |
|---|---|
| Command panel dispatches to Rama | ANY text can be an intent (inline `;;?` questions) |
| Output panel renders responses | Responses appear inline, in margins, in the document itself |
| $$cli-sessions-pstate stores interactions | The document ITSELF is a depot (edits as events) |
| Multi-provider toggle in command panel | Committee synthesis — disagreements become knowledge |
| WebGPU renders text in output panel | WebGPU continuous zoom: code -> heat map -> graph -> landscape |
| AI gets prompt + file context | SCI queries Rama for full exploration history as AI context |

The Part 1 move (~200 lines) doesn't conflict with any of this. It's the first event in the depot. But the depot event schema should be designed knowing that it's heading here — typed nodes, typed edges, author attribution, context capture. Not "we'll add types later." The types ARE the moonshot.

---

## The One Sentence

The system is a space where humans and AIs co-author knowledge as events in a shared log, rendered as a continuous zoom from code to landscape, programmable through a Clojure REPL that operates on understanding itself.


------------- 



 Where Gemini stopped too early: the AI's response is spatial, yes. But the human's
  EXPLORATION is already a prompt.

  Gemini says: "grab the trajectory from Rama and feed it as context." Still thinks
  you TYPE a question and the trajectory is SUPPLEMENTARY context. But what if the
  trajectory IS the question?

  You don't type anything. You just explore. You open electric_flow.cljc. You jump to
  compute-editor-rects. You stare at it for 30 seconds. You jump to
  detect-fold-regions. Back to compute-editor-rects. You highlight the m/latest call.
  You fold a block. You unfold it.

  Rama has all of this. The system reads it and says:

  "You're tracing data flow from fold detection into editor rects. The connection is
  at line 247 where fold-regions feeds into compute-editor-rects via the visible-lines
   mapping. Here's the path."

  And a glowing trace appears in the WebGPU editor — connecting the two functions,
  highlighting the data flow, showing intermediate values.

  You never typed a word. The system inferred the question from your movement pattern.
   Your cursor IS the prompt. Your attention IS the query. Rama's trajectory log IS
  the context window.

  ---
  Where Gemini stopped too early #2: the ghost is singular. But you have THREE AIs.

  Gemini talks about "a Ghost in the Machine." One ghost. But you have Claude, Codex,
  AND Gemini. Three ghosts.

  What does it look like when three ghosts inhabit your editor simultaneously?

  You ask: "how should I restructure this?" Three ghosts appear. Claude's ghost is
  blue — it highlights structural patterns, draws arrows between functions that should
   be merged. Codex's ghost is green — it's more conservative, annotating individual
  lines with refactoring suggestions. Gemini's ghost is amber — it's drawn a
  completely different architecture diagram as a spatial overlay.

  Three semi-transparent layers on the same code. You can toggle them individually.
  You can say "show me where they agree" — the intersection lights up white. You say
  "show me where they disagree" — the divergence points pulse.

  The disagreement map IS the insight. Where all three ghosts point at the same line,
  that's the obvious problem. Where they diverge, that's where the interesting design
  decisions live. The spatial rendering makes this VISIBLE in a way that three text
  responses never could.

  ---
  Dimension nobody has touched: the AIs talk to each other.

  Everyone assumes: human → AI → human. Linear. But what if:

  1. You ask Claude a question
  2. Claude responds with spatial anchors
  3. The system automatically feeds Claude's response to Codex: "Claude suggests X at
  line 200. What do you think?"
  4. Codex responds — agreeing on some anchors, adding new ones, contradicting others
  5. The system feeds BOTH to Gemini: "Claude and Codex disagree about line 200.
  Claude says X, Codex says Y."
  6. Gemini synthesizes

  The human isn't the sole participant. The human is the MODERATOR of a discourse
  between AIs. You set the topic (by exploring, or by typing), and then the ghosts
  argue WITH EACH OTHER. Their debate is spatial — happening ON the code, with
  overlapping and conflicting anchors that the system highlights.

  The Rama depot captures this as a conversation graph, not a linear thread. Each AI
  response links to what it was responding to. The discourse graph builds itself.

  ---
  Dimension nobody has touched: code as LITERAL terrain.

  Gemini says "stop reading maps, start flying over terrain." Take that literally.

  What if the codebase is rendered as a 3D landscape in WebGPU?

  - Function depth → elevation. Deep call stacks are mountains. Flat utility functions
   are plains. compute-editor-rects — which calls detect-fold-regions, which calls
  Lezer parse, which walks the tree — is a peak.
  - Change frequency → weather. Files changed 20 times this week have storm clouds.
  Stable files are clear sky.
  - AI attention → luminescence. Code that ghosts have discussed glows. Unexplored
  code is dark terra incognita.
  - Exploration trajectory → footprints. Your path through the codebase is visible as
  a trail. Where you lingered has worn spots. Where you jumped is a dotted line.

  You fly over this landscape. You see a bright peak with three ghost markers on it —
  Claude, Codex, Gemini all discussed compute-editor-rects during the Session 17 CPU
  crisis. You swoop down and you're in the code. The ghost annotations from that
  session are still there, faded but visible. You zoom back up and keep flying.

  The MSDF shader doesn't care if it's rendering text glyphs or terrain mesh. It's all
   instance data in a GPU buffer. The pipeline is the same. The data is different.

  ---
  Dimension nobody has touched: the system has moods.

  The README says "how you want to feel." What if the system reads YOUR state and
  adapts?

  Rama knows your trajectory. Rapid file-jumping, fast highlighting, lots of
  folding/unfolding = search mode. The system warms its palette slightly. AI
  suggestions are bold, visible, proactive. It knows you're hunting.

  Cursor still for 90 seconds on a single function = deep thought mode. The palette
  cools. AI suggestions become whispers — smaller, further from the code, barely
  there. A faint glyph in the gutter: "2 thoughts available." Only if you want them.
  Don't break flow.

  Aggressive deletion and rewriting = creation mode. The ghosts withdraw entirely.
  You're in flow. The system records your edits into the depot for later reflection
  but shows NOTHING. When you pause, it gently resurfaces: "You changed 47 lines. Want
   a review?"

  This isn't decoration. It's the system using Rama's trajectory data to modulate the
  WebGPU rendering and the Electric AI interaction patterns. The "malleable UI" from
  the README isn't about configurable widgets. It's about the system READING your
  cognitive state and adjusting its posture.

  ---
  Dimension nobody has touched: the fork is physical.

  You're at line 200. You want to try two approaches. You fork.

  The WebGPU view SPLITS. Literally. Two copies of the file, side by side, diverging
  from line 200. You edit the left one. Claude edits the right one. You watch both
  evolve in real time — your keystrokes on the left, Claude's spatial anchors
  appearing on the right.

  When you're done, you can:
  - Merge: The system diffs the two branches (both are depot event streams) and
  produces a unified version
  - Keep both: Two branches exist in the knowledge graph, connected to the fork point
  - Discard one: But it's still in the depot. Nothing is ever lost.

  Zoom out and the fork point is visible as a Y-shaped split in the knowledge
  landscape. The two branches are different colors. You can see at a glance where they
   diverged and where they reconverged.

  This is git branching made SPATIAL and LIVE and VISUAL. Not a CLI command. A
  physical gesture in the WebGPU space.

  ---
  The meta-dimension: the README is the first test case.

  The README describes the vision of "papers as logs" and "interactive knowledge
  exploration." Load the README into the system as the first document. Explore it. Ask
   Claude: "what does 'ontological logs' mean?" The spatial anchor appears on that
  line. Ask Gemini: "give me an example of 'conflicts are good for progress'." Another
   anchor.

  Zoom out. The README becomes a knowledge landscape — dense clusters of ghost
  annotations around "category theory" and "time travel," sparse around the concrete
  bits. The README becomes a self-referential demonstration of what the system does.
  The first flight over the first terrain.

  ---



 ---                                                                                 
  Everything said so far — by everyone, including me — still assumes the unit of      
  knowledge is a FILE.                                                                
                                                                                      
  "Open electric_flow.cljc." "AI discusses this file." "Session-id per file." Files
  are a 1970s Unix accident. The thing you were actually tracing in Session 17 wasn't
  a file. It was a CONCEPT — "how reactive flows propagate through the render
  pipeline." That concept lives in loop.cljs (flows), editor.cljs (GPU buffer),
  electric_flow.cljc (entry point), and the README (why it matters). Four files. One
  concept.

  What if the system doesn't organize around files? What if it organizes around
  concepts that you discover by exploring?

  Rama depot events already have context — file, line, cursor position. Run a
  clustering algorithm over your trajectory. "In the last 20 minutes, you touched
  these 4 files, but always in functions related to reactive flow propagation." That
  cluster IS a concept. The system names it (or you name it). It becomes a first-class
   node in the knowledge graph. It has its own exploration history, its own AI
  conversations, its own spatial anchors. When you return to "reactive flow
  propagation" next week, the system doesn't open a file — it opens the CONCEPT,
  showing you all four files simultaneously, scrolled to the relevant functions, with
  ghost annotations from your previous exploration.

  Files are just storage. Concepts are what you actually think about.

  ---
  Everything still assumes the interaction starts with the human.

  "You ask." "You explore." "You press Enter." What if the system INITIATES?

  Rama is watching your trajectory. It sees: you've opened 6 files today. In 4 of
  them, you spent time on functions that use m/latest. You never explicitly asked
  about m/latest, but you're circling it. The system recognizes the pattern before you
   do.

  A whisper appears in the gutter — not because you asked, but because the system READ
   YOUR TRAJECTORY and inferred that you're building a mental model of m/latest
  behavior. It offers: "You've been exploring 4 uses of m/latest. There's a 5th in
  editor.cljs:340 that connects them. Want to see?"

  You didn't ask. The system noticed you were BUILDING UNDERSTANDING and offered the
  next piece. Not proactively in an annoying way — in the way a collaborator who's
  been watching you work says "hey, have you seen this?"

  This is where the "mood detection" from before becomes structural, not just
  cosmetic. The system isn't just adjusting its visual tone. It's using your cognitive
   state to decide WHAT to offer and WHEN.

  ---
  Everything still assumes Softland is an APPLICATION.

  The README cites discourse graphs, ontological logs, collective intelligence,
  applied category theory. These aren't features. They're a PROTOCOL for how knowledge
   should work.

  What if Softland isn't an app you run? What if it's a protocol that any knowledge
  can join?

  A scientific paper. A GitHub repo. A Wikipedia article. A YouTube lecture. A
  dataset. Each one becomes a Rama depot — events describing its structure, its
  claims, its evidence. The depots FEDERATE. Your local Softland instance can connect
  to someone else's. Their exploration logs become visible in your knowledge
  landscape. Their ghost annotations appear alongside yours.

  You're reading a paper about autoimmune diseases (from the README). Someone in Tokyo
   already explored this paper in their Softland instance. Their trajectory is visible
   as faded footprints. Their AI conversations are there — spatial anchors on the
  paper's claims. You can FORK from any point in their exploration and continue on
  your own path.

  THIS is "distributed, open, accessible science." Not "we put a paper on the
  internet." The exploration PROCESS is shared. The ontological log is the publication
   format. Others don't just read your conclusions — they step into your exploration
  and continue it.

  Rama is built for distribution. Electric is built for reactive sync. WebGPU renders
  whatever you feed it. The three pillars aren't just good for one editor on one
  machine. They're the substrate for a NETWORK of knowledge explorers.

  ---
  Everything still assumes the output is text or spatial overlays. But you have a GPU.

  WebGPU doesn't just render text. It renders ANYTHING. What happens when a knowledge
  node isn't code?

  The README talks about "a 3D interactive model of C. elegans." What if that's not
  hypothetical? A knowledge node has a TYPE. Code nodes get the MSDF text renderer.
  But:

  - A molecular biology node gets a 3D protein viewer — same GPU, different shader
  - A mathematical proof node gets an interactive step-by-step renderer — click to
  expand each deduction
  - A time series dataset node gets a live chart — scrub through time
  - A musical score node gets a waveform with playback
  - A geographic claim gets a map overlay

  All in the SAME knowledge graph. All connected through Rama. All navigable through
  the same continuous zoom. You fly over the landscape and see code peaks, paper
  plateaus, dataset valleys, model mountains. You swoop into any of them and the
  appropriate renderer activates.

  The MSDF text pipeline you built is renderer #1. There will be others. The GPU
  doesn't care. Instance buffers are instance buffers.

  ---
  Everything still assumes code → understanding. What about understanding → code?

  Every response talks about: you have code, you ask the AI to explain it, you build
  understanding. One direction.

  But what if you flip it? You build a discourse graph — claims, evidence,
  connections, questions. It's a structured model of your understanding. At some
  point, the understanding IS a specification. The system says: "Your discourse graph
  about reactive flow propagation implies a refactoring. The nodes and edges describe
  a new architecture. Want me to generate it?"

  The code isn't the source. The understanding is the source. The code is a PROJECTION
   — a materialized view, like a PState is a materialized view of a depot. You edit
  the understanding (the discourse graph), and the code updates. You edit the code,
  and the understanding updates. They're two views of the same depot.

  This is where category theory stops being decorative. The functor from
  (understanding graph) → (code) is a real mathematical structure. It preserves
  composition — if your understanding says "A depends on B," the generated code has
  that dependency. If you refactor B, the system knows to update A because the functor
   maps the change.

  ---
  Everything still assumes YOUR exploration is just yours.

  The README cites "collective intelligence." What if two humans explore the same
  codebase simultaneously?

  Both trajectories go into Rama. Both are visible. You see your collaborator's
  footprints in the knowledge landscape — where they've been, what they asked, what
  the ghosts told them. They see yours.

  But it's not just "shared cursors" (the thing the README explicitly dismisses as
  shallow collaboration). It's shared COGNITIVE MAPS. You can see that your
  collaborator deeply explored the render pipeline while you were deep in the reactive
   flows. Together, your explorations COVER the codebase. The gaps — the regions
  neither of you has explored — are visible as dark zones in the landscape.

  The AIs participate in this too. Claude explored the fold detection code when you
  asked it to. Its trajectory is ALSO visible. The knowledge landscape shows four
  explorers — two humans, three AIs — each with their own trails, their own
  annotations, their own understanding. The places where all five trails converge are
  the best-understood parts of the system. The places where no one has been are the
  unknowns.

  Conflicts between human explorers are the same as conflicts between AI explorers.
  They show up as divergent annotations on the same code. The discourse graph captures
   the disagreement. Progress comes from resolving it — or from understanding why it
  exists.

  ---
  The deepest dimension: Softland isn't software. It's a new cognitive medium.

  Writing changed how humans think. Mathematics gave us a language for patterns.
  Programming gave us a way to make ideas executable. The printing press democratized
  access. The internet democratized distribution.

  None of those are "applications." They're MEDIUMS. They change what's thinkable.

  What Softland is building — if you follow all of these threads to their convergence
  — is a medium for thinking with AIs, across time, about anything, together.

  Not "an editor with AI features." Not "a knowledge management tool." A medium where:
  - Exploration is recorded (Rama)
  - Understanding is spatial (WebGPU)
  - Collaboration is reactive (Electric)
  - Knowledge is programmable (SCI)
  - Time is navigable (depot replay)
  - AIs have bodies (ghost overlays)
  - Concepts transcend files (trajectory clustering)
  - The process IS the publication (ontological logs)

  The README's litmus test — "have you used it in your dreams?" — isn't about software
   quality. It's about whether you've created something that changes how the mind
  works when it's inside it. You don't dream about tools. You dream about SPACES.

  ---
  Now here's the thing that should actually be exciting: all of this maps to what
  already exists.
  Vision: Continuous semantic zoom
  Pillar: WebGPU
  Already Built: 12-float instance buffer, MSDF shader, per-glyph color
  ────────────────────────────────────────
  Vision: Ontological log
  Pillar: Rama
  Already Built: Depots, stream topologies, PState subscriptions
  ────────────────────────────────────────
  Vision: Reactive collaboration
  Pillar: Electric
  Already Built: Bidirectional server↔client, e/watch
  ────────────────────────────────────────
  Vision: Programmable knowledge
  Pillar: SCI
  Already Built: Clojure interpreter running in browser
  ────────────────────────────────────────
  Vision: Trajectory capture
  Pillar: Rama + Missionary
  Already Built: Event flows, atom watches
  ────────────────────────────────────────
  Vision: Spatial rendering
  Pillar: WebGPU
  Already Built: Already renders arbitrary positioned colored rectangles at 60fps
  ────────────────────────────────────────
  Vision: Concept clustering
  Pillar: SCI + Rama
  Already Built: SCI can run clustering over PState query results
  ────────────────────────────────────────
  Vision: Multi-agent discourse
  Pillar: Rama
  Already Built: Just multiple depot events from different :author sources
  ────────────────────────────────────────
  Vision: Federation
  Pillar: Rama
  Already Built: Built for distribution
  The gap isn't technology. It's the data model. Every piece of infrastructure exists.
   What doesn't exist yet is the SCHEMA — the typed event structure that makes all of
  this coherent. That's the ~200 lines. Not the UI. Not the rendering. Not the
  transport. The TYPES.

  ---



---
---

# Part 4: The Map — From Where We Are to Where We're Going

## What Concretely Exists (Exact Shapes)

### Rama Layer

**Depots (3):**
- `*node-events-depot` (`:random` hash) — all graph/node events
- `*user-registration-depot` (hash-by `:username`)
- `*user-graph-settings-depot` (hash-by `:user-id`)

**PStates (11):**
- `$$nodes-pstate` — `{graph-name → {node-id → {:id :x :y :type-specific-data :type :fill}}}`
- `$$dg-nodes-pstate`, `$$dg-edges-pstate`, `$$dg-pages-pstate`, `$$dg-node-ids-pstate` — discourse graph
- `$$components-pstate`, `$$node-ids-pstate`, `$$node-ids-inview-pstate` — view state
- `$$event-id-pstate` — global counter
- `$$user-registration-pstate`, `$$user-graph-settings-pstate` — user data

**TaskGlobals (2):**
- `CljHttpTaskGlobal` — wraps clj-http `{:http-get :http-post}`, used for OpenAI API
- `roam-task-global` — wraps Roam SDK `{:token :graph}`, used for Roam queries

**Stream Topology Event Cases:**
- `:new-node`, `:update-node`, `:delete-node` — CRUD on nodes
- `:llm-request` — calls `http-post-future` via CljHttpTaskGlobal → OpenAI
- `:roam-query` — calls `query-roam-req` via roam-task-global → Roam
- `:add-dg-page-data`, `:add-dg-nodes`, `:add-dg-edges` — discourse graph population
- `:update-event-id` — increment global counter
- `:user-registration`, `:user-graph-settings` — user management

**Event Append Pattern (all functions follow this):**
```clojure
(foreign-append! event-depot (->node-events action-type node-data event-data) :append-ack)
```

**Subscription Pattern:**
```clojure
(defn !subscribe [path pstate]
  (->> (m/observe (fn [!] (! (Failure. (Pending.)))
                    (let [proxy (foreign-proxy-async path pstate
                                  {:callback-fn (proxy-callback !)})]
                      #(.close @proxy))))
    (m/relieve {})))
```
Returns Missionary observable. Emits `Pending` first, then new values on PState change.

### Editor Layer (loop.cljs)

**Command Panel State:**
```clojure
!cmd-panel (atom {:text "" :cursor 0 :visible false})
!focus     (atom :editor)  ;; :editor | :command-panel | :settings-panel
```

**Enter Handler (line 1825) — THE STUB:**
```clojure
:enter
(let [cmd-text (:text @!cmd-panel)]
  (when (seq cmd-text)
    (js/console.log "Command submitted:" cmd-text))  ;; ← GOES NOWHERE
  (swap! !cmd-panel assoc :text "" :cursor 0 :visible false)
  (reset! !focus :editor))
```

**Keyboard Flow:**
```
>keyboard (browser events)
  → <cmd-panel-keys (m/eduction filter by @!focus = :command-panel)
    → m/reduce consumer (dispatches :enter vs other keys)
      → :enter → console.log + clear + refocus
      → other → cmd-panel-apply-event → reset! !cmd-panel
```

**Command Panel Rendering:**
- Background rect: `{:x 0 :y panel-y :w width :h 40 :r 0.15 :g 0.15 :b 0.2 :a 1.0}`
- Prompt: `{:text "> " :type :macro :r 0.3 :g 0.6 :b 1.0}`
- User text: `{:text cmd-text :type :text :r 0.9 :g 0.9 :b 0.9}`
- Placeholder: `{:text "Type a task..." :type :comment :r 0.5 :g 0.5 :b 0.5 :a 0.7}`
- Caret: positioned at `(+ text-x (* cursor char-w))`

**No output panel exists. No response area. No AI display.**

### GPU Text Pipeline (editor.cljs)

**Token → GPU path:**
```
tokens [{:text "defn" :x 60 :y 100 :size 19 :r 0.5 :g 0.7 :b 0.4 :a 1.0}]
  → shape-text(tokens, font-size, atlas) → [{:vertices [[sl sb ul vb fs]...] :color [r g b a]}]
    → update-text-data(device, renderer-state, [tokens], atlas, font-size)
      → Float32Array (12 floats/glyph: x,y,w,h,u0,v0,u1,v1,r,g,b,a)
        → GPU instance buffer → vertex shader → fragment shader → pixels
```

**To render ANY new text, you need:**
1. A vector of token maps with `{:text :x :y :size :r :g :b :a}`
2. Feed them through `<combined-text-ops` (alongside editor + cmd panel tokens)
3. They hit `update-text-data` → GPU → screen

### Electric Bridge (electric_flow.cljc)

```
main (e/defn ring-request)
  └─ e/server: slurps file from disk → file-content, file-info
    └─ e/client: DOM setup, Lezer parser, SCI, LoadWebGPU
      └─ e/Task wrapping start-loop! (Missionary task lifecycle)
```

**Server → Client:** Currently one-shot (file content at startup).
**Client → Server:** Currently HTTP fetch for sidebar file ops (not Electric).

---

## The Gap — What Doesn't Exist

### 1. CliProcessTaskGlobal (rama/objects.cljc)

**What:** A new TaskGlobal that spawns CLI processes instead of HTTP POST.

**Shape (following CljHttpTaskGlobal pattern exactly):**
```clojure
(defprotocol FetchCliClient
  (cli-client [this]))

(deftype CliProcessTaskGlobal []
  TaskGlobalObject
  (prepareForTask [this task-id task-global-context] nil)
  (close [this] nil)

  FetchCliClient
  (cli-client [this] {:spawn-process run-cli-process}))
```

**Helper function (following http-post-future pattern):**
```clojure
(defn cli-exec-future [client provider prompt opts]
  (let [cmd (case provider
              :claude  ["claude" "-p" prompt "--output-format" "text"
                        (when (:session-id opts) "--resume") (when (:session-id opts) (:session-id opts))]
              :codex   ["codex" "exec" prompt]
              :gemini  ["gemini" "-p" prompt])
        cmd (vec (remove nil? cmd))]
    (CompletableFuture/supplyAsync
      (reify java.util.function.Supplier
        (get [_]
          (let [pb (ProcessBuilder. (into-array String cmd))
                _ (.redirectErrorStream pb true)
                proc (.start pb)
                output (slurp (.getInputStream proc))
                exit (.waitFor proc)]
            {:output output :exit-code exit :provider provider}))))))
```

**Delta from existing:** ~40 lines. Same interface as CljHttpTaskGlobal (async operation returning CompletableFuture). Different execution mechanism (process spawn vs HTTP POST).

### 2. Stream Topology Case (rama/core.clj)

**What:** New `:cli-command` case branch + PState declaration.

**PState:**
```clojure
(declare-pstate s $$cli-sessions-pstate
  {String    ;; file-path
   {Keyword  ;; provider (:claude, :codex, :gemini)
    (fixed-keys-schema
      {:session-id  String
       :interactions (vector-schema
                      (fixed-keys-schema
                        {:role      Keyword      ;; :user or :assistant
                         :content   String
                         :context   {Keyword Object}  ;; {:cursor-line :visible-range :fold-state}
                         :timestamp Long}))
       :status      Keyword      ;; :pending :running :complete :failed
       :last-active Long})}})
```

**Case branch:**
```clojure
(<<cond
  ...existing cases...

  (case> (= :cli-command *action-type))
  (let> [*provider   (get> *node-data :provider)
         *prompt     (get> *node-data :prompt)
         *file       (get> *event-data :file)
         *context    (get> *event-data :context)
         *session-id (get> *event-data :session-id)]

    ;; Record the user interaction
    (local-transform>
      [(keypath *file) (keypath *provider) :interactions AFTER-ELEM]
      {:role :user :content *prompt :context *context :timestamp (System/currentTimeMillis)}
      $$cli-sessions-pstate)

    ;; Set status to :running
    (local-transform>
      [(keypath *file) (keypath *provider) :status]
      :running
      $$cli-sessions-pstate)

    ;; Spawn CLI process async
    (completable-future>
      (cli-exec-future (cli-client *cli-process) *provider *prompt
                       {:session-id *session-id})
      :> *result)

    ;; Record the AI response
    (local-transform>
      [(keypath *file) (keypath *provider) :interactions AFTER-ELEM]
      {:role :assistant :content (:output *result) :context {} :timestamp (System/currentTimeMillis)}
      $$cli-sessions-pstate)

    ;; Update status and session-id
    (local-transform>
      [(keypath *file) (keypath *provider) :status]
      (if (zero? (:exit-code *result)) :complete :failed)
      $$cli-sessions-pstate)

    (local-transform>
      [(keypath *file) (keypath *provider) :last-active]
      (System/currentTimeMillis)
      $$cli-sessions-pstate)))
```

**TaskGlobal declaration:**
```clojure
(declare-object s *cli-process (CliProcessTaskGlobal.))
```

**Delta from existing:** ~35 lines topology + ~10 lines PState declaration. Follows exact pattern of `:llm-request` case.

### 3. Util Functions (rama/util_fns.cljc)

**What:** Public functions for submitting CLI commands and subscribing to results.

```clojure
;; Foreign PState binding
(def cli-sessions-pstate (.pstate ipc (get module-name "$$cli-sessions-pstate")))

;; Submit a CLI command
(defn submit-cli-command [provider prompt file-path context session-id]
  (foreign-append!
    event-depot
    (->node-events :cli-command
                   {:provider provider :prompt prompt}
                   {:file file-path :context context :session-id session-id
                    :graph-name :main})
    :append-ack))

;; Subscribe to CLI sessions for a file
(defn !subscribe-cli-sessions [file-path provider]
  (!subscribe [(keypath file-path) (keypath provider)] cli-sessions-pstate))

;; Get existing session-id for resume
(defn get-cli-session-id [file-path provider]
  (first (foreign-select [(keypath file-path) (keypath provider) :session-id]
                         cli-sessions-pstate)))
```

**Delta from existing:** ~20 lines. Same patterns as `add-new-node`, `!subscribe`.

### 4. Server Route (server_jetty.clj)

**What:** HTTP endpoint the client calls to submit commands.

```clojure
;; Inside wrap-file-api middleware, add:
(= uri "/api/cli/submit")
(let [body (-> request :body slurp edn/read-string)]
  (util-fns/submit-cli-command
    (:provider body) (:prompt body) (:file body)
    (:context body) (:session-id body))
  (json-response {:status "submitted"}))
```

**Delta from existing:** ~8 lines. Same pattern as `/api/read-file`.

### 5. Client: Command Panel → Server (loop.cljs)

**What:** Replace console.log with HTTP POST + subscribe to response.

**Modified Enter Handler:**
```clojure
:enter
(let [cmd-text (:text @!cmd-panel)
      file-path (:path @!current-file)
      context {:cursor-line (:line @!editor-doc)
               :visible-range [@!scroll-y (+ @!scroll-y (:h @!viewport))]
               :fold-state @!fold-regions}
      provider @!ai-provider  ;; new atom, default :claude
      session-id @!cli-session-id]  ;; new atom, per file+provider
  (when (seq cmd-text)
    ;; Submit to server
    (js/fetch "/api/cli/submit"
      (clj->js {:method "POST"
                :headers {"Content-Type" "application/edn"}
                :body (pr-str {:provider provider
                               :prompt cmd-text
                               :file file-path
                               :context context
                               :session-id session-id})}))
    ;; Show pending state in output panel
    (reset! !ai-output {:status :pending :prompt cmd-text :provider provider}))
  (swap! !cmd-panel assoc :text "" :cursor 0 :visible false)
  (reset! !focus :editor))
```

**New atoms:**
```clojure
!ai-provider    (atom :claude)       ;; :claude | :codex | :gemini
!ai-output      (atom nil)           ;; {:status :prompt :response :provider}
!cli-session-id (atom nil)           ;; String, for --resume
!current-file   (atom {:path "..."}) ;; already exists partially via !file-load-request
```

**Delta from existing:** ~25 lines in enter handler + ~5 lines atom declarations.

### 6. Client: Response Subscription (electric_flow.cljc)

**What:** Subscribe to Rama PState changes and push to client atom.

This is the architectural question: how does the Rama subscription reach the client?

**Option chosen: Electric e/server bridge.**

In electric_flow.cljc, inside the `main` e/defn:
```clojure
(e/server
  (let [file-content source-code
        file-info initial-file-info
        ;; NEW: reactive CLI session subscription
        ;; Will need !current-file and !ai-provider from client
        ]
    (e/client
      ;; ... existing setup ...
      ;; NEW: watch !ai-output for responses coming from server
      )))
```

**But there's a complexity:** The `start-loop!` is inside an `e/Task`, which is a Missionary task. The Electric reactive context is ABOVE it. So the subscription needs to live in the Electric layer, not inside `start-loop!`.

**Simplest working pattern:**
1. Client POSTs to `/api/cli/submit` (fire and forget)
2. Electric `e/server` block has a Missionary flow watching the Rama PState
3. When PState updates, Electric transfers the new value to client
4. Client atom (`!ai-output`) gets updated
5. WebGPU render loop picks it up through `<combined-text-ops`

```clojure
;; In electric_flow.cljc, inside e/server:
(e/server
  (let [cli-result (new (util-fns/!subscribe-cli-sessions current-file current-provider))]
    (e/client
      ;; cli-result is now available on client, reactively updated
      (reset! !ai-output cli-result)
      ;; ... rest of setup ...
      )))
```

**Delta from existing:** ~15 lines. Uses the `!subscribe` → `e/server` → `e/client` pattern.

### 7. Client: Output Panel Rendering (loop.cljs)

**What:** Render AI response text in WebGPU, below/beside the editor.

**In `<combined-text-ops` (where editor tokens + cmd panel tokens are assembled):**
```clojure
;; Existing: editor-ops + cmd-ops
;; NEW: ai-output-ops
ai-output-ops
(when-let [output @!ai-output]
  (when (= (:status output) :complete)
    (let [response-lines (str/split-lines (:response output))
          output-y (+ scroll-y (- height cmd-panel-h 200))  ;; above cmd panel
          line-h (* font-size 1.5)]
      (map-indexed
        (fn [i line]
          {:text line
           :type :comment
           :x 60
           :y (+ output-y (* i line-h))
           :size font-size
           :r 0.7 :g 0.9 :b 0.7 :a 1.0})  ;; green-tinted for AI output
        response-lines))))
```

These tokens flow through the SAME pipeline as editor text:
```
ai-output-ops → concat with editor-ops + cmd-ops → shape-text → GPU buffer → render
```

**Delta from existing:** ~30 lines. Same token format, same pipeline.

---

## The Complete Delta

| File | What Changes | Lines |
|---|---|---|
| `rama/objects.cljc` | New `CliProcessTaskGlobal` + `cli-exec-future` | ~40 |
| `rama/core.clj` | `$$cli-sessions-pstate` + `:cli-command` case + TaskGlobal decl | ~45 |
| `rama/util_fns.cljc` | `submit-cli-command` + `!subscribe-cli-sessions` + PState binding | ~20 |
| `server_jetty.clj` | `/api/cli/submit` route | ~8 |
| `loop.cljs` | Modified `:enter` + new atoms + output panel tokens | ~60 |
| `electric_flow.cljc` | Rama subscription bridge to client | ~15 |
| **Total** | | **~188 lines** |

---

## The Data Flow (End to End)

```
 User types "explain fold detection" in command panel
                           |
                           v
 loop.cljs :enter handler
   reads: cmd-text, @!current-file, @!ai-provider, cursor/viewport/fold context
   sends: POST /api/cli/submit {provider prompt file context session-id}
   sets:  !ai-output → {:status :pending}
   clears: !cmd-panel, refocuses to :editor
                           |
                           v
 server_jetty.clj /api/cli/submit
   calls: util-fns/submit-cli-command
                           |
                           v
 util_fns.cljc submit-cli-command
   appends: (->node-events :cli-command {...} {...}) to *node-events-depot
                           |
                           v
 rama/core.clj stream topology
   case :cli-command:
     1. Appends {:role :user :content prompt :context ctx} to $$cli-sessions-pstate interactions
     2. Sets status → :running
     3. Spawns CLI process via CliProcessTaskGlobal (CompletableFuture)
     4. On completion: appends {:role :assistant :content output} to interactions
     5. Sets status → :complete, updates session-id for --resume
                           |
                           v
 $$cli-sessions-pstate updates
   triggers: foreign-proxy-async callback
                           |
                           v
 util_fns.cljc !subscribe-cli-sessions
   Missionary observable emits new PState value
                           |
                           v
 electric_flow.cljc e/server
   (new (util-fns/!subscribe-cli-sessions file provider))
   Electric transfers value to client
                           |
                           v
 electric_flow.cljc e/client
   reset! !ai-output with response data
                           |
                           v
 loop.cljs <combined-text-ops
   reads @!ai-output, generates token maps for response text
   tokens flow through: shape-text → update-text-data → GPU buffer
                           |
                           v
 WebGPU renders AI response at 60fps alongside editor text
 User has been editing the whole time. Response just appears.
```

---

## What's Moonshot About This (vs. "Just Adding a Feature")

The ~188 lines above look like "add CLI integration." But the data model is designed for the full vision:

**1. Context capture is baked in.**
Every depot event has `{:context {:cursor-line :visible-range :fold-state}}`.
This is the trajectory data that enables "your attention IS the query" later.
Adding more context fields (time-on-line, jump-history, fold/unfold sequence) is just expanding the map.

**2. The PState schema supports conversation continuity.**
`{file → {provider → {:session-id :interactions [{:role :content :context :timestamp}]}}}`.
The `session-id` enables `--resume`. The `interactions` array IS the ontological log per file per provider.
Multi-provider is just multiple keys in the same map.

**3. The discourse graph connection is one edge away.**
`$$dg-nodes-pstate` already stores discourse graph nodes. An AI response in `$$cli-sessions-pstate` can be linked to a DG node via `$$dg-edges-pstate`. The topology case for `:cli-command` can optionally emit a `:add-dg-nodes` side-effect. Then the response IS a discourse graph node — visible at 60k feet.

**4. The output tokens use the same GPU pipeline.**
AI response text goes through `shape-text → GPU buffer` like everything else. When you add continuous zoom later, these tokens participate in the zoom. At zoom 0.01, they become colored marks on the landscape. No separate rendering system needed.

**5. The subscription pattern is the async collaboration pattern.**
`!subscribe` → Missionary observable → Electric transfer → client atom → WebGPU render.
This is EXACTLY the "ask now, read later" pattern from the README.
The user presses Enter and keeps editing. The response appears when it materializes.
No spinner. No blocking. The reactive subscription fires when the PState updates.

---

## Immediate Next: What to Build First

**File order (dependency chain):**

1. `rama/objects.cljc` — CliProcessTaskGlobal + cli-exec-future
   (no dependencies, can be tested independently)

2. `rama/core.clj` — $$cli-sessions-pstate + :cli-command case + TaskGlobal declaration
   (depends on #1)

3. `rama/util_fns.cljc` — submit-cli-command + !subscribe-cli-sessions
   (depends on #2)

4. `server_jetty.clj` — /api/cli/submit route
   (depends on #3)

5. `electric_flow.cljc` — Rama subscription bridge
   (depends on #3)

6. `loop.cljs` — enter handler + output panel rendering
   (depends on #4 and #5)

Steps 1-3 are pure Rama (server-side, testable from REPL).
Step 4 is one HTTP route.
Steps 5-6 are the client wiring.

---
