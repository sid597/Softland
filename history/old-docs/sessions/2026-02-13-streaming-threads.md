# Session Log: 2026-02-13

## Part 1: Streaming Agent Output (completed)

### What was done
Implemented real-time token-level streaming from Claude CLI to the WebGPU agent output panel.

**Key discoveries:**
- `--output-format stream-json` alone gives 3 coarse events (system, assistant, result) — NOT token streaming
- `--include-partial-messages` flag unlocks real `content_block_delta` events with token chunks
- Events wrapped in `{"type":"stream_event","event":{...}}` — text at `event.delta.text`
- `ring.util.io/piped-input-stream` does NOT stream — Jetty never flushes between reads from InputStream
- **Fix**: `ring.core.protocols/StreamableResponseBody` gives direct OutputStream access with real flushing

**Files changed:**
- `objects.cljc`: `provider-default-argv` now accepts `:output-format` and `:include-partials?` kwargs
- `server_jetty.clj`: Added `stream-cli-process`, `parse-stream-json-line`, `write-event!`, `run-agent-stream` + `/api/agent/stream` route using `StreamableResponseBody`
- `loop.cljs`: Added `stream-agent-run!` (fetch + ReadableStream + SSE buffering), modified `submit-agent-run!` for streaming with auto-scroll

---

## Part 2: The Code Threads Vision

### The Problem
When you explore a codebase, you're always pulling threads: "how does X work?" leads to file A line 50, which calls something in file B line 200, which depends on file C line 30. The understanding IS the thread — the connected path through code.

Current AI interactions return **text about code**. What we want is **code arranged as a narrative**.

### The Metaphor
Crossing a valley. A bad guide says "go north, then east, then cross the stream." A great guide WALKS WITH YOU, pointing at each stepping stone: "step HERE, then HERE, then HERE." Each stone is visible, clickable, in context.

### What it looks like

1. **Start**: Blank slate. User asks: "how does Softland's reactive rendering work?"

2. **AI reads files**, comes back with a structured response — NOT just text, but text interleaved with **anchored code snippets**:
   ```
   [explanation] "The reactive loop starts in start-loop! which creates atoms and wires flows..."
   [snippet] loop.cljs:1430-1445 — the atom declarations
   [explanation] "These atoms feed into derived flows via m/latest..."
   [snippet] loop.cljs:688-710 — <cmd-panel-rects showing m/latest composition
   [explanation] "The flows produce render ops consumed by the GPU..."
   [snippet] editor.cljs:280-310 — update-text-data writing to GPU buffer
   [explanation] "The render loop only runs when state changes (dirty checking)..."
   [snippet] loop.cljs:2380-2400 — identical? check on world snapshot
   ```

3. **The UI renders this as a thread** — a vertical path of code snippets connected by explanation text. Each snippet is syntax-highlighted, shows the file name, and is clickable.

4. **Clicking a snippet** opens that file in the editor, scrolled to that exact location. The snippet is highlighted. You see it in full context.

5. **Drilling deeper**: You see `m/latest` in a snippet. You click it (or ask "what does m/latest do here?"). A sub-thread appears — more snippets, more connections, deeper into the rabbit hole.

6. **The thread IS the understanding.** You can save it, share it, fork it. It's not a chat log — it's a MAP of the codebase seen through the lens of your question.

### Key technical shape

**AI output format**: Claude returns structured output (JSON/EDN) with interleaved text and code references:
```edn
{:thread
 [{:type :explanation :text "The reactive loop starts in start-loop!..."}
  {:type :snippet :file "loop.cljs" :start-line 1430 :end-line 1445
   :highlight-lines [1432 1435]
   :annotation "These 6 atoms are the ONLY mutable state"}
  {:type :explanation :text "These atoms feed into derived flows..."}
  {:type :snippet :file "loop.cljs" :start-line 688 :end-line 710
   :annotation "m/latest combines all atom watches into rect geometry"}
  ...]}
```

**Server side**: The AI agent reads files (it already can — Claude CLI has Read/Grep/Glob tools). The structured response is stored in Rama as a thread event:
```
depot event: {:action :thread-response
              :question "how does reactive rendering work?"
              :thread [{snippet} {explanation} {snippet} ...]
              :context {:project "softland" :timestamp ...}}
```

**Client side**: New rendering mode — "thread view." Instead of a single file in the editor, render a vertical sequence of:
- Explanation text (wrapped, styled differently from code)
- Code snippets (syntax-highlighted via Lezer, file label, line numbers)
- Connection lines between snippets (visual thread)

**Clicking a snippet**: Opens that file in the editor, scrolls to that line range, highlights the relevant lines. The thread view stays open (split? overlay? side panel?).

### What this connects to

- **Rama**: Threads are depot events. They persist. You can revisit "how does rendering work?" months later.
- **--resume**: Follow-up questions drill deeper into the same thread. The AI has context.
- **Discourse graph**: Each thread node is a potential discourse graph node. The thread IS a subgraph.
- **Multi-AI**: Ask Claude AND Codex the same question. Two threads through the same codebase. Compare the paths they chose.
- **Continuous zoom**: At code level, you see snippets. Zoom out, snippets become colored marks on files. Zoom further, files become nodes with thread-edges between them. The thread IS the graph.

### The three use cases mentioned

1. **Understanding**: "how does X work?" → thread through the code showing the data flow
2. **Implementation**: "add feature Y" → thread showing the entry point, the files to modify, the integration points
3. **Review**: "review this change" → thread showing the diff in context, what it connects to, what might break

All three are the same shape: a question → a guided path through code → anchored snippets with explanations.

### Open questions (from initial brainstorm — most resolved by Part 3)
- How does the AI produce structured snippet references? → **RESOLVED**: tool_use events in CLI stream ARE the structured references
- Thread view layout: replace editor? Split view? Overlay panel? → **Deferred**: use existing agent panel for v0
- How to handle snippets from files not yet loaded? → **Deferred**: Layer 2, click-to-navigate loads on demand
- Incremental thread building (stream snippets as they're discovered vs. all-at-once)? → **RESOLVED**: streaming gives incremental by default

---

## Part 3: The Agent-as-Developer Reframe

### The key insight
The original "code threads" vision had a hard unsolved problem: how do you get the AI to produce structured snippet references? System prompt? Post-processing? Hope it formats correctly?

**Reframe**: The developer is the Claude agent. The user is the reviewer. The agent's tool_use stream (Read, Grep, Edit calls) IS the exploration trail. No special prompting needed — the work session IS the thread.

### The fractal pattern
The developer→reviewer relationship is fractal. Same shape at every org level:

```
agent        ──trail──▶ developer (review code decisions)
developer    ──trail──▶ tech lead (review approach/tradeoffs)
tech lead    ──trail──▶ PM (review impact/timeline/risk)
```

At every handoff, reasoning context is discarded and only the output survives. The trail fixes this — same data structure, different zoom levels. The trail compresses, it doesn't disappear.

### What changes architecturally
The data source for threads is NOT a specially-prompted AI response. It's the raw streaming output from `--output-format stream-json --include-partial-messages`:
- `content_block_start` with `type: "tool_use"` → agent looked at something
- `input_json_delta` → what file/path/pattern it targeted
- `content_block_delta` with `type: "text_delta"` → agent's reasoning
- The trail reconstructs itself from the stream

### Concrete workflow mapping (DiscourseGraphs example)
1. Pick up Linear issue → agent gets context
2. Agent explores codebase → trail captures every Read/Grep/Glob
3. Agent makes changes → trail captures every Edit with old/new
4. User reviews trail → navigable, not just a diff
5. Tech lead reviews summarized trail → key decisions, tradeoffs
6. PM sees high-level → impact, timeline, risk

For v0: only steps 2-4. Steps 1, 5, 6 are future layers.

---

## Part 4: Implementation Plan — Spike-then-Layer

### The hypothesis
> An agent's tool_use stream, interleaved with reasoning text, presented as a structured trail, makes code review meaningfully better than reading a diff or a chat log.

### Risk assessment (ordered by kill potential)

**Risk 1 (HIGHEST): Does the CLI stream contain tool_use events?**
- `parse-stream-json-line` at server_jetty.clj:349 explicitly skips `content_block_start/stop`
- This means we've SEEN these events — we just throw them away
- Verify: run CLI with tool_use task, grep raw output for `tool_use`
- If fails: fallback to custom API agent loop (viable, more work)

**Risk 2 (HIGH): Is the trail actually more useful than flat text?**
- Risk of "verbose log with headers" instead of navigable map
- Verify: render basic trail, make UX judgment call
- If fails: need filtering/summarization before trail is compelling

**Risk 3 (MEDIUM): Tool input JSON reconstruction**
- `input_json_delta` events are partial JSON chunks that need accumulation + parse
- Well-defined problem, no ambiguity
- If fails: accumulate on server instead of client

**Risk 4 (LOW): WebGPU trail rendering**
- Already render colored text, background rects, click targets in agent panel
- Trail is just a vertical list of styled text blocks
- No new rendering primitives needed for v0

### Decision tree

```
Step 1: CLI stream has tool_use events?
├── NO → Custom API agent loop (1 session detour) → re-evaluate
└── YES ↓

Step 2: Basic trail rendering feels useful?
├── NO → Trail too noisy → try filtering/collapsing → re-evaluate
└── YES ↓

Step 3: Click-to-navigate works?
├── NO → Simplify to text-only file:line refs → continue
└── YES ↓

Step 4: Rama persistence works for trails?
├── NO → Schema issues → adjust → continue
└── YES → Core vision proven → Layer 4 (summarization)
```

Each step is a gate. Fail at any gate → stop, evaluate, adjust.

### The Spike (Session 1, ~3-4 hours)

**Goal**: Prove data pipeline works, get first look at trail UX.

**Step 1: Verify stream data (30 min)**
```bash
claude -p "read loop.cljs and tell me what start-loop! does" \
  --output-format stream-json --include-partial-messages \
  | tee /tmp/stream-raw.jsonl
grep tool_use /tmp/stream-raw.jsonl
```
Look for: `content_block_start` with `type: "tool_use"`, `input_json_delta` events.
If absent → STOP. Evaluate alternatives before continuing.

**Step 2: Extend server-side parsing (1 hour)**
File: `server_jetty.clj`, function: `parse-stream-json-line`

Add cases for:
- `"content_block_start"` → check if `content_block.type == "tool_use"` → emit `{:event :tool-start :tool-name "Read" :tool-id "..." :block-idx N}`
- `"content_block_delta"` with `delta.type == "input_json_delta"` → emit `{:event :tool-input-delta :text "..." :block-idx N}`
- `"content_block_stop"` → emit `{:event :tool-end :block-idx N}`

Server stays a transparent pipe — parse and forward, no trail logic.

**Step 3: Client-side trail building (1.5 hours)**
File: `loop.cljs`, function: `submit-agent-run!` event handler

Modify `!agent-output` atom shape:
```clojure
{:status :running
 :provider :claude
 :prompt "..."
 :trail []           ;; NEW: structured trail nodes
 :output ""          ;; KEEP: flat text (backward compat, fallback)
 :tool-buf {}}       ;; accumulator: block-idx → {:tool "Read" :input-json ""}
```

New event handlers:
- `:text-delta` → append to `:output` AND conj `{:type :reasoning :text "..."}` to `:trail`
- `:tool-start` → add entry to `:tool-buf` for this block-idx
- `:tool-input-delta` → accumulate partial JSON in `:tool-buf`
- `:tool-end` → parse accumulated JSON, conj `{:type :tool-call :tool "Read" :input {:file_path "..." ...}}` to `:trail`, remove from `:tool-buf`

**Step 4: Trail rendering — basic (1 hour)**
File: `loop.cljs`, agent output rendering section

For each trail node, format as styled text:
- `:reasoning` → normal white/gray text (same as current)
- `:tool-call` with tool "Read" → `▸ READ file.cljs:offset-limit` in cyan/accent color
- `:tool-call` with tool "Edit" → `▸ EDIT file.cljs` in yellow/accent
- `:tool-call` with tool "Grep" → `▸ SEARCH "pattern" in path/` in green/accent

No new rendering primitives — just colored text in existing wrapped layout.

**Step 5: Test and evaluate (30 min)**
Run real agent task, look at trail output.
- Is it more useful than flat text?
- Can you follow the agent's thinking?
- Is it too noisy or just right?
→ This judgment determines whether we continue to Layer 1.

### Layer 1: Structured Trail (Session 2)

**Server**: Accumulate tool input deltas server-side. Send complete tool call events:
```clojure
{:event :tool-call :tool "Read" :input {:file_path "loop.cljs" :offset 1430 :limit 15}}
```
(Instead of raw deltas — cleaner for client.)

**Client**: Merge consecutive `:reasoning` nodes into single blocks. Richer trail data structure.

**Rendering**:
- Background rect behind tool call lines (slightly darker than panel bg)
- Visual separation between reasoning and actions
- Auto-scroll still works (same mechanism)

### Layer 2: Click-to-Navigate (Session 3)

- Hit detection on tool call header lines (same pattern as cmd-panel click targets)
- On click: load file (or switch to loaded file), scroll to line offset
- Highlight relevant line range briefly after navigation
- Trail panel stays visible — user clicks back and forth between trail and code

This is where it becomes a **real code thread** — not a log, but a navigable map.

### Layer 3: Rama Persistence (Session 4)

New PState:
```clojure
$$agent-trails {session-id -> {:actor :agent-type
                               :question prompt-text
                               :trail [nodes...]
                               :created-at timestamp
                               :status :complete/:running
                               :children []}}  ;; for fractal nesting later
```

Enables:
- Reopening trails later ("show me yesterday's exploration")
- Querying by file ("what has the agent done to loop.cljs?")
- Resume support (--resume session-id already captured)
- Foundation for org-wide reasoning ledger

### Layer 4: Summarization (Session 5+)

- Agent summarizes its own trail at different zoom levels
- "Developer view": full trail (default)
- "Reviewer view": key decisions, tradeoffs, files changed
- "Manager view": impact, timeline, risk
- This is the fractal layer — same trail, different zoom for different reviewers

### What NOT to build (scope control)

1. No syntax highlighting in trail snippets until Layer 2+
2. No split view (trail + editor side by side) — trail lives in agent panel
3. No sub-threads / drill-down / branching — flat trail first
4. No fractal nesting (user-trails containing agent-trails) — one level first
5. No custom agent loop — use CLI stream unless proven inadequate
6. No alternative providers — Claude only (Codex/Gemini don't have tool_use streaming)
7. No offline / file export — trails are in-memory until Layer 3

### Data model (designed for fractal extension)

```edn
;; Trail node types
{:type :reasoning :text "..." :timestamp ms}
{:type :tool-call :tool "Read" :input {:file_path "..." :offset N :limit N} :timestamp ms}
{:type :tool-call :tool "Edit" :input {:file_path "..." :old_string "..." :new_string "..."} :timestamp ms}
{:type :tool-call :tool "Grep" :input {:pattern "..." :path "..."} :timestamp ms}
{:type :tool-result :content "..." :tool-id "..." :timestamp ms}

;; Trail container (Rama PState shape, used from Layer 3)
{:trail-id "uuid"
 :actor {:type :agent :provider :claude :session-id "..."}
 :reviewer {:type :human :id "sid"}
 :task "implement auth middleware"
 :nodes [trail-nodes...]
 :children []           ;; sub-trail IDs (fractal nesting, Layer 4+)
 :created-at timestamp
 :completed-at timestamp
 :status :complete}
```

### Confidence assessment

| Aspect | Confidence | Why |
|--------|-----------|-----|
| Data exists in stream | HIGH | Line 349 explicitly skips content_block_start/stop — we've seen them |
| Server parsing works | HIGH | Well-defined JSON parsing, existing pattern |
| Client trail building | HIGH | Atom accumulation, straightforward state machine |
| Basic trail rendering | HIGH | Colored text in existing panel, no new primitives |
| Trail is actually useful | MEDIUM | UX judgment call — might be too noisy without filtering |
| Click-to-navigate | MEDIUM | Hit detection exists, but line offset → scroll position needs care |
| Rama persistence | MEDIUM | Schema design is clean, Rama DSL has gotchas |
| Summarization | LOW | Prompt engineering + uncertain quality, needs experimentation |

### What we're most likely to learn
1. Whether 10 tool calls or 50 tool calls is the norm for a typical agent task → determines if filtering is needed
2. Whether the reasoning text between tool calls is enough context or too sparse → determines if we need tool result content too
3. Whether the existing agent panel dimensions work for trail rendering or if we need more screen space → determines Layer 2 layout decisions
