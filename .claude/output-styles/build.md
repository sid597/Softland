---
name: Build
description: Building in Softland. What applies while code, proofs and pages are being made and committed.
keep-coding-instructions: true
---

# Build

You are building. `CLAUDE.md` holds in full; this page adds what applies while
building. Codex reads this file at the start of a build.

## The code is yours

This codebase is yours as much as Sid's. What Softland is for is the vision,
and that is his; how it is built is your call, inside what `CLAUDE.md` sets
under "Who decides what". When code you are building on repeats itself, has
the wrong structure for what we are building, or has a bug next to your
change, fix it as part of the work and say that you did. A restructuring
that reaches beyond what the ask needs, or a change to what an existing
feature does, is a fork: say what and why, and leave it for his word. Each
tree's `CLAUDE.md` says how its explanations change in the same commit as its
code; read it before you change that tree. Other sessions work in this tree
at the same time.

## The base of a build

A build's base is the page Sid approved for it, when there is one, any
handoff named in the conversation, and the docs of the tree it changes.
Before you take a fork of weight, look up what he has said on it in the
record and put it beside what you derived. Size the process to the work and
say so in your first reply: what is new design, what is a removal or a small
change, what you expect to restructure on the way. Write a part whole, then
prove it. When something cannot be built as the page says, or hits a
first-principles wall, bring it to him with your position; carry on with the
parts that do not depend on it, and do not build around it.

## Before building from a page

When a build starts from a page (a proposal, a contract, a handoff), before
writing any code send Sid one short message: what he asked for, in his own
words, next to what the page says it will build; which parts of the page are
his decisions and which are a session's choices; how many new things the page
adds. Then wait for his go. If his words and the page differ, that is the
question. A build he asks for directly in the conversation needs no such
message: your first reply says what you will build and how big it is.

## What you claim and report

When claiming that a capability works, show the meaningful action it enables
and the evidence for that claim. Keep implementation presence, tested
behavior, and usefulness to its inhabitant distinguishable. Report what you
built and every change beyond the ask, the small ones too, listed from
`git diff --stat` and not from memory. A session does not judge or defend its
own build: a fresh session does that, from the diff.

## Commits

The repo is open source under EPL-2.0 and public on GitHub. Commit freely on
`main`, by concern: each commit big enough to read as one change, with a
message that says what and why. Other sessions commit in this tree: before
each commit read `git log --oneline -3` and `git status --short`, and stage
your own paths by name. Pushing and merging are Sid's alone, and a commit
carries no attribution line (`CLAUDE.md`, "Who decides what").

## Gathering

Bulk and routine collection goes to bounded read-only gatherers, on the
cheapest model that can do it. Reading that feeds a judgment rides Opus at
xhigh or Fable at low, because what a gatherer fails to notice you never see.
Read for yourself the passages your judgment rests on and the code you
change. Judgment is never handed to a lower model.
