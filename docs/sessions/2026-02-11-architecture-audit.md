# Session Notes — 2026-02-11 (Architecture Audit: Rama Integration)

## Scope

Full architecture audit of the AI interaction pipeline. Checked whether the system is reactive end-to-end (Rama → Electric → WebGPU) as described in `claude-3.md`.

---

## Finding: Two Parallel Systems

The codebase had **two disconnected systems** for AI interactions:

**System A (Working, Imperative):**
```
Client Ctrl-K → submit-agent-run! → HTTP POST /api/agent/run
  → server_jetty.clj spawns process (blocks HTTP thread)
  → HTTP response → .then callback → reset! !agent-output atom
  → m/watch picks it up → WebGPU renders
```

**System B (Scaffolded, Inert):**
```
$$agent-runs-pstate   — declared, topology case exists, stores metadata only
$$cli-sessions-pstate — declared, foreign-pstate bound, NEVER written to
util_fns/submit-agent-run — defined, never called from HTTP handler
util_fns/get-cli-session  — defined, never called in hot path
objects/parse-claude-json-output — defined, never called from HTTP handler
objects/provider-default-argv    — uses "json" format (correct), never called
```

System A worked. System B was dead code. The systems never touched each other.

---

## What Was Broken

### 1. `--resume` (session-id) was end-to-end broken

- `parse-claude-json-output` correctly extracts `session_id` from Claude JSON → never called
- `$$cli-sessions-pstate` declared to store session-ids → never written to
- `get-cli-session` reads from PState for resume → never called before execution
- `provider-default-argv` in objects.cljc injects `--resume` + uses `"json"` format → never used

The HTTP handler had its OWN `provider-default-argv` (duplicate) using `"text"` format, which doesn't return session-ids.

### 2. Rama was completely bypassed

- `/api/agent/run` handler stored results in `(defonce !agent-runs (atom {}))` — a plain Clojure atom
- Never called `submit-agent-run` or any depot-append function
- AI interactions were fire-and-forget: refresh → gone

### 3. Context capture went nowhere

- Client captures `{:cursor :selection :visible-range :file-path :timestamp}` in POST body
- Server ignores it entirely — not stored, not passed to AI

---

## Changes Made

### 1. `core.clj` — New `:update-cli-session` topology case (lines 295-306)

Handles the new event type by writing `{session-id, last-active}` to `$$cli-sessions-pstate`:

```clojure
(case> (= :update-cli-session *action-type))
(local-select> (keypath :file-path) *event-data :> *file-path)
(local-select> (keypath :provider) *event-data :> *provider)
(local-select> (keypath :session-id) *event-data :> *session-id)
(identity (System/currentTimeMillis) :> *now)
(local-transform>
  [(keypath *file-path) (keypath *provider)
   (multi-path
     [:session-id (termval *session-id)]
     [:last-active (termval *now)])]
  $$cli-sessions-pstate)
```

### 2. `util_fns.cljc` — New `update-cli-session` function (lines 205-217)

Appends `:update-cli-session` event to the depot:

```clojure
(defn update-cli-session [file-path provider session-id]
  (when (and (seq file-path) provider (seq session-id))
    (foreign-append! event-depot
      (->node-events :update-cli-session {} {:graph-name :main ...})
      :append-ack)))
```

### 3. `server_jetty.clj` — Wired to Rama

Three sub-changes:

**a. Added requires** for `util-fns` and `rama-objects` (lines 10-11)

**b. Removed duplicate `provider-default-argv`** that used `"text"` format.
   Now uses `rama-objects/provider-default-argv` which uses `"json"` for Claude.

**c. Modified `run-agent-request`** (lines 194-270):
- Before execution: looks up session-id from Rama via `util-fns/get-cli-session`
- Injects `--resume SESSION_ID` into argv when session exists
- After execution: parses Claude JSON via `rama-objects/parse-claude-json-output`
- Extracts session-id and clean text content from JSON response
- On success: stores session-id in Rama via `util-fns/update-cli-session`
- Returns clean text (not raw JSON) to client

---

## New Data Flow (End to End)

```
User types "explain fold detection" → Enter

loop.cljs submit-agent-run!
  reads: @!ai-provider, @!current-file, cursor/viewport context
  sends: POST /api/agent/run {:provider :claude :prompt "..." :file "..." :context {...}}
  sets:  !agent-output → {:status :running}

server_jetty.clj run-agent-request
  1. Looks up: util-fns/get-cli-session → $$cli-sessions-pstate → {:session-id "abc"}
  2. Builds argv: ["claude" "--resume" "abc" "-p" "..." "--output-format" "json"]
  3. Spawns process, blocks until complete
  4. Parses: rama-objects/parse-claude-json-output → {:session-id "def" :content "The fold..."}
  5. Stores: util-fns/update-cli-session → depot → topology → $$cli-sessions-pstate
  6. Returns: {:status :complete :result {:output "The fold..."}}

loop.cljs .then callback
  resets !agent-output → {:status :complete :output "The fold..."}
  m/watch propagates → <combined-text-ops → GPU buffer → WebGPU renders
```

Next time user asks about the same file+provider:
- Step 1 finds session-id "def" in Rama
- Claude resumes with full conversation context

---

## What Was NOT Changed (Deliberate)

- **Electric bridge remains one-shot** — HTTP long-poll works; `e/watch` can't cross server→client boundary
- **`$$agent-runs-pstate` still stores metadata only** — the actual execution path still uses the HTTP handler directly, not the Rama topology's `:agent-run` case
- **Context field still not persisted** — sent in POST body, not yet stored in Rama
- **No streaming** — still blocking HTTP; streaming requires a different transport

---

## Architectural Quality

| Aspect | Before | After |
|--------|--------|-------|
| Session persistence | None (atom, dies on restart) | Rama PState (survives restart) |
| `--resume` support | Broken end-to-end | Working: lookup → inject → parse → store |
| Claude output format | `text` (no session-id) | `json` (session-id + clean content) |
| Code duplication | Two `provider-default-argv` functions | One (in objects.cljc) |
| Rama in the loop | Bypassed entirely | Session-ids flow through depot → topology → PState |
| Client experience | Same | Same (cleaner output: no JSON noise) |

---

## Remaining Gaps (for future sessions)

1. **Context storage** — Pipe cursor/selection/viewport/fold-state into a Rama depot event
2. **Interaction history** — Expand `$$cli-sessions-pstate` schema to store prompt/response pairs
3. **`$$agent-runs-pstate` integration** — Have HTTP handler also call `submit-agent-run` for metadata logging
4. **Electric reactive bridge** — When streaming is needed, wire `!subscribe` → `e/server` → `e/client`
5. **Command panel disappearing on type** — Separate UI bug, not architectural
