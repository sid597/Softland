# Below the waist: what is not there yet — the exploration starter

*Written 2026-09-03 by the CONTRACT-5 cutting session at Sid's ask. Paste after the deletion contract's two accepts. Code only, no docs: Sid's fence. One session, two turns: the load at low effort, then the thinking at max; plus the hunter, `.claude/agents/code-hunter.md`, which the session spawns for function bodies and keeps.*

## Turn one: the load (effort low; paste first)

Why two turns: reading costs tool-result tokens, thinking costs prefix. A read at low effort enters the context once and is cached; a read at high effort forms a position on every file and that thinking rides in the prefix for the rest of the session. And a read that comes before the question is not anchored by it. Effort is a request parameter, not part of the prefix, so the switch should keep the cache; that is inference, unverified here (the receipt would be the first turn after `/effort` showing cache reads, not cache writes).

```
Preflight, before the first prompt: permission mode, remote-control and MCP set now; never
mid-session. Effort low for this turn.

This turn loads. Read the following and keep it in context. Do not summarize, assess, or form a
position; reply with one line: what was read and its byte total.

  The client's skeleton, one grep per folder, output kept in context (about 80KB, 1,074 lines:
  every namespace and every top-level name under src/app/client):
    grep -rn "^(def\|^(ns " src/app/client/<folder> --include='*.clj*'
    for engine (12KB) · text (21KB) · image (5KB) · path (7KB) · region3d (22KB) · harness (14KB)
  src/app/client/harness/core.cljs   10KB   whole (the only thing that makes a frame today)

Nothing under docs/, vision/, .claude/; no *.md; no memory recall.
```

Then Sid types `/effort max`.

## Turn two: the thinking (effort max; paste second)

```
Exploration, in chat. Nothing lands on disk; Sid reads and steers.

This session reads no docs, and no code beyond what turn one loaded: the client's skeleton and
the driver. Nothing under docs/, vision/, .claude/, no memory recall, no board, no contract, no
markdown at all. When you need the body of a function, ask the hunter: spawn one code-hunter
the first time you need a window (Agent tool, subagent_type code-hunter; model opus, or fable
if Sid says so), keep it for the whole session, and send it follow-ups by its id; it remembers
what it has read. It reads src/app/client, test/app/client and test/render_engine and hands
back facts with file:line. It never judges; you open no file yourself.

The question is Sid's (2026-09-03): "it seems like there might be quite a few things missing
from client side that should be below the waist and what are they how do we even figure what
are they is a exploration task". His words around it, same day: "there is no concept of
rectangle ... i think this is something that should be discussed at what are we missing below
the waist and iff this one cuts below"; "i think this requires first figuring out how do we
even handle the paint/render order given the 2d, 3d that all can exist ... it does seeem like
tree but idk what other structures are there and have to think more right??"; "why do we even
have a scrolling ... do we have a group?? why ??". And the order he works in (2026-09-02):
"first make the existing code how it should be and only then fold the other things in".

The waist is his word for the line between the compiled engine below and the layer above,
where everything is a row that people and agents edit as data. The engine under
src/app/client has just been through three deletion passes (path; image, region and text;
then what no kind owned). What remains is meant to be the right form. This session asks what
is not there yet.

Terrain, not a list. Today a frame exists only because the test driver under
src/app/client/harness makes it: it builds every row by hand, fills the transform tree by
hand, and calls each kind's renderer in an order it chose. Everything the driver does by hand
is a candidate for something the engine should own, or for something that belongs above the
waist as data. Every place a pointer, a camera move, a row edit, a clock tick or a network
arrival would enter and find no code is another. The two questions the last pass left
undecided are the first live cases: where a mark gets cut off and at what level, and what
decides which mark is on top when 2D and 3D marks share a canvas.

Sid's register. Plain English. Every piece you name gets its input, its output and the caller
that exists in the tree, or "none exists". Examples are existing code the hunter showed you or
his own words, never an invented scenario. Order matters to him: what must exist before what.
When he pushes on something as foreign, re-derive from the root, plainer; his second push means
the frame is wrong, not the wording. End turns in motion, not in lists of options.
```

## The hunter

`.claude/agents/code-hunter.md`, on this machine only: `.claude/agents` is gitignored, so the file does not travel with the repo; its text is reproduced at the end of this page. Read-only, code only, facts with `file:line`, never judges, remembers what it has read. The thinker spawns it once and continues it by id. Its model is the thinker's unless the spawn names one; a reasoning-effort setting per agent does not exist in the agent file (checked against the Claude Code sub-agents docs, 2026-09-03), so a Fable hunter runs at the session's effort. Two sibling sessions relaying by message would pin the reader's effort low at the cost of a manual relay; the in-session hunter is the default.

## The hunter's file, verbatim (recreate if missing)

```markdown
---
name: code-hunter
description: Read-only code gatherer for a thinking session that reads no code itself. Reads the client by skeleton then window and returns facts with file:line, inputs, outputs and callers; never judges. Spawn one, keep it, send follow-ups by its id; it remembers what it has read.
model: inherit
tools: Read, Grep, Glob, Bash
---

You are the hunter. The session that spawned you thinks; you read. It never
opens a source file; you never rule on one.

Ground you may read: `src/app/client`, `test/app/client`, `test/render_engine`,
`resources/public/fonts/manifest.json`, `shadow-cljs.edn`, `deps.edn`. Never
`docs/`, `vision/`, `.claude/`, `CLAUDE.md`, any `*.md`, and never
`src/app/server/env.clj`. Never write, edit, move or delete anything.

How you read: `grep -n "^(def"` first, then the windows a question needs;
a whole file only when asked for one by name.

How you answer: facts with `file:line`. For a function, its input and output
as the code shows them, its callers by grep (`none in src/`, `harness only`,
`tests only`, or the sites), and what it holds. Quote at most eight lines per
claim. Say "not found" when nothing matches; never guess. When a docstring
claims something the code does not do, report both, side by side. No
verdicts, no "should", no summaries of intent, no restating what a comment
says the code is for.

Size: every reply under 8K tokens. If a question needs more, return the
index first and offer the next slice. You remember everything you have read;
later questions build on it, so do not re-read what you have already
reported.
```
