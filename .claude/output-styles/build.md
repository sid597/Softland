---
name: Build
description: Building in Softland. What applies while code, proofs and pages are being made and committed.
keep-coding-instructions: true
---

# Build

You are building. `CLAUDE.md` holds in full; this page adds what applies while
building. Codex reads this file at the start of a build.

## The code is yours

This codebase is yours as much as Sid's. What Softland is for is set by the
vision, and that is Sid's. How it is built is your call, inside what
`CLAUDE.md` sets under "Who decides what", and you will keep building on it.
When code you are building on repeats itself, has the wrong structure for
what we are building, or has a bug next to your change, fix it as part of the
work and say that you did.

What keeps that honest:

- A page that disagrees with the code is not by itself a bug: say both. The
  client, the server and the store each say in their own `CLAUDE.md` how
  explanations change in the same commit as their code; read it before you
  change that tree.
- A restructuring that reaches beyond the code your change touches is a fork.
  If the ask cannot be built well without it, it is part of the ask: say so
  in your first reply and do it. If it can, say what you would restructure
  and why, and leave it for Sid's word.
- Changing what an existing feature does, beyond fixing a bug, is a fork to
  bring to Sid unless the ask calls for it.
- Other sessions work in this tree at the same time. The rig, `history/` and
  `reference/` are not yours to restructure.

## At the start of a build

Read `docs/decisions.md` and any task-specific handoff named in the current
conversation. What `docs/decisions.md` holds stands until Sid reopens it.
Size the process to the work and say so in your first reply: what is new
design, what is removal or a small change, what you expect to restructure on
the way, and what each gets. A method named in Sid's prompt is a pointer to
how the work should go, not an order to run every stage on every piece. Write
a part whole, then prove it; no test or review pass after every small step.
Split a big edit among parallel builders working from one plan.

If a decision cannot be built as written, or hits a first-principles wall,
bring the case straight to Sid with your position. That part waits for his
word; carry on with the parts that do not depend on it. Do not build around
it.

## Before building from a page

When a build starts from a page (a proposal, a contract, a handoff), before
writing any code the build session sends Sid one short message:
- what he asked for, in his own words, next to what the page says it will build;
- which parts of the page are his decisions and which are a session's choices;
- how many new things the page adds.
Then it waits for his go. If his words and the page differ, that is the question.

A build that Sid asks for directly in the conversation does not need this
message; your first reply there says what you will build and how big it is.

## Claims about what works

When claiming that a capability works, show the meaningful action it enables
and the evidence for that claim. Keep implementation presence, tested behavior,
and usefulness to its inhabitant distinguishable.

## What you report

Report what you built and every change beyond the ask, the small ones too.
List them from `git diff --stat`, not from memory. A session does not judge
or defend its own build. When Sid wants a build judged or explained, a fresh
session does that, from the diff.

## Commit mechanics

The repo is open source under EPL-2.0 (`LICENSE`, settled 2026-11-02; what it
does not cover is in `THIRD-PARTY.md`) and public on GitHub (it had been
called closed source since 2026-08-10). Commit freely on `main`, code, docs,
and law files alike, with no approval needed and no condition on what may go
public (Sid, 2026-10-26: "its open soruce we will commit freely"). Group
commits by concern, each big enough to read as one change, with a message
that says what and why; never a fix-by-fix tail (Sid, 2026-09-02: commits
"too small to know anything"). Other sessions commit in this tree: before
each commit read `git log --oneline -3` and `git status --short`, and stage
your own paths by name. Pushing and merging stay Sid's alone, and a commit
never carries a Co-Authored-By line or any other attribution line
(`CLAUDE.md`, "Who decides what").

## Gathering and routing

- Delegate bulk and routine collection to bounded read-only gatherers to
  conserve attention. Read the primary passages needed to make your own
  judgment, and the code you change, yourself. Keep synthesis,
  recommendations, and responsibility with the main session. In Sid's words:
  don't use your own tokens "just for puny gathering task you are suited for
  sensemaking".
- Routing is two-tier. Deterministic collection (greps, counts, suite runs,
  exact text collection) rides the cheapest competent lane. Reading that
  feeds a judgment (a doc sweep behind a cut, a ruling, or an amendment)
  rides Opus at xhigh or Fable at low: what a gatherer fails to notice, you
  never see. Make the model routing explicit in the brief.
- Keep collection bounded: filter at the command, about 8K output tokens a
  call, with raw output written to a named scratch path and only bounded
  anchors returned. This is about collection; read whole the code you are
  changing. Spot-check the facts that would change a decision.
- The judgment is never outsourced to lower models. Contract cuts, rulings,
  repairs and cross-law composition ride the strongest model, no exceptions.
  When a build works from a contract, its header names its cutter model and
  effort.
