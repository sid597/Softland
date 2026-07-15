# first-light — where the land runs (ops walkthrough seed)

2026-07-15 · Born from Sid's deployment interrogation ("where does the data go? is it local? where do model calls run? how does the model know which code?"). Same discipline as WALKTHROUGH.md: grounded facts cited, inferences flagged, real decisions extracted.

## Today's truth (verified in code)

One JVM on Sid's laptop runs everything (`clj -A:dev -X dev/-main`): Electric server + Rama + ingest watchers + the model lane. The browser talks to localhost.

- **Rama is in-memory.** The runtime creates the cluster with `create-ipc` from `com.rpl.rama.test` (e.g. `object_container/runtime.clj:13`) — the in-process test cluster. PStates and depots live in RAM; **nothing Rama holds survives a process restart.**
- **The durable layer today is files + git.** md/docs, the repo, and Claude Code's own transcripts (`~/.claude/projects/**/*.jsonl`, watched per `dogfood/transcript.clj:79`) are on disk; the land re-ingests them. The land today = a projection over durable files + volatile native edits.
- **The model lane shells out to the `claude` CLI** in stream-json mode from the server process (`dogfood/llm.clj:2137-2169`): context bundle written to stdin as a structured envelope, observations parsed from the stream, per-run cost and the native Claude session id captured (`:result/cost-usd`, `:native/claude-session-id`), nonzero exits recorded as failed-run observations. The subprocess runs with a working directory from the run spec (repo, by design), so the CLI's own tools can read real files.
- **Code awareness = addresses, not vibes:** runs receive the SCENE-CTX bundle (visible material, selection, material addresses, `:assembly/src-path`); code material is addressed-not-copied (git-spine/code-atoms), so an address maps to file + commit. Demonstrated: the $0.46 bundle-riding run answered with repo awareness.

## The gap the questions exposed

First-light's gate says "restart preserves conversation, face revision, and causal trail." Ingested material survives restarts (files re-ingest). **Native-born material — first-light conversations, wishes, face revisions — currently has no durable home.** The gate cannot pass on `create-ipc` alone. This was implicit; it is now explicit and is the package's one hard ops prerequisite.

## Answers of record

- **Multi-turn and `claude -p`:** one turn per invocation, yes — but the CLI supports resuming sessions, and the lane already captures the session id, so cross-invocation continuity is plumbed-ready. The deeper stance (constitutional): **the land is the memory** — each run is fed the trail material it needs; the "session" is the trail, not the tool's session file. That keeps the model tool swappable (replaceable-reader rule): CLI today, direct API or local model later, same trail in, receipts out. Practical: resume-by-session-id where it helps caching; never as the source of truth.
- **Where model calls run:** from wherever the land's JVM runs. Keys/auth stay with the server process; the browser never holds them. Hosted land ⇒ calls from the host; local land ⇒ from the laptop.

## Decisions

1. **Where the land lives: DECIDED (Sid, 2026-07-15).** The land runs on the PC (where Sid works; can power off / lose power). The MacBook (8GB, low spec, configured to stay on) is the **vault, never a runtime** — it holds journal copies over the existing rsync pipe. Files rsync; clusters don't.
   *Mac-hosting considered and declined (Sid asked 07-15):* 8GB swaps under JVM+RAM-state+CLI subprocesses, and lag is an H1 kill risk; splitting land (Mac) from workshop (PC) adds a cross-machine deploy step inside the repair loop, killing wish-to-worn-change latency during exactly the wear month; the protection Sid wanted (unstable code must not endanger real data) is delivered by the journal at the data layer — the process is a disposable cache of the log. *Server trigger, not server date:* the first organ that must run while Sid sleeps (consolidator / overnight agents / phone access). The move then = copy journal + replay on the new box — portable by construction.
2. **Durability: DECIDED (Sid's requirement, 2026-07-15 — "before first-light I want the log to exist and some form of backup so we can replay it in future").** The **durable-log slice** (below), pre-first-light: write-ahead journal + replay-only boot + explicit ingest + rsync vault. Real single-node Rama demoted to a later graduation (when replay gets slow → snapshot or real cluster; the journal survives that migration too).
3. **Model lane for first-light:** (a) keep the CLI subprocess lanes (claude + codex) on **subscription auth** ← pick, and Sid's stated constraint (2026-07-15: "use the subscription that i have for claude and codex, not API pricing") · (b) direct API — API pricing; fallback/special-cases only · (c) swappable later via the replaceable-reader boundary (unchanged).
4. **Backups:** nightly copy of the durable layer (journal or cluster dir) alongside existing git — boring on purpose ← pick.

## The durable-log slice (pre-first-light; contract next)

Shape — the log is primary, applied to ops; the running cluster becomes a cache:

1. **Write-ahead journal.** Every event entering any depot is appended first to a plain append-only file (one self-describing EDN map per line: schema version, wall time, depot, event verbatim), fsync'd, then appended to the depot. Segments roll daily (`journal/YYYY-MM-DD.ndedn`). Power cut ⇒ at worst one truncated final line, detected and skipped at replay — never corruption.
2. **Boot = replay only.** Startup reads segments in order and re-appends into the in-memory cluster. The boot-time ingest (`initial-sweep!` + `start-ingest-watchers!` + `run-git-spine-boot!`, `file_viewer.cljc:193-198`) comes OFF the startup path (Sid, 2026-07-15: "they were for testing and now it's done") and becomes an explicit act — a callable command now, a wish later. **Ordering law: removal lands WITH replay in one slice** — removed alone, every boot is an empty land (boot-ingest is what currently refills RAM).
3. **Ingest writes through the journal too.** Explicit ingestion appends events like everything else — one truth path, one replay path; recovery never needs re-ingest. Events carry their existing deterministic ids, so replay re-appends stored events verbatim, regenerating nothing.
4. **Backup = the existing rsync pipe.** Journal dir → MacBook on a timer whenever the Mac is reachable (append-only files rsync cheaply). PC off ⇒ backup resumes next co-uptime. Sense-line privacy: the vault copy inherits the never-pushed rule; encrypt if it ever leaves the two machines.

**Why journal, not Rama's own disk (answers Sid's "not confident in the existing structure and code"):** the journal is *code-independent* — plain lines any future implementation can replay, even a full rewrite; Rama cluster state is opaque and version-coupled. This inverts the confidence problem: the ONE artifact that must be right is the journal format; all code stays churnable. The log outlives the code; the code is a view.

**The path, as confirmed with Sid (07-15):**

```
  YOU (type · wish · accept)          AGENT (subprocess observations)
        └───────────────┬──────────────────┘
                        ▼
  ① JOURNAL  (PC disk, append-only, flushed first — the only artifact
              that must survive; everything below rebuilds from it)
                        ▼
  ② RAMA depots (RAM) → ③ topologies/PStates (RAM) → ④ projection (seen)
      what Sid sees = echo of processed truth, own keystrokes included

  BOOT:    journal/*.ndedn ──replay──▶ ② ▶ ③ ▶ ④   (nothing else)
  BACKUP:  journal/ ──rsync when both awake──▶ MacBook vault
  LOSS BOUND: one in-flight event (a keystroke that never reached disk
  never existed anywhere); half-written tail line skipped at replay.
```

**Flush policy — two duty classes (refined with Sid, 07-15, from the "is typing slow now?" question):** journal *appends* are buffered (~µs, invisible); only the *flush* costs (0.5–2ms SSD). Text/edit ops ride **group commit** — buffered, flushed every ~50ms; per-keystroke felt cost ≈ 0; power-cut loss bound = the keys since the last flush (RAM dies with the power too, so no inconsistency is possible — only a missing final instant). **Signing-grade events flush immediately before processing** — wish, accept, revert, run-the-agent — because an agent must never have run for a reason the land doesn't remember; rare and human-paced, 1–2ms is nothing. Same silver/gold instinct applied to disk. Event grain unchanged from today (block-write already streams per-op; the 7.66ms echo is that grain, in RAM). Growth: ~MB/day, ~GB/year — trivial; the real future cost is boot replay time → snapshot-plus-tail is the named graduation.

**Falsification list for the slice contract:** replay must be pure materialization — executors/LLM intents must NOT re-fire on replayed events (intent-vs-executor split verified under replay); every depot entry point passes the shim (foreign appends audited); group-commit + signing-flush latency measured on the real write path (block-write p95 must survive; signing events verified durable-before-side-effect); truncated-tail recovery tested by killing the JVM mid-write; replay idempotence tested by double-replay; boot time at N months of events measured, snapshot threshold named.

## Subscriptions, not API (Sid's constraint — already the built default)

The problem space is already researched and canonical: `architecture/dogfood-runtime/llm-track-claude-research.md` (auth-path priority order §~207; the two Softland modes; the OpenClaw ban patterns §4) and `llm-track-canonical.md` (§2 Codex protocol). Verified in code this session:

- `claude-process-spec` defaults `:llm/auth-mode` to **`:subscription`** (`dogfood/llm.clj:1878`).
- In subscription mode the child env is the system env **stripped of sensitive keys** (`claude-child-env`, llm.clj:1851-1856) — `ANTHROPIC_API_KEY` etc. are removed, so the CLI can only fall back to its logged-in OAuth (Sid's Max plan). API-key mode is the explicit, opt-in branch and (per the research doc) spawns `claude --bare` so it structurally cannot drift onto subscription credentials. Clean either/or, subscription-safe by default.
- Codex is a first-class backend (`#{:codex :claude}`, full observation vocabulary) — its ChatGPT-subscription path is the `llm-track-canonical.md` §2 protocol (not line-verified this session).

Consequences: **(1)** the CLI-subprocess lane isn't just convenient — it is the only legitimate way to ride the subscriptions, which pins Decision 3(a). **(2)** Wherever the land runs, the CLIs must be logged in there — laptop-local aligns perfectly; a future host means logging in Sid's CLIs on Sid's own box (single-user, own workload = the sanctioned shape per the research doc; the banned shapes are pooling/serving others — and North's own answer for visitors is "every visitor arrives with their own assistant," never Softland holding their tokens). **(3)** Budgets are rate-limit-shaped, not dollar-shaped: overnight/consolidator work spends Max-plan window capacity; `:result/cost-usd` in subscription mode is API-equivalent accounting, render it as such, never as money spent.

## The rest of the ops question space (one-line stances)

Secrets: env.clj + CLI auth stay server-side, never client. · Cost: every run's cost is already an observation — the land can render its own burn; always-on budgets are a later decision. · Offline: reading/writing works without network; only model calls need it; wishes can queue. · Upgrades: module updates must replay/migrate the log — contract concern once durability exists. · Privacy: sense-line data is the most personal data Sid owns → local-first + never-pushed docs branch already encode the stance; backups inherit it (encrypt if off-machine). · Phone/multi-device: requires hosting or a tunnel — deferred until wanted. · Provider outage: the land degrades to a quiet land (write/read fine, agents silent). · Agent permissions: the subprocess runs with Sid's user rights today; sandboxing becomes a real decision when agents act more autonomously.
