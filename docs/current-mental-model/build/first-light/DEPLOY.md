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

1. **Where the land lives (first-light):** (a) laptop, as now — zero new ops, it's where Sid works ← pick · (b) always-on home box · (c) VPS. The always-on box becomes right when overnight agents / the consolidator arrive; log-primary keeps the move cheap once durability exists.
2. **Durability (forced by the restart gate) — needs one technical check before the contract locks:** (a) real single-node Rama locally (native disk persistence; exact recipe + licensing checked against Rama docs/skill) · (b) keep IPC + the land journals native events to a local append-only file and replays at boot (files stay the durable layer — consistent with how ingest already works; hand-rolls part of what Rama does) · (c) RAM-only + transcript-ingest fallback — REJECTED, fails the gate for wishes/face revisions.
3. **Model lane for first-light:** (a) keep the CLI subprocess lanes (claude + codex) on **subscription auth** ← pick, and Sid's stated constraint (2026-07-15: "use the subscription that i have for claude and codex, not API pricing") · (b) direct API — API pricing; fallback/special-cases only · (c) swappable later via the replaceable-reader boundary (unchanged).
4. **Backups:** nightly copy of the durable layer (journal or cluster dir) alongside existing git — boring on purpose ← pick.

## Subscriptions, not API (Sid's constraint — already the built default)

The problem space is already researched and canonical: `architecture/dogfood-runtime/llm-track-claude-research.md` (auth-path priority order §~207; the two Softland modes; the OpenClaw ban patterns §4) and `llm-track-canonical.md` (§2 Codex protocol). Verified in code this session:

- `claude-process-spec` defaults `:llm/auth-mode` to **`:subscription`** (`dogfood/llm.clj:1878`).
- In subscription mode the child env is the system env **stripped of sensitive keys** (`claude-child-env`, llm.clj:1851-1856) — `ANTHROPIC_API_KEY` etc. are removed, so the CLI can only fall back to its logged-in OAuth (Sid's Max plan). API-key mode is the explicit, opt-in branch and (per the research doc) spawns `claude --bare` so it structurally cannot drift onto subscription credentials. Clean either/or, subscription-safe by default.
- Codex is a first-class backend (`#{:codex :claude}`, full observation vocabulary) — its ChatGPT-subscription path is the `llm-track-canonical.md` §2 protocol (not line-verified this session).

Consequences: **(1)** the CLI-subprocess lane isn't just convenient — it is the only legitimate way to ride the subscriptions, which pins Decision 3(a). **(2)** Wherever the land runs, the CLIs must be logged in there — laptop-local aligns perfectly; a future host means logging in Sid's CLIs on Sid's own box (single-user, own workload = the sanctioned shape per the research doc; the banned shapes are pooling/serving others — and North's own answer for visitors is "every visitor arrives with their own assistant," never Softland holding their tokens). **(3)** Budgets are rate-limit-shaped, not dollar-shaped: overnight/consolidator work spends Max-plan window capacity; `:result/cost-usd` in subscription mode is API-equivalent accounting, render it as such, never as money spent.

## The rest of the ops question space (one-line stances)

Secrets: env.clj + CLI auth stay server-side, never client. · Cost: every run's cost is already an observation — the land can render its own burn; always-on budgets are a later decision. · Offline: reading/writing works without network; only model calls need it; wishes can queue. · Upgrades: module updates must replay/migrate the log — contract concern once durability exists. · Privacy: sense-line data is the most personal data Sid owns → local-first + never-pushed docs branch already encode the stance; backups inherit it (encrypt if off-machine). · Phone/multi-device: requires hosting or a tunnel — deferred until wanted. · Provider outage: the land degrades to a quiet land (write/read fine, agents silent). · Agent permissions: the subprocess runs with Sid's user rights today; sandboxing becomes a real decision when agents act more autonomously.
