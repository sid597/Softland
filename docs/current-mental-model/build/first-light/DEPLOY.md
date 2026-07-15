# first-light — where the land runs (ops walkthrough seed)

2026-07-15 · Born from Sid's deployment interrogation ("where does the data go? is it local? where do model calls run? how does the model know which code?"). Same discipline as WALKTHROUGH.md: grounded facts cited, inferences flagged, real decisions extracted.

**Correction of record (Sid's catch, 07-15 late):** an earlier version of this doc designed a hand-rolled write-ahead "journal" organ beside Rama. That was wrong twice: (1) it re-implemented Rama's own core primitive — durable, replayable event logs are what depots ARE ("All storage is durable and replicated," `docs/reference/rama/01-index.md:11`) — the actual problem is that we run the in-memory TEST cluster (`create-ipc`) as the product, when Rama's documented workflow is "test your modules using InProcessCluster and then deploy to a real cluster" (`32-downloads-maven-local-dev.md:71`); (2) journaling at burst grain while views rendered per-op state would have made the whole visible land optimistic against durable truth — contradicting the settled 07-12 no-optimistic-echo ruling. Three patches over three walls (fsync latency → group commit; semantics → burst coalescing; then the optimism contradiction) was the tell that the design was wrong at the root. Replaced below by the platform answer. Git holds the dead branch of thought.

## Today's truth (verified in code)

One JVM on Sid's PC runs everything (`clj -A:dev -X dev/-main`): Electric server + Rama + ingest watchers + the model lane. The browser talks to localhost.

- **Rama is in-memory.** The runtime creates the cluster with `create-ipc` from `com.rpl.rama.test` (e.g. `object_container/runtime.clj:13`) — the in-process TEST cluster. PStates and depots live in RAM; **nothing Rama holds survives a process restart.** This is a dev-stage artifact, not an architecture.
- **The durable layer today is files + git.** md/docs, the repo, and Claude Code's own transcripts (`~/.claude/projects/**/*.jsonl`, watched per `dogfood/transcript.clj:79`) are on disk; the land re-ingests them at boot. The land today = a projection over durable files + volatile native edits.
- **The model lane shells out to the `claude` CLI** in stream-json mode from the server process (`dogfood/llm.clj:2137-2169`): context bundle on stdin, observations parsed from the stream, per-run cost and native session id captured, nonzero exits recorded as failed-run observations. The subprocess runs with a working directory from the run spec (repo, by design).
- **Code awareness = addresses, not vibes:** runs receive the SCENE-CTX bundle (visible material, selection, material addresses, `:assembly/src-path`); code material is addressed-not-copied (git-spine/code-atoms). Demonstrated: the $0.46 bundle-riding run answered with repo awareness.

## The durable-ground slice (pre-first-light; contract next, written under /rama)

The problem: native-born material (first-light conversations, wishes, face revisions) has no durable home, and the restart gate ("restart preserves conversation, face revision, causal trail") cannot pass on `create-ipc`. The platform answer:

1. **Run a real single-node Rama cluster on the PC.** Depots and PStates become durable and recover natively on restart — no replay machinery, no new organ, no code beyond swapping `create-ipc` for a foreign-client connection to the local cluster and deploying modules via the Rama CLI. Every acked write is durable truth: the per-op stream and the 7.66ms truth-echo law stay **exactly** as ruled 07-12 — what Sid sees remains the echo of durable truth, per keystroke, no optimism anywhere, no grain decisions, no flush lanes.
2. **Backup = Rama's built-in mechanism** (`22-backups.md`: pluggable provider, all module state, one-line restore) to a local filesystem target, rsync'd to the MacBook vault on the existing pipe. Vault copies inherit the never-pushed privacy rule; encrypt if they ever leave the two machines.
3. **Boot-time ingest comes OFF the startup path** (Sid, 07-15: "they were for testing and now it's done") — with a durable cluster this is trivial and independent: state is simply there at boot; `initial-sweep!` + `start-ingest-watchers!` + `run-git-spine-boot!` (`file_viewer.cljc:193-198`) become explicit acts (a callable command now, a wish later).
4. **Undo** is orthogonal to storage (it always was): the editor's undo unit is the coalesced burst, and undo appends a compensating write — history only grows. No storage-layer involvement.

**Contract-time checks (the slice contract is written under the /rama skill, which should have been loaded before any of this was designed):** single-node footprint on the PC (Zookeeper/Conductor/Supervisor processes — RAM/disk overhead measured); licensing terms for a production single-node cluster; IPC-developed modules deploying unchanged via the CLI; foreign-client swap localized in the runtimes; depot trimming/retention defaults; backup provider recipe to local filesystem; restart-recovery actually exercised (kill -9 mid-typing, reboot, verify conversation + revisions + trail); the 7.66ms echo re-measured against the real cluster (network hop localhost, expected fine — measured, not assumed).

## Answers of record

- **Multi-turn and `claude -p`:** one turn per invocation — but the CLI resumes sessions and the lane captures session ids, so continuity is plumbed-ready. Constitutional stance: **the land is the memory** — each run is fed the trail material it needs; the "session" is the trail, not the tool's session file. Model tool stays swappable (replaceable-reader rule). Resume-by-session-id = caching optimization, never source of truth.
- **Where model calls run:** from wherever the land's JVM runs. Keys/auth stay with the server process; the browser never holds them.

## Decisions

1. **Where the land lives: DECIDED (Sid, 2026-07-15).** The land runs on the PC (where Sid works; can power off / lose power — native cluster recovery makes that a non-event). The MacBook (8GB, low spec, kept running) is the **vault, never a runtime** — it holds backup copies over the existing rsync pipe.
   *Mac-hosting considered and declined (Sid asked 07-15):* 8GB swaps under JVM + state + CLI subprocesses, and lag is an H1 kill risk; splitting land (Mac) from workshop (PC) adds a cross-machine deploy step inside the repair loop, killing wish-to-worn-change latency during the wear month; the protection Sid wanted (unstable code must not endanger real data) is delivered by durable storage + backups at the data layer. *Server trigger, not server date:* the first organ that must run while Sid sleeps (consolidator / overnight agents / phone access). The move then = deploy modules to the new box + restore from backup — portable by construction.
2. **Durability: DECIDED in shape (Sid's requirement, 07-15 — "before first-light I want the log to exist and some form of backup so we can replay it in future"), CORRECTED in mechanism (Sid's catch, same day):** the **durable-ground slice** above — a real single-node Rama cluster + native backups to the vault. No hand-rolled journal. Replayability = Rama's own model (recompute views from depots); "actual data of first-light and all the chaos after" accrues in the depots from day one.
3. **Model lane for first-light:** (a) CLI subprocess lanes (claude + codex) on **subscription auth** ← pick, and Sid's stated constraint (07-15: "use the subscription that i have for claude and codex, not API pricing") · (b) direct API — API pricing; fallback only · (c) swappable later via the replaceable-reader boundary.
4. **Backups:** Rama-native backups to local filesystem + rsync to the Mac whenever both machines are awake — boring on purpose ← pick.

## Subscriptions, not API (Sid's constraint — already the built default)

The problem space is already researched and canonical: `architecture/dogfood-runtime/llm-track-claude-research.md` (auth-path priority order §~207; the two Softland modes; the OpenClaw ban patterns §4) and `llm-track-canonical.md` (§2 Codex protocol). Verified in code this session:

- `claude-process-spec` defaults `:llm/auth-mode` to **`:subscription`** (`dogfood/llm.clj:1878`).
- In subscription mode the child env is the system env **stripped of sensitive keys** (`claude-child-env`, llm.clj:1851-1856) — `ANTHROPIC_API_KEY` etc. are removed, so the CLI can only fall back to its logged-in OAuth (Sid's Max plan). API-key mode is explicit opt-in and (per the research doc) spawns `claude --bare` so it structurally cannot drift onto subscription credentials.
- Codex is a first-class backend (`#{:codex :claude}`, full observation vocabulary) — its ChatGPT-subscription path is the `llm-track-canonical.md` §2 protocol (not line-verified this session).

Consequences: **(1)** the CLI-subprocess lane is the only legitimate way to ride the subscriptions — pins Decision 3(a). **(2)** Wherever the land runs, the CLIs must be logged in there; single-user own-workload is the sanctioned shape; pooling/serving others is the banned shape; visitors bring their own assistants (North). **(3)** Budgets are rate-limit-shaped, not dollar-shaped: `:result/cost-usd` on subscription is API-equivalent accounting — render as such, never as money spent.

## The rest of the ops question space (one-line stances)

Secrets: env.clj + CLI auth stay server-side, never client. · Cost: every run's cost is already an observation — the land can render its own burn; always-on budgets are a later decision. · Offline: reading/writing works without network; only model calls need it; wishes can queue. · Upgrades: Rama module update is a built-in operation; exercised at contract time. · Privacy: sense-line data is the most personal data Sid owns → local-first + never-pushed docs branch already encode the stance; backups inherit it (encrypt if off-machine). · Phone/multi-device: requires hosting or a tunnel — deferred until wanted. · Provider outage: the land degrades to a quiet land (write/read fine, agents silent). · Agent permissions: the subprocess runs with Sid's user rights today; sandboxing becomes a real decision when agents act more autonomously.
