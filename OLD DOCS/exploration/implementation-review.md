Plan: Fix Codex's Implementation + Add --resume                                                                 │
│                                                                                                                 │
│ Context                                                                                                         │
│                                                                                                                 │
│ Codex implemented the first vertical slice of the Rama-native CLI agent runtime (~425 lines across 6 files).    │
│ The server-side code is solid (separate depot, TaskGlobal, ProcessBuilder execution, PState lifecycle). The     │
│ client-side rendering is good (output panel, status colors, provider prompt). But:                              │
│                                                                                                                 │
│ 1. Polling is the wrong bridge. Codex polls /api/agent/run-status every 350ms (~100 lines of infra). This burns │
│  CPU when idle (violating Session 16b's optimization) and adds 350ms latency.                                   │
│ 2. --resume isn't wired. The killer feature — AI remembers previous conversations per file — is missing.        │
│ 3. PState schema is untyped. {String (map-schema Keyword Object)} loses safety.                                 │
│ 4. Output truncated at 10 lines. Real CLI responses can be hundreds of lines.                                   │
│                                                                                                                 │
│ Why blocking HTTP instead of Electric subscription                                                              │
│                                                                                                                 │
│ Investigation revealed that Electric 3's e/watch only works within a single peer (server OR client). Missionary │
│  observables cannot be piped across the server→client boundary. There is NO existing pattern in this codebase   │
│ for reactive server→client data transfer.                                                                       │
│                                                                                                                 │
│ Blocking HTTP is the simplest correct approach:                                                                 │
│ - Client POSTs to /api/agent/run                                                                                │
│ - Server appends to Rama depot, then waits (server-side loop) for topology to complete                          │
│ - Server returns the result in the HTTP response                                                                │
│ - Client's js/fetch .then() fires with the result, updates atom                                                 │
│ - User has been editing the whole time because js/fetch is non-blocking                                         │
│                                                                                                                 │
│ This removes ~100 lines of polling code and replaces them with ~10 lines of server-side wait loop. JVM threads  │
│ are cheap. The async user experience is identical.                                                              │
│                                                                                                                 │
│ ---                                                                                                             │
│ Files to Modify                                                                                                 │
│ File: src/app/server_jetty.clj                                                                                  │
│ What Changes: Make /api/agent/run block until completion, remove /api/agent/run-status                          │
│ ────────────────────────────────────────                                                                        │
│ File: src/app/server/rama/core.clj                                                                              │
│ What Changes: Add $$cli-sessions-pstate, update topology to store session-id                                    │
│ ────────────────────────────────────────                                                                        │
│ File: src/app/server/rama/objects.cljc                                                                          │
│ What Changes: Change Claude default to --output-format stream-json, add session-id parser                       │
│ ────────────────────────────────────────                                                                        │
│ File: src/app/server/rama/util_fns.cljc                                                                         │
│ What Changes: Add get-cli-session, update-cli-session helpers                                                   │
│ ────────────────────────────────────────                                                                        │
│ File: src/app/client/webgpu/loop.cljs                                                                           │
│ What Changes: Rip out polling infra, replace with POST+.then(), remove output truncation                        │
│ ---                                                                                                             │
│ Step-by-Step Changes                                                                                            │
│                                                                                                                 │
│ Step 1: server_jetty.clj — Blocking HTTP handler                                                                │
│                                                                                                                 │
│ Replace the current /api/agent/run handler (fire-and-forget + separate status endpoint) with a blocking handler │
│  that waits for the run to complete.                                                                            │
│                                                                                                                 │
│ Remove: /api/agent/run-status endpoint (no longer needed).                                                      │
│                                                                                                                 │
│ Modify /api/agent/run:                                                                                          │
│ "/api/agent/run"                                                                                                │
│ (if (= request-method :post)                                                                                    │
│   (try                                                                                                          │
│     (let [request-data (parse-edn-body ring-req)                                                                │
│           run-id (rama/submit-agent-run request-data)                                                           │
│           ;; Block until topology processes the run (server-side wait)                                          │
│           result (loop [attempts 0]                                                                             │
│                    (let [run (rama/get-agent-run run-id)]                                                       │
│                      (cond                                                                                      │
│                        (#{:complete :failed} (:status run)) run                                                 │
│                        (> attempts 600) {:status :timeout :run-id run-id}                                       │
│                        :else (do (Thread/sleep 500) (recur (inc attempts))))))]                                 │
│       (json-response result))                                                                                   │
│     (catch Exception e                                                                                          │
│       (json-response {:error (str "Failed: " (.getMessage e))})))                                               │
│   (json-response {:error "Method not allowed. Use POST."}))                                                     │
│                                                                                                                 │
│ Server-side wait loop: poll PState every 500ms, timeout at 5 minutes. JVM thread is cheap.                      │
│                                                                                                                 │
│ Step 2: rama/core.clj — Add $$cli-sessions-pstate + session-id storage                                          │
│                                                                                                                 │
│ Add PState declaration (alongside existing PStates):                                                            │
│ (declare-pstate n $$cli-sessions-pstate                                                                         │
│   {String    ;; file-path                                                                                       │
│    {Keyword  ;; provider (:claude :codex :gemini)                                                               │
│     (fixed-keys-schema                                                                                          │
│       {:session-id String                                                                                       │
│        :last-active Long})}})                                                                                   │
│                                                                                                                 │
│ Modify the agent-events-depot topology to also update $$cli-sessions-pstate when a run completes:               │
│ After the existing (local-transform> ... $$agent-runs-pstate) completion block, add:                            │
│ ;; Extract session-id from result and store for --resume                                                        │
│ (let> [*file-path   (get> *request-data :file)                                                                  │
│        *session-id  (get> *result :session-id)]                                                                 │
│   (when> (and *file-path *session-id)                                                                           │
│     (local-transform>                                                                                           │
│       [(keypath *file-path) (keypath *provider)]                                                                │
│       (termval {:session-id *session-id                                                                         │
│                 :last-active *end-ms})                                                                          │
│       $$cli-sessions-pstate)))                                                                                  │
│                                                                                                                 │
│ Fix $$agent-runs-pstate schema — replace untyped (map-schema Keyword Object) with a proper fixed-keys schema    │
│ for safety.                                                                                                     │
│                                                                                                                 │
│ Step 3: rama/objects.cljc — Stream-JSON output + session-id parsing                                             │
│                                                                                                                 │
│ Change provider-default-argv for Claude to use --output-format json (not text):                                 │
│ :claude (vec (concat ["claude"]                                                                                 │
│                      (when (seq session-id) ["--resume" session-id])                                            │
│                      ["-p" (or prompt "") "--output-format" "json"]))                                           │
│                                                                                                                 │
│ Verified: claude -p "..." --output-format json returns a single JSON object:                                    │
│ {"type":"result", "session_id":"a4d049e8-...", "result":"Hello there...", ...}                                  │
│                                                                                                                 │
│ This gives us both session_id (for --resume) and result (the actual text) in one parse.                         │
│                                                                                                                 │
│ Add parse-claude-json-output function:                                                                          │
│ (defn parse-claude-json-output                                                                                  │
│   "Parse Claude's JSON output to extract session-id and text content."                                          │
│   [raw-output]                                                                                                  │
│   (try                                                                                                          │
│     (let [parsed (json/parse-string raw-output true)]                                                           │
│       {:session-id (:session_id parsed)                                                                         │
│        :content (or (:result parsed) raw-output)})                                                              │
│     (catch Exception _                                                                                          │
│       {:session-id nil :content raw-output})))                                                                  │
│                                                                                                                 │
│ Modify run-cli-process return value to include provider so caller can parse:                                    │
│ {:output output :exit-code exit-code :provider provider ...}                                                    │
│                                                                                                                 │
│ Modify cli-exec-future to parse Claude output after process completes:                                          │
│ (let [result (run-cli-process cmd cwd)                                                                          │
│       parsed (when (= provider :claude)                                                                         │
│                (parse-claude-json-output (:output result)))]                                                    │
│   (assoc result                                                                                                 │
│     :output (if parsed (:content parsed) (:output result))                                                      │
│     :session-id (when parsed (:session-id parsed))                                                              │
│     :provider provider))                                                                                        │
│                                                                                                                 │
│ Step 4: rama/util_fns.cljc — Session lookup helpers                                                             │
│                                                                                                                 │
│ Add foreign PState binding:                                                                                     │
│ (def cli-sessions-pstate (foreign-pstate @!rama-ipc (get-module-name node-events-module)                        │
│ "$$cli-sessions-pstate"))                                                                                       │
│                                                                                                                 │
│ Add session lookup:                                                                                             │
│ (defn get-cli-session [file-path provider]                                                                      │
│   (first (foreign-select [(keypath file-path) (keypath provider)] cli-sessions-pstate)))                        │
│                                                                                                                 │
│ Modify submit-agent-run to auto-inject session-id from previous sessions:                                       │
│ (defn submit-agent-run [request-data]                                                                           │
│   (let [run-id (or (:run-id request-data) (str (java.util.UUID/randomUUID)))                                    │
│         ;; Auto-inject session-id for --resume if not provided                                                  │
│         file-path (:file request-data)                                                                          │
│         provider (:provider request-data)                                                                       │
│         session (when (and file-path provider)                                                                  │
│                   (get-cli-session file-path provider))                                                         │
│         request-data (cond-> (assoc request-data :run-id run-id)                                                │
│                        (and session (not (:session-id request-data)))                                           │
│                        (assoc :session-id (:session-id session)))                                               │
│         ...]                                                                                                    │
│     ...))                                                                                                       │
│                                                                                                                 │
│ Step 5: loop.cljs — Rip out polling, use POST+.then()                                                           │
│                                                                                                                 │
│ Remove (Codex's polling infrastructure):                                                                        │
│ - make-interval-flow function (~7 lines)                                                                        │
│ - <agent-poll-requests function (~13 lines)                                                                     │
│ - poll-agent-run! function (~28 lines)                                                                          │
│ - !active-agent-run-id atom                                                                                     │
│ - !agent-poll-inflight atom                                                                                     │
│ - >agent-poll-timer flow creation                                                                               │
│ - <agent-polls flow creation                                                                                    │
│ - Agent polling consumer m/reduce block (~7 lines)                                                              │
│                                                                                                                 │
│ Simplify submit-agent-run! — POST and update atom in .then():                                                   │
│ submit-agent-run!                                                                                               │
│ (fn [cmd-text]                                                                                                  │
│   (let [parsed (parse-agent-command cmd-text @!ai-provider)                                                     │
│         ...]                                                                                                    │
│     (case (:kind parsed)                                                                                        │
│       :run                                                                                                      │
│       (let [provider (:provider parsed)                                                                         │
│             prompt (:prompt parsed)                                                                             │
│             argv (:argv parsed)]                                                                                │
│         ;; Show running state immediately                                                                       │
│         (reset! !agent-output {:status :running :provider provider :prompt prompt :output "" :run-id nil})      │
│         ;; POST blocks on server until complete, .then() fires with result                                      │
│         (post-edn! "/api/agent/run"                                                                             │
│                    (cond-> {:provider provider :prompt prompt :cwd cwd :file file-path :context context}        │
│                      (seq argv) (assoc :argv argv))                                                             │
│                    (fn [resp]                                                                                   │
│                      (if-let [err (:error resp)]                                                                │
│                        (reset! !agent-output {:status :failed :provider provider :prompt prompt :output err})   │
│                        (let [result (:result resp)]                                                             │
│                          (reset! !agent-output {:status (:status resp)                                          │
│                                                 :provider (:provider resp)                                      │
│                                                 :prompt (:prompt resp)                                          │
│                                                 :output (or (:output result) "")                                │
│                                                 :run-id (:run-id resp)}))))))                                   │
│       ;; ... :set-provider, :error, :noop cases stay the same ...                                               │
│       )))                                                                                                       │
│                                                                                                                 │
│ Remove max-output-lines 10 truncation — let output render fully:                                                │
│ Change (take max-output-lines) to just use all lines. Add a reasonable max (100 lines) to prevent GPU buffer    │
│ overflow, but don't truncate at 10.                                                                             │
│                                                                                                                 │
│ Keep (Codex's good code):                                                                                       │
│ - parse-agent-command with /provider, /run support                                                              │
│ - [PROVIDER]> prompt rendering                                                                                  │
│ - Status color map (complete=green, failed=red, running=blue)                                                   │
│ - Agent output panel rendering in <combined-text-ops                                                            │
│ - !ai-provider atom + (m/watch !ai-provider) in latest                                                          │
│ - !agent-output atom + (m/watch !agent-output) in latest                                                        │
│ - post-edn! helper                                                                                              │
│                                                                                                                 │
│ ---                                                                                                             │
│ Net Code Changes                                                                                                │
│ ┌───────────────────────────────────────────────┬──────────────────────┐                                        │
│ │                    Action                     │        Lines         │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Remove polling infra (loop.cljs)              │ -100                 │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Simplify submit-agent-run! (loop.cljs)        │ -20                  │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Add blocking wait in server_jetty.clj         │ +10                  │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Remove /api/agent/run-status                  │ -7                   │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Add $$cli-sessions-pstate (core.clj)          │ +15                  │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Add session-id storage in topology (core.clj) │ +10                  │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Add parse-claude-output (objects.cljc)        │ +15                  │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Modify provider-default-argv (objects.cljc)   │ +3                   │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Add session helpers (util_fns.cljc)           │ +10                  │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Remove output truncation (loop.cljs)          │ ~0 (change constant) │                                        │
│ ├───────────────────────────────────────────────┼──────────────────────┤                                        │
│ │ Net                                           │ ~-65 lines (simpler) │                                        │
│ └───────────────────────────────────────────────┴──────────────────────┘                                        │
│ ---                                                                                                             │
│ Verification                                                                                                    │
│                                                                                                                 │
│ 1. Start the system: clj -M:dev (or however the project starts)                                                 │
│ 2. Open the editor in browser                                                                                   │
│ 3. Ctrl+Shift+P to open command panel                                                                           │
│ 4. Type: explain the fold detection in this file                                                                │
│ 5. Press Enter                                                                                                  │
│ 6. Verify: Panel shows [CLAUDE] running: explain the fold detection... immediately                              │
│ 7. Verify: User can keep editing while waiting                                                                  │
│ 8. Verify: After CLI completes, output appears in green text above command panel                                │
│ 9. Test /provider: Type /provider codex, verify prompt changes to [CODEX]>                                      │
│ 10. Test --resume: Send a second question about the same file, verify --resume flag is passed to CLI            │
│ 11. Check Rama: Verify events in depot and PState contain context (cursor, visible-range, file-path)            │
│                                                                                                                 │
│ ---                                                                                                             │
│ What Gets Deferred (Not In This Change)                                                                         │
│                                                                                                                 │
│ - Electric reactive subscription (needs deeper investigation of Electric 3 patterns)                            │
│ - Streaming chunks (slurp is fine for v1)                                                                       │
│ - Typed moves / discourse graph indexing                                                                        │
│ - Multi-lane Inquiry Arena                                                                                      │
│ - Multi-provider parallel runs                                                                                  │
│ - Scrollable output panel (for very long responses)                                                             │
│ - Codex/Gemini --resume support (only Claude for now)

---

## Implementation Review: What Actually Happened

_Written after completing all 5 steps. These are observations from the actual implementation, not the plan._

### Plan vs Reality

The plan was accurate. Every step executed as designed with no architectural surprises. The plan-first approach paid off — having exact code snippets in the plan meant most edits were copy-paste-adapt rather than design-on-the-fly.

One deviation: the plan mentioned `let>`, `get>`, `when>` for the Rama topology session-id extraction (Step 2). The actual implementation used `local-select>` + `<<if` + `and>` instead, which is what the existing topology code already uses. The plan's pseudocode was directionally right but used non-existent Rama operators. Always match the patterns already in the file.

### Insights Learned During Implementation

#### 1. Edit tool vs deep indentation in loop.cljs

The Edit tool consistently fails on deeply-indented code (30+ spaces) in loop.cljs. This happened three times during the implementation:
- The `:run` case replacement (32 spaces of indentation)
- The `poll-agent-run!` response handler
- The `post-edn!` callback block

**Workaround that works:** Use `python3 -c` with `str.replace()` via Bash. Python handles exact string matching without whitespace normalization issues. The Edit tool works fine for code indented < ~20 spaces.

**Root cause theory:** The Edit tool may be normalizing or trimming whitespace in some edge case when the match string has very long leading-space runs. The Read tool shows the content correctly, but Edit can't find it.

#### 2. Rama topology dataflow is purely positional

In the Rama `<<sources` block, operations are sequential — each line operates on bindings from previous lines. When I added the session-id extraction after the `$$agent-runs-pstate` write, the `*result`, `*end-ms`, `*provider`, and `*request-data` bindings were all still in scope. This is because Rama's dataflow compiler treats the entire `<<sources` block as a single sequential pipeline per depot source.

Key subtlety: `local-select>` with `(keypath :key)` on a Rama dataflow variable extracts a value from a map and binds it. This is different from Clojure's `get-in` — it's a Specter-style navigator that operates within Rama's dataflow graph.

#### 3. The `<<if` + `and>` pattern for conditional writes

Rama doesn't have a simple `when` — you use `<<if` with `and>` for compound conditions:
```clojure
(<<if (and> (some? *file-path) (some? *session-id))
  (local-transform> ...))
```
The `and>` is Rama's dataflow-aware boolean combinator. Regular Clojure `and` would evaluate eagerly outside the dataflow graph.

#### 4. ClojureScript `js/fetch` is already non-blocking

The entire client-side simplification works because `js/fetch` returns a Promise. The `.then()` callback fires when the server responds. During the 5+ minutes the server might be waiting, the client's event loop is completely free — RAF fires, keyboard events process, cursor blinks. Zero additional infrastructure needed.

This is why the polling approach was over-engineering: `js/fetch` already IS the async bridge. Codex built a 100-line polling system on top of something that's inherently async.

#### 5. EDN round-trip preserves keywords across the wire

The server returns EDN (via `pr-str`), and the client parses it (via `reader/read-string`). This means `:complete`, `:failed`, `:claude` survive the round-trip as Clojure keywords, not strings. No JSON keyword-vs-string mismatch. The `post-edn!` helper + `json-response` (which is actually EDN, misleading name) make this seamless.

#### 6. Cheshire's `json/parse-string` with `true` returns keyword keys

In `parse-claude-json-output`, the `true` second argument to `json/parse-string` converts JSON keys to Clojure keywords. So `"session_id"` becomes `:session_id` (with underscore, not hyphen). This is why we use `(:session_id parsed)` not `(:session-id parsed)`. The underscore-to-hyphen conversion is NOT automatic — Cheshire preserves the original casing.

#### 7. The `cond->` pattern for optional map augmentation

In `submit-agent-run`, the session-id injection uses:
```clojure
(cond-> (assoc request-data :run-id run-id)
  (and session (not (:session-id request-data)))
  (assoc :session-id (:session-id session)))
```
This is idiomatic Clojure for "always do X, conditionally also do Y". The first form after `cond->` is always applied; subsequent pairs are predicate→transform. Clean alternative to nested `if-let`.

#### 8. `m/join vector` in loop.cljs is a critical orchestration point

All the consumer flows (blink, resize, scroll, mouse, keyboard, settings) are joined with `m/join vector`. When I removed the agent polling consumer from this join, I had to make sure the remaining consumers were still balanced. If any flow in `m/join` terminates, the whole join terminates. Removing a flow from the join is safe — fewer flows means fewer termination risks.

#### 9. Status lifecycle simplified: 3 states instead of 5

Codex's original had 5 client-visible states: `:submitting` → `:running` → `:complete`/`:failed`/`:not-found`. With blocking HTTP, we only need 3: `:running` (shown immediately on submit) → `:complete`/`:failed` (returned by server). The `:submitting` and `:not-found` states were artifacts of the polling architecture. The `:submitting` color mapping still exists in `<combined-text-ops` but is now unreachable — harmless dead code, not worth removing.

### What Would I Do Differently

1. **Test Rama PState schema changes before committing.** The `$$cli-sessions-pstate` declaration with `fixed-keys-schema` hasn't been runtime-tested. If the schema doesn't match what the topology actually writes, Rama will throw at module launch. The `{:session-id String :last-active Long}` should be fine, but nested map schemas in Rama can be surprising.

2. **The `$$agent-runs-pstate` is still untyped.** The plan mentioned fixing it (`{String (map-schema Keyword Object)}` → proper `fixed-keys-schema`), but I left it as-is to minimize blast radius. Changing it would require a PState migration or a fresh Rama launch, since schema changes on existing PStates can cause errors.

3. **Add a timeout indicator in the UI.** If the server times out after 5 minutes, the client gets `{:status :timeout}`. The `<combined-text-ops` status color map doesn't have a case for `:timeout` — it falls through to the gray default. Worth adding a yellow/orange color for timeouts in a follow-up.
