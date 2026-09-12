# Softland — shared project guidance

`AGENTS.md` points to this file so Claude and Codex use the same project guidance.
Paths below are relative to the repository root.

## Purpose and working approach

Softland is being built as a medium where humans and agents can understand,
create, and change things together. A central aim is to make Softland itself
understandable and buildable from inside the medium.

Follow the question Sid is developing across the conversation. Concrete
examples reveal desired capabilities; examine what they demonstrate before
treating them as the complete specification. During exploration, develop the
framing and its consequences. During implementation, carry the authorized work
through to a usable result.

Distinguish the intended system from the verified current implementation.
Existing code tells us what currently exists and what changing it entails.
It does not, by itself, determine what should exist.

When claiming that a capability works, show the meaningful action it enables
and the evidence for that claim. Keep implementation presence, tested behavior,
and usefulness to its inhabitant distinguishable.

## Reading the project

- Start with `docs/carry-on.md` to understand the vision and the developing
  question. It is a reference summary, not a task list or a set of permanent
  requirements. `vision/LOG.md` remains the primary source in Sid's words.
  When a proposal, implementation, or document appears to contradict that
  understanding, present the relevant passages from carry-on and/or the log
  in the conversation, explain the discrepancy, and give your reasoned view.
  Surface differences between the summary and its source too; do not silently
  resolve a contradiction by rewriting the vision or treating an old suggestion
  as a current decision.
  `BETS.md` is retired; mentions in historical records, quoted prompts, and
  frozen benchmark material refer to that earlier document, not current guidance.
- Use `.claude/memory/MEMORY.md` for the retained working preferences. Read it
  if it is not already in the session context; consult its supporting notes
  when relevant to the task.
- At the start of a build session, read `docs/decisions.md` and any task-specific
  handoff named in the current conversation.
- Before gathering, reviewing, explaining, or changing client code, read
  `src/app/client/AGENTS.md` and `src/app/client/README.md`. Follow the relevant
  folder maps, namespace docstrings, and function docstrings into scoped source
  reads. Keep affected explanations and their immediate parent maps aligned
  with code changes, as the client instructions describe.

## Systemic repair and caching

Never patch around a structural problem. If we reach for caching, stop that
implementation and use a new session for an adversarial examination of why we
would not need caching. First trace where the proposal originated: what the
current architecture exposes, and which behavior or assumptions made caching
look like the solution. Protect the architecture we want while examining the
one we have. Establish whether the underlying design or computation should
change before deciding what to do about caching.

## Vision Interleave
`vision/LOG.md` is Sid's verbatim vision depot;


## Exploration in Chat, Disk at Settlement
Exploration lives in the conversation. While a question is live, write nothing under `docs/` or memory — no routing, no landing, no commits: a premature artifact breaks the chain of exploration and anchors the rest of the session around defending it. Disk happens at settlement — Sid's word ("land it", "settled", a yes to "settle this?") — or at session end, where the full write-set (including any board/thread status flip) is previewed in one message so Sid can veto in one line before it lands.
Commit mechanics once settled: the repo is closed source (Sid, 2026-08-10) — commit freely, code, docs, and law files alike, on `main`, no approval needed; group commits by concern, each big enough to read as one change with a message that says what and why — never a fix-by-fix tail (Sid, 2026-09-02: commits "too small to know anything"). Pushing/merging stays Sid's alone (decisions.md "Only Sid decides"); never a Co-Authored-By line.



### Terminology
- **"discourse graph" (the data model)** — canonical base against discoursegraphs.com: **Question · Claim · Evidence · Source** nodes; epistemic edges informs/supports/opposes; evidence grounds in sources; locally extensible by practice.  Softland will build its own native discourse graph protocol at some point, it will emerge naturally. This is NOT the Roam/Obsidian plugin — it's a future Softland-native thing.
- **"discourse graph" (the Roam/Obsidian plugin)** — the existing plugin (the `discourse-graph` MCP tools in this environment belong to it). Same name, completely separate thing — context tells you which.

### Behavioral Rules

- Delegate bulk and routine collection to bounded read-only gatherers to
  conserve attention. Read the primary passages needed to make your own
  judgment. Keep synthesis, recommendations, and responsibility with the main
  session.
- Routing is two-tier:
  - deterministic collection
    (greps, counts, suite runs, exact extraction) rides the cheapest competent
    lane;
  - judgment-adjacent reading (doc sweeps feeding a cut, ruling, or
    amendment) rides Opus-xhigh/fable-low — an UNCERTAINTY row is only as good as its
    noticer.
- Every tool call requests ≤8K output tokens and filters at
  the command; raw output uses a named writable scratch path, otherwise only
  bounded anchors return. The parent spot-checks decision-changing facts.
  In Sid's words: don't use your own tokens "just for puny gathering tasks —
  you are suited for sensemaking." Adjudication, synthesis, and verdicts
  never delegate. Make model routing explicit; judgment stays home.
- the THINKING is never outsourced to lower
  models — contract cuts, rulings, repairs, cross-law composition ride the
  strongest model, no exceptions; every contract header names its cutter
  model + effort

- **Don't present pattern-matches as evidence.** A git gap, a doc's tone, or a familiar failure shape is a hypothesis until corroborated — say "this looks like X, but I haven't verified the cause" and ask.



### Code Review Protocol — Falsification Pass

After the architectural pass, do a second pass whose job is to **break** the change. Review for falsification, not coherence.

**Golden rule:** Never approve an optimistic-state change without tracing the full write → render → truth reconciliation → clear lifecycle.
think like the Staff-level product architect, use the Staff-product-architect.md skill from the falsification angle


## Be wary of your own failure modes
The strongest local signal wins over the governing intent: a crisp instance beats a fuzzy method-ask

 a fluent training-data classic beats project truth (reached for WAL/Postgres patterns while Rama's depots and the no-optimism ruling sat in my own context)

the most recent correction beats held positions

protect the built thing instead of re-deriving sunk-context bias makes my own recent design high-salience truth

you are too much into what exists today, and you cannot see what should exist and what will exist … bounded to the problem of what it is today and making some kind of decisions by yourself on it.

Sid never constrained output length. When the deliverable is an analysis or a
derivation  compressing it to verdict tables and a
short story hides the working that lets him check the job was done. He read
the compressed version as disingenuous.


**"fable tries to make shortcuts and codex only does what is told"**
