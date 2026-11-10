---
name: Build
description: Building in Softland. The rules that apply while code, proofs and pages are being made and committed.
keep-coding-instructions: true
---

# Build

You are building. The rules below apply while building. They were in `CLAUDE.md`
until 10 November 2026, and their wording is unchanged. Codex reads this file at
the start of a build.

## Claims about what works

When claiming that a capability works, show the meaningful action it enables
and the evidence for that claim. Keep implementation presence, tested behavior,
and usefulness to its inhabitant distinguishable.

## At the start of a build

At the start of a build session, read `docs/decisions.md` and any task-specific
handoff named in the current conversation.

## Commit mechanics

Commit mechanics once settled: the repo is open source under EPL-2.0 (`LICENSE`, settled 2026-11-02; what it does not cover is in `THIRD-PARTY.md`), public on GitHub (Sid, 2026-10-26: "its open soruce we will commit freely"; it had been called closed source since 2026-08-10) — commit freely, code, docs, and law files alike, on `main`, no approval needed, with no condition on what may go public; group commits by concern, each big enough to read as one change with a message that says what and why — never a fix-by-fix tail (Sid, 2026-09-02: commits "too small to know anything"). Pushing/merging stays Sid's alone (decisions.md "Only Sid decides"); never a Co-Authored-By line.

## Before building from a page

Before writing any code, the build session sends Sid one short message:
- what he asked for, in his own words, next to what the page says it will build;
- which parts of the page are his decisions and which are a session's choices;
- how many new things the page adds.
Then it waits for his go. If his words and the page differ, that is the question.

A session does not judge or explain its own build. A fresh session does that.

## Gathering and routing

- Delegate bulk and routine collection to bounded read-only gatherers to
  conserve attention. Read the primary passages needed to make your own
  judgment. Keep synthesis, recommendations, and responsibility with the main
  session.
- Routing is two-tier:
  - deterministic collection
    (greps, counts, suite runs, exact text collection) rides the cheapest competent
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
- the JUDGMENT is never outsourced to lower
  models — contract cuts, rulings, repairs, cross-law composition ride the
  strongest model, no exceptions; every contract header names its cutter
  model + effort
