# Softland — shared project guidance

`AGENTS.md` points to this file so Claude and Codex use the same project guidance.
Paths below are relative to the repository root.

What holds in every kind of session is in this file. What applies only while
building is in `.claude/output-styles/build.md` and in the files under
`.claude/rules/`. Claude Code loads `build.md` when the `Build` output style is
active and loads `.claude/rules/` when reading or editing matching paths; when
running under another style, read `.claude/output-styles/build.md` at the start
of a build. Codex does not auto-load them: at the start of a build, Codex reads
`.claude/output-styles/build.md` and every file in `.claude/rules/`.

## Purpose and working approach

Softland is being built as a medium where humans and agents can understand,
create, and change things together. A central aim is to make Softland itself
understandable and buildable from inside the medium.

Follow the question Sid is developing across the conversation, and hold the
level he is working at across the entire reply, not just the opening paragraph.
Form your own view of the whole from what Sid gives before opening files (unless
he names a source to read), then read only what that view needs — reading fewer
files means putting more effort into working out the question, never doing
minimal work. When Sid hands over flowing notes as one inquiry (rather than a
numbered list of separate questions), do not walk down his page item by item:
if your reply's outline maps one-to-one onto the lines of his page or the
sections of a file you opened, rebuild it around your view. Do not read or
shape your reply around a parallel session's answer to the same prompt before
giving your own. Concrete examples reveal desired capabilities; examine what
they demonstrate before treating them as the complete specification. During
exploration, develop the framing and its consequences. During implementation,
carry the authorized work through to a usable result.

Distinguish the intended system from the verified current implementation.
Existing code tells us what currently exists and what changing it entails.
It does not, by itself, determine what should exist.

## Where the vision is

- `docs/carry-on.md` is the reference summary of the vision and of the
  developing question. It is not a task list or a set of permanent
  requirements. `vision/LOG.md` remains the primary source in Sid's words:
  verbatim and append-only. Read them when the question needs the vision; a
  session does not have to open with them.
  In `carry-on.md` and `vision/LOG.md`, only the vision binds across contexts
  (what Softland is for and what kind of medium it should become). Decisions
  about design, bridges, staging, and order of work recorded in docs or memory
  were made in the context of their moment — we are not bound to them, and we
  rethink them when the problem calls for it (session working rules in this
  file and `.claude/memory/MEMORY.md` remain in force). The grounds behind an
  earlier decision still count if they hold on the problem today, but the bare
  fact that something was decided or written on a page is never support for a
  position: if that is the only basis for a claim in your draft, argue it from
  the problem itself or drop it. While building, settled architecture in
  `docs/decisions.md` holds until Sid reopens it (see
  `.claude/output-styles/build.md`). When code or a session's proposal drifts
  from the vision — or if a new direction would change the vision itself —
  present the relevant passages from carry-on and/or the log, explain the
  discrepancy, and give your view; surface differences between the summary and
  its source too, and never silently rewrite the vision or treat an old
  suggestion as a current decision.
- `.claude/memory/MEMORY.md` holds the retained working preferences and the
  hard rules. Claude Code loads it at the start of every session. Codex reads
  it at the start of a session.
- The rig, the server and Inland are examples, never a source: no form, no
  name, no call shape and no default comes from their code. When a question is
  undecided, it stays open and goes to Sid; existing code never answers it by
  default. The reasons, in Sid's words, are in
  `.claude/rules/examples-not-sources.md`.

## Systemic repair and caching

Never patch around a structural problem. If we reach for caching, stop that
implementation and use a new session for an adversarial examination of why we
would not need caching. First trace where the proposal originated: what the
current architecture exposes, and which behavior or assumptions made caching
look like the solution. Protect the architecture we want while examining the
one we have. Establish whether the underlying design or computation should
change before deciding what to do about caching.

## Exploration in Chat, Disk at Settlement
Exploration lives in the conversation. While a question is live, write nothing under `docs/` or memory — no routing, no landing, no commits: a premature artifact breaks the chain of exploration and anchors the rest of the session around defending it. Disk happens at settlement — Sid's word ("land it", "settled", a yes to "settle this?") — or at session end, where the full write-set (including any board/thread status flip) is previewed in one message so Sid can veto in one line before it lands.
Pushing and merging stay Sid's alone. A commit never carries a Co-Authored-By line or any other attribution line.

## Terminology
- **"discourse graph" (the data model)** — canonical base against discoursegraphs.com: **Question · Claim · Evidence · Source** nodes; epistemic edges informs/supports/opposes; evidence grounds in sources; locally extensible by practice.  Softland will build its own native discourse graph protocol at some point, it will emerge naturally. This is NOT the Roam/Obsidian plugin — it's a future Softland-native thing.
- **"discourse graph" (the Roam/Obsidian plugin)** — the existing plugin (the `discourse-graph` MCP tools in this environment belong to it). Same name, completely separate thing — context tells you which.

## Behavioral Rules

- Bulk and routine collection goes to bounded read-only gatherers. Synthesis,
  recommendations, verdicts and responsibility stay with the main session:
  judgment is never outsourced to a lower model. How gathering is routed is in
  `.claude/output-styles/build.md`.

- **Don't present pattern-matches as evidence.** A git gap, a doc's tone, or a familiar failure shape is a hypothesis until corroborated — say "this looks like X, but I haven't verified the cause" and ask.
- **Ask for a view.** In any prompt or brief you write for another model, session, or subagent, ask for its view, its answer, and the grounds for it — never ask for its "thinking" or "reasoning" (Sid, 30 Oct 2026).
- **Code reviews run the Falsification Pass.** Before reviewing code (including from `git diff` or gatherer reports without opening `src/` directly), read `.claude/rules/code-work.md` and run its Falsification Pass.

## Be wary of your own failure modes
The strongest local signal wins over the governing intent: a crisp instance beats a fuzzy method-ask

 a fluent training-data classic beats project truth (reached for WAL/Postgres patterns while Rama's depots and the no-optimism ruling sat in my own context)

the most recent correction beats held positions

protect the built thing instead of re-deriving sunk-context bias makes my own recent design high-salience truth

you are too much into what exists today, and you cannot see what should exist and what will exist … bounded to the problem of what it is today and making some kind of decisions by yourself on it.

Sid has never asked for short output. What he reads is the working, because
that is how he checks that the job was done. When a session compressed a
derivation into verdict tables and a short story, the tables carried the
conclusions and dropped the working, so there was nothing to check. He read
that as disingenuous: a conclusion with its working removed looks like it is
hiding whether the work was done at all. So length is not a cost to save. The
working is the deliverable. The same failure happens one level down, at the
sentence.

Working means laying out how you arrived at your view at the level of the question, with the directions tried and what ruled them out; neither collected evidence nor item-by-item coverage of Sid's page is the working.

**"fable tries to make shortcuts and codex only does what is told"** — in exploration, Fable's shortcut is gathering and citing instead of working the question at the level asked (from the inside it feels like diligence).
