---
name: feedback-durable-work-lands-in-docs-not-tmp
description: "Anything a later session must inherit (pages, fact bases, handoffs, benches, hunter reports) lands under docs/ in the repo, never only in /tmp or a scratchpad; Sid restarts machines and /tmp is gone (Sid, 2026-09-05: 'don't keep things in tmp they will be lost when i restart')"
metadata:
  type: feedback
---

The scratchpad and `/tmp/claude-1000/...` are for throwaway files. Anything meant to
outlive the session, a page, a fact base, a handoff brief, a bench, hunter reports,
goes into the repo under `docs/` (for the path kind: `docs/below-the-waist/path-kind/`)
and is committed with exact paths. The artifact URL survives on its own; the source
file behind it must still land.

**Why:** Sid, 2026-09-05, when session 7's work and session 4's bench sat in
`/tmp/claude-1000/`: "don't keep things in tmp they will be lost when i restart".
A restart wipes `/tmp`; a session's scratchpad path is also unreachable to a fresh
session without the path being handed over.

**How to apply:** before a session ends or a handoff is written, move every file a
successor would read into `docs/` and commit (exact paths, plain commit). Point the
handoff and the starter at the repo path, never at `/tmp`. Composes with
[[feedback-code-maps-one-notch-in-out-why]] (pages land as .md + .html twins) and
[[feedback-parallel-sessions-shared-branch-git]] (exact-path staging when siblings
have uncommitted work).
